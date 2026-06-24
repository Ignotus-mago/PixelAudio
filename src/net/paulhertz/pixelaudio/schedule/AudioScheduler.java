/*
 *  Copyright (c) 2024 - 2025 by Paul Hertz <ignotus@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.paulhertz.pixelaudio.schedule;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A sample-accurate scheduler for things that should occur at specific times in an audio stream.
 *
 * <p><b>Two event shapes</b></p>
 * <ul>
 *   <li><b>PointEvent</b>: happens once at an absolute sample time.</li>
 *   <li><b>SpanEvent</b>: is active over an interval [startSample, endSample) and can report start/end
 *       offsets plus per-block callbacks while active.</li>
 * </ul>
 *
 * <p><b>Threading model</b></p>
 * <ul>
 *   <li>{@code schedulePoint(...)} and {@code scheduleSpan(...)} may be called from any thread.</li>
 *   <li>{@code processBlock(...)} must be called exactly once per audio block on the audio thread.</li>
 * </ul>
 *
 * <p><b>Happenings</b></p>
 * <p>
 * The generic type parameter {@code H} is a user-defined "Happening": a compact piece of information
 * that you want to schedule in time (e.g., x/y coordinates, pan/gain, an instrument id, a brush reference,
 * or a small command object). A Happening is carried by events and delivered to handlers at the correct time.
 * </p>
 *
 * <p><b>Sample accuracy</b></p>
 * <p>
 * {@code processBlock(...)} reports an {@code offsetInBlock} for point events and span boundaries, so you can
 * align actions to an exact sample within the current audio block. If your instrument can only start on
 * block boundaries, you can still use this scheduler for deterministic block timing today, and later
 * upgrade your instrument to accept offsets without changing scheduling code. This is currently the 
 * case with the Sampler instrument as implemented in current example code. 
 * TODO sample-accurate playback of Sampler instrument, a demo sketch to point to here. 
 * </p>
 */
public final class AudioScheduler<H> {

    /* ----------------------------- Event Types ----------------------------- */

    /** A one-shot event that occurs at an absolute sample index. */
    public static final class PointEvent<H> {
        /** Absolute sample index (0-based) at which the event should fire. */
        public final long sampleTime;
        /** The user-defined Happening carried by this event. */
        public final H happening;

        /**
         * Creates a point event.
         *
         * @param sampleTime   absolute sample index (0-based)
         * @param happening    user-defined payload delivered to handlers
         */
        public PointEvent(long sampleTime, H happening) {
            this.sampleTime = sampleTime;
            this.happening = happening;
        }
    }

    /**
     * A duration event active on [startSample, endSample) where endSample is exclusive.
     * While active, it may be visited each block via {@link SpanHandler#onBlock}.
     */
    public static final class SpanEvent<H> {
        /** Absolute start sample index (inclusive). */
        public final long startSample;
        /** Absolute end sample index (exclusive). Must be greater than startSample. */
        public final long endSample;
        /** The user-defined Happening carried by this event. */
        public final H happening;

        /**
         * Creates a span event.
         *
         * @param startSample absolute start sample index (inclusive)
         * @param endSample absolute end sample index (exclusive)
         * @param happening user-defined payload delivered to handlers
         */
        public SpanEvent(long startSample, long endSample, H happening) {
            if (endSample <= startSample) {
                throw new IllegalArgumentException("SpanEvent endSample must be > startSample.");
            }
            this.startSample = startSample;
            this.endSample = endSample;
            this.happening = happening;
        }
    }

    /* ----------------------------- Handlers ------------------------------ */

    /** Handler for point events delivered during block processing. */
    @FunctionalInterface
    public interface PointHandler<H> {
        /**
         * Called when a point event occurs within the current audio block.
         *
         * @param happening      the event's Happening
         * @param offsetInBlock  sample offset in [0, blockSize)
         */
        void onPoint(H happening, int offsetInBlock);
    }

    /** Handler for span events delivered during block processing. */
    public interface SpanHandler<H> {
        /**
         * Called once when a span begins within the current block (possibly mid-block).
         *
         * @param happening      the event's Happening
         * @param offsetInBlock  sample offset in [0, blockSize)
         */
        void onStart(H happening, int offsetInBlock);

        /**
         * Called for each audio block the span overlaps.
         * Useful for driving continuous/streamed behaviors while the span is active.
         *
         * @param happening         the event's Happening
         * @param blockStartSample  absolute start sample of this block
         * @param blockSize         number of samples in this block
         */
        void onBlock(H happening, long blockStartSample, int blockSize);

        /**
         * Called once when a span ends within the current block (possibly mid-block).
         *
         * @param happening      the event's Happening
         * @param offsetInBlock  sample offset in [0, blockSize]; may equal blockSize if end == block end
         */
        void onEnd(H happening, int offsetInBlock);
    }

    /** Policy for point events whose sampleTime is already in the past when processed. */
    public enum LatePolicy {
        /** Drop late point events. (Span events still run if they overlap the current block.) */
        DROP,
        /** Clamp late point events to offset 0 of the current block. */
        CLAMP_TO_BLOCK_START
    }

    /* ----------------------------- Internals ------------------------------ */

