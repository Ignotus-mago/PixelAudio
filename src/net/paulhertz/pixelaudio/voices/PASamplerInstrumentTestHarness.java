package net.paulhertz.pixelaudio.voices;

import processing.core.PApplet;
import ddf.minim.Minim;
import ddf.minim.AudioOutput;
import ddf.minim.MultiChannelBuffer;

import java.io.File;

/**
 * Minimal Processing/Minim test harness for WFSamplerInstrument.
 * Designed to run in Eclipse with Processing libraries on classpath.
 */
public class PASamplerInstrumentTestHarness extends PApplet {

    private Minim minim;
    private AudioOutput out;
    private PASamplerInstrument instrument;
    private MultiChannelBuffer buffer;

    // screen size (also used for simple mapping)
    private static final int WIDTH = 768;
    private static final int HEIGHT = 512;
    
	String dataPath = "/Users/paulhz/Code/Workspace/TestProcessing/src/net/paulhertz/workflow/ariadata/";
	String audioStartFile = dataPath + "Aria_1.wav";


    @Override
    public void settings() {
        size(WIDTH, HEIGHT);
    }

    @Override
    public void setup() {
        surface.setTitle("WFSamplerInstrument Test Harness");

        // init Minim
        minim = new Minim(this);
        out = minim.getLineOut();
       
        // load audio into MultiChannelBuffer
        // Replace with a valid file path
        String audioPath = audioStartFile;  // put a test file here
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            System.err.println("Audio file not found: " + audioPath);
            exit();
        }

        buffer = new MultiChannelBuffer(1, 1); // dummy init
        minim.loadFileIntoBuffer(audioPath, buffer);

        // default ADSR params: maxAmp, attack, decay, sustain, release
        ADSRParams env = new ADSRParams(
            0.8f,   // maxAmp
            0.01f,  // attack (s)
            0.1f,   // decay (s)
            0.7f,   // sustain level
            0.3f    // release (s)
        );

        // construct instrument with 8 voices
        instrument = new PASamplerInstrument(buffer, out.sampleRate(), 8, out, env);

        background(0);
        fill(255);
        text("Click anywhere to trigger audio slices.", 20, 20);
    }

    @Override
    public void draw() {
        // simple oscilloscope view
        stroke(0, 255, 0);
        for (int i = 0; i < out.bufferSize() - 1; i++) {
            line(i, HEIGHT/2 + out.left.get(i) * 50,
                 i+1, HEIGHT/2 + out.left.get(i+1) * 50);
        }
    }

    @Override
    public void mousePressed() {
        if (instrument == null) return;

        // map mouseX to sample position
        int samplePos = (int) map(mouseX, 0, WIDTH, 0, instrument.getBufferSize());
        int sampleLen = 20000; // ~0.5s slice at 44.1kHz

        float amplitude = 0.8f;
        instrument.playSample(samplePos, sampleLen, amplitude);

        // visual feedback
        fill(255, 0, 0, 128);
        noStroke();
        ellipse(mouseX, mouseY, 20, 20);
    }

    @Override
    public void stop() {
        if (instrument != null) instrument.close();
        if (minim != null) minim.stop();
        super.stop();
    }

    // entry point for running from Eclipse
    public static void main(String[] args) {
        PApplet.main(PASamplerInstrumentTestHarness.class);
    }
}
