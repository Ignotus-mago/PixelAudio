/**
 * Gesture, path, and Bézier curve modeling classes for PixelAudio.
 *
 * <p>This package supports timed gesture capture, point reduction, Bézier path
 * construction, brush shapes, hit testing, curve drawing, and gesture-derived
 * event paths for audio and visual applications.</p>
 *
 * <p><b>Core gesture and brush classes</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.curves.PACurveMaker PACurveMaker} records
 *   timed gestures and derives reduced points, Bézier paths, brush shapes,
 *   polygons, and event points.</li>
 *   <li>{@link net.paulhertz.pixelaudio.curves.PAGesture PAGesture} defines the
 *   basic interface for gesture point and timing data.</li>
 *   <li>{@link net.paulhertz.pixelaudio.curves.AudioBrush AudioBrush} combines a
 *   gesture curve with audio synthesis parameters.</li>
 *   <li>{@link net.paulhertz.pixelaudio.curves.GranularBrush GranularBrush} and
 *   {@link net.paulhertz.pixelaudio.curves.SamplerBrush SamplerBrush} specialize
 *   AudioBrush for gesture playback with granular and sampler audio synthesis.</li>
 * </ul>
 *
 * <p><b>Path and curve geometry</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.curves.PABezShape PABezShape} stores and
 *   draws composite Bézier paths, including transforms and hit-test polygons.</li>
 *   <li>{@link net.paulhertz.pixelaudio.curves.PABezVertex PABezVertex} and
 *   {@link net.paulhertz.pixelaudio.curves.PALineVertex PALineVertex} represent
 *   curved and straight path segments.</li>
 *   <li>{@link net.paulhertz.pixelaudio.curves.PAVertex2DINF PAVertex2DINF}
 *   defines shared behavior for 2D path vertices.</li>
 *   <li>{@link net.paulhertz.pixelaudio.curves.PACurveUtility PACurveUtility}
 *   provides static geometry, reduction, and drawing utilities.</li>
 * </ul>
 *
 * <p><b>Gesture mapping, transforms, and parameter curves</b></p>
 * <ul>
  *   <li>{@link net.paulhertz.pixelaudio.curves.GestureTransformState GestureTransformState}
 *   stores geometric transform state for gesture and curve modeling.</li>
 *   <li>{@link net.paulhertz.pixelaudio.curves.PABoundsPolicy PABoundsPolicy}
 *   applies boundary rules to gesture points and schedules.</li>
 *   <li>{@link net.paulhertz.pixelaudio.curves.PAControlCurve PAControlCurve} and
 *   {@link net.paulhertz.pixelaudio.curves.PAKeyframeControlCurve PAKeyframeControlCurve}
 *   model control values over normalized time.</li>
 * </ul>
 * 
 * <p><b>Experimental</b></p>
 * <ul>
 *   <li>{@link net.paulhertz.pixelaudio.curves.PAGestureParametric PAGestureParametric},
 *   {@link net.paulhertz.pixelaudio.curves.PAIndexParametric PAIndexParametric},
 *   and {@link net.paulhertz.pixelaudio.curves.PAPathParametric PAPathParametric}
 *   provide parametric sampling over gestures, indices, and paths.</li>
 *   <li>{@link net.paulhertz.pixelaudio.curves.PAGestureWriter PAGestureWriter}
 *   supports output or serialization of curves implemented by {@code BezShape}.</li>
 * </ul>
 *
 * <p>Curve-modeling adapted from the IgnoCodeLib Processing library for use with PixelAudio.</p>
 */
package net.paulhertz.pixelaudio.curves;