package net.paulhertz.pixelaudio.schedule;

import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Comparator;

/**
 * SampleAccurateScheduler
 *
 * A generic scheduler for events that should fire at an absolute sample index.
 * 
 * Typical usage:
 *  - schedule(...) from UI / control thread
 *  - tick(currentSample, consumer) from the audio thread (once per sample or per block)
 *
 * T is the event payload, e.g., a GrainEvent, a UI trigger, etc. Renamed to timeEvent here, 
 * since we are primarily concerned with musical or animation events. 
 */
public final class SampleAccurateScheduler<T> {

    public static final class ScheduledEvent<T> {
        public final long startSample; // absolute sample index in the audio stream
        public final T timeEvent;

        public ScheduledEvent(long startSample, T timeEvent) {
            this.startSample = startSample;
            this.timeEvent = timeEvent;
        }
    }

    @FunctionalInterface
    public interface EventConsumer<T> {
        void accept(ScheduledEvent<T> event);
    }

    private final PriorityQueue<ScheduledEvent<T>> queue =
            new PriorityQueue<>(Comparator.comparingLong(e -> e.startSample));

    /**
     * Schedule a timeEvent at an absolute sample index.
     */
    public synchronized void schedule(long startSample, T timeEvent) {
        queue.offer(new ScheduledEvent<>(startSample, timeEvent));
    }

    /**
     * Schedule a pre-built event.
     */
    public synchronized void scheduleEvent(ScheduledEvent<T> event) {
        if (event != null) {
            queue.offer(event);
        }
    }

    /**
     * Schedule multiple events at once.
     */
    public synchronized void scheduleAll(Collection<ScheduledEvent<T>> events) {
        if (events == null) return;
        for (ScheduledEvent<T> e : events) {
            if (e != null) {
                queue.offer(e);
            }
        }
    }

    /**
     * Called from the audio thread.
     *
     * For the given currentSample, deliver all events whose startSample
     * is <= currentSample to the consumer.
     *
     * You can call this:
     *  - once per sample (easiest integration, most accurate), OR
     *  - once per block, passing the blockStartSample and letting the
     *    consumer handle offset logic.
     */
    public void tick(long currentSample, EventConsumer<T> consumer) {
        while (true) {
            final ScheduledEvent<T> event;

            synchronized (this) {
                event = queue.peek();
                if (event == null || event.startSample > currentSample) {
                    return;
                }
                queue.poll();
            }
            // Fire event outside synchronized block
            consumer.accept(event);
        }
    }

    public synchronized void clear() {
        queue.clear();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}