    private final ConcurrentLinkedQueue<Object> inbox = new ConcurrentLinkedQueue<>();
    private final PriorityQueue<Object> queue =
            new PriorityQueue<>(Comparator.comparingLong(AudioScheduler::startSampleOf));

    private LatePolicy latePolicy = LatePolicy.DROP;

    /**
     * Set how late point events are handled. Default is {@link LatePolicy#DROP}.
     *
     * @param policy   late point-event handling policy
     */
    public void setLatePolicy(LatePolicy policy) {
        this.latePolicy = Objects.requireNonNull(policy, "policy");
    }

    /**
     * Returns the current late point-event handling policy.
     *
     * @return late event policy
     */
    public LatePolicy latePolicy() {
        return latePolicy;
    }

    /* ----------------------------- Scheduling ------------------------------ */

    /**
     * Schedule a point event at an absolute sample time.
     *
     * @param sampleTime   absolute sample index (0-based)
     * @param happening    the Happening to deliver
     */
    public void schedulePoint(long sampleTime, H happening) {
        inbox.add(new PointEvent<>(sampleTime, happening));
    }

    /**
     * Schedule a span event active on [startSample, endSample).
     *
     * @param startSample absolute start sample (inclusive)
     * @param endSample   absolute end sample (exclusive)
     * @param happening   the Happening to deliver
     */
    public void scheduleSpan(long startSample, long endSample, H happening) {
        inbox.add(new SpanEvent<>(startSample, endSample, happening));
    }

    /**
     * Clears all pending events (both newly scheduled and already queued).
     * Safe to call from any thread.
     */
    public void clear() {
        inbox.clear();
        synchronized (queue) {
            queue.clear();
        }
    }

    /* ----------------------------- Audio-thread processing ------------------------------ */

    /** Called only on the audio thread. */
    private void drainInbox() {
        Object ev;
        while ((ev = inbox.poll()) != null) {
            synchronized (queue) {
                queue.add(ev);
            }
        }
    }

    private static long startSampleOf(Object o) {
        if (o instanceof PointEvent<?> p) return p.sampleTime;
        return ((SpanEvent<?>) o).startSample;
    }

    /**
     * Process one audio block. Call exactly once per block on the audio thread.
     *
     * <p>All point events that occur within this block (or earlier, if {@link LatePolicy#CLAMP_TO_BLOCK_START})
     * will be delivered to {@code pointHandler}. Span events will deliver start/end callbacks (when the boundary
     * falls within this block) and an {@code onBlock} callback for each block they overlap.</p>
     *
     * @param blockStartSample absolute sample index of the first sample in this block
     * @param blockSize        number of samples in this block
     * @param pointHandler     handler for point events (may be null if you never schedule points)
     * @param spanHandler      handler for span events  (may be null if you never schedule spans)
     */
    public void processBlock(long blockStartSample,
                             int blockSize,
                             PointHandler<H> pointHandler,
                             SpanHandler<H> spanHandler) {

        final long blockEndSample = blockStartSample + blockSize; // exclusive
        drainInbox();

        while (true) {
            final Object ev;
            synchronized (queue) {
                ev = queue.peek();
                if (ev == null) return;
                if (startSampleOf(ev) >= blockEndSample) return; // starts in the future
                queue.poll();
            }

            if (ev instanceof PointEvent<?> p0) {
                if (pointHandler == null) continue;

                @SuppressWarnings("unchecked")
                PointEvent<H> p = (PointEvent<H>) p0;

                if (p.sampleTime >= blockStartSample) {
                    int offset = (int) (p.sampleTime - blockStartSample);
                    if (offset >= 0 && offset < blockSize) {
                        pointHandler.onPoint(p.happening, offset);
                    }
                } 
                else {
                    // late point
                    if (latePolicy == LatePolicy.CLAMP_TO_BLOCK_START) {
                        pointHandler.onPoint(p.happening, 0);
                    }
                    // else DROP
                }
            }
            else {
                if (spanHandler == null) continue;

                @SuppressWarnings("unchecked")
                SpanEvent<H> s = (SpanEvent<H>) ev;

                // Does span overlap this block?
                boolean overlaps = s.startSample < blockEndSample && s.endSample > blockStartSample;
                if (!overlaps) continue;

                // Start within this block?
                if (s.startSample >= blockStartSample && s.startSample < blockEndSample) {
                    int offset = (int) (s.startSample - blockStartSample);
                    spanHandler.onStart(s.happening, offset);
                }

                // Always called for overlapping blocks
                spanHandler.onBlock(s.happening, blockStartSample, blockSize);

                // End within this block?
                if (s.endSample > blockStartSample && s.endSample <= blockEndSample) {
                    int offset = (int) (s.endSample - blockStartSample);
                    // offset may equal blockSize if end==blockEndSample; clamp for safety
                    if (offset < 0) offset = 0;
                    if (offset > blockSize) offset = blockSize;
                    spanHandler.onEnd(s.happening, offset);
                    // finished; do not requeue
                } 
                else {
                    // continues; requeue so we see it again next block
                    synchronized (queue) {
                        queue.add(s);
                    }
                }
            }
        }
    }
}
