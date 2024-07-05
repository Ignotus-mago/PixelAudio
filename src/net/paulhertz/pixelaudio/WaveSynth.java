package net.paulhertz.pixelaudio;

import processing.core.PImage;

public class WaveSynth {
	// WaveSynth objects
	public PixelAudioMapper mapper;
	public PImage image;
	public int[] colorSignal;
	public float[] signal;
	// WaveSYnth control variables
	public float gain;
	public float gamma;
	public boolean isScaleHisto = false;
	public int histoLow;
	public int histoHigh;
	public int animSteps = 720;
	public int step = 0;
	/** comments for JSON file */
	public String comments = "---";
	// animation variables
	/** increment in phase over the image pixels, typically TWO_PI / image size */
	public float mapInc;
	/** array of amplitudes associated with the WaveData operators */
	public float[] weights;
	/** array of colors associated with the WaveData operators */
	public int[] waveColors;
	/** array of color values for mask, especially useful when it is constant */
	public int[] maskScan;
	/** offset for normailzing signal, see renderFrame method */
	public float woff = 1.0f;
	/** scaling facotr for normalizing signal, see renderFrame method */
	public float wscale = 0.5f;
	// may be extraneous
	public int videoFramerate = 24;
	public String videoFilename = "motion_study.mp4";


	public WaveSynth() {

	}

}
