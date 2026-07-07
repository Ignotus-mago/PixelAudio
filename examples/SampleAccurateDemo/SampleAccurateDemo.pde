import ddf.minim.AudioOutput;
import ddf.minim.Minim;

/**
 * Minimal setup for sample-accurate Sampler/Granular timing experiments.
 *
 * This sketch intentionally fixes the AudioOutput sample rate so beat and gesture
 * schedules produce reproducible integer sample positions across runs.
 */

Minim minim;
AudioOutput audioOut;

final float REQUIRED_SAMPLE_RATE = 48000.0f;
final int AUDIO_BUFFER_SIZE = 1024;
final float SAMPLE_RATE_TOLERANCE = 0.5f;

boolean audioReady = false;
String audioStatus = "";

public void settings() {
  size(640, 360);
}

public void setup() {
  surface.setTitle("SampleAccurateDemo");
  initAudio();
}

public void draw() {
  background(24);
  fill(audioReady ? color(210, 240, 220) : color(255, 190, 170));
  textSize(18);
  text(audioStatus, 24, 40, width - 48, height - 80);
}

/**
 * Initializes Minim at the fixed sample rate required by this timing demo.
 */
public void initAudio() {
  minim = new Minim(this);
  audioOut = minim.getLineOut(Minim.STEREO, AUDIO_BUFFER_SIZE, REQUIRED_SAMPLE_RATE);
  enforceAudioSampleRate(audioOut, REQUIRED_SAMPLE_RATE);

  audioReady = true;
  audioStatus = "Audio ready: " + nf(audioOut.sampleRate(), 0, 1) + " Hz, buffer " + audioOut.bufferSize();
  println("---- " + audioStatus);
}

/**
 * Stops the demo if Minim did not provide the requested output sample rate.
 *
 * @param out           audio output to validate
 * @param requiredRate  required output sample rate in Hz
 */
public void enforceAudioSampleRate(AudioOutput out, float requiredRate) {
  if (out == null) {
    throw new IllegalStateException("AudioOutput could not be created.");
  }

  float actualRate = out.sampleRate();
  if (abs(actualRate - requiredRate) <= SAMPLE_RATE_TOLERANCE) {
    return;
  }

  audioReady = false;
  audioStatus = "SampleAccurateDemo requires " + nf(requiredRate, 0, 1)
      + " Hz, but Minim provided " + nf(actualRate, 0, 1)
      + " Hz. Set the audio device to " + nf(requiredRate, 0, 1)
      + " Hz and restart the sketch.";
  println("---- " + audioStatus);
  noLoop();
  throw new IllegalStateException(audioStatus);
}

public void stop() {
  if (minim != null) {
    minim.stop();
  }
  super.stop();
}
