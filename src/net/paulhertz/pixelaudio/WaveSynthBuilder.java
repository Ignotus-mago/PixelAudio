/**
 * 
 */
package net.paulhertz.pixelaudio;

import java.util.ArrayList;

/**
 * 
 */
public class WaveSynthBuilder {
	public static final double semitoneFac = Math.pow(2, 1 / 12.0);

	/**
	 * Generates an ArrayList of WaveData objects to be used by a WaveSynth to
	 * generate RGB pixel values and (on request) audio signal values.
	 *
	 * @return an ArrayList of WaveData objects
	 */
	public static ArrayList<WaveData> synthTrumpet(float fundamental, int howManyPartials, float pianoKey, int animSteps) {
		ArrayList<WaveData> list = new ArrayList<WaveData>();
		if (howManyPartials < 1)
			howManyPartials = 1;
		// funda is the fundamental of a musical tone that is somewhat like a trumpet
		// in its frequency spectrum. Vary it to see how the image and sound change.
		float funda = fundamental;
		float frequency = funda;
		float amplitude = 0.55f;
		float phase = 0.766f;
		float dc = 0.0f;
		float cycles = -8.0f;
		int waveColor = PixelAudioMapper.composeColor(0, 89, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		int steps = animSteps;
		WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 1)
			return list;
		frequency = 2 * funda;
		amplitude = 0.52f;
		phase = -0.89f;
		cycles = 8.0f;
		waveColor = PixelAudioMapper.composeColor(89, 199, 55);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 2)
			return list;
		frequency = 3 * funda;
		amplitude = 0.6f;
		phase = -0.486f;
		cycles = 3.0f;
		waveColor = PixelAudioMapper.composeColor(254, 89, 110);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 3)
			return list;
		frequency = 4 * funda;
		amplitude = 0.45f;
		phase = -0.18616974f;
		cycles = -2.0f;
		waveColor = PixelAudioMapper.composeColor(89, 110, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 4)
			return list;
		frequency = 5 * funda;
		amplitude = 0.42f;
		phase = 0.6846085f;
		cycles = -5.0f;
		waveColor = PixelAudioMapper.composeColor(233, 34, 21);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 5)
			return list;
		frequency = 6 * funda;
		amplitude = 0.45f;
		phase = 0.68912f;
		cycles = 13.0f;
		waveColor = PixelAudioMapper.composeColor(220, 199, 55);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 6)
			return list;
		frequency = 7 * funda;
		amplitude = 0.25f;
		phase = 0.68f;
		cycles = 11.0f;
		waveColor = PixelAudioMapper.composeColor(159, 190, 255);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		if (howManyPartials == 7)
			return list;
		frequency = 8 * funda;
		amplitude = 0.32f;
		phase = 0.68f;
		cycles = -11.0f;
		waveColor = PixelAudioMapper.composeColor(209, 178, 117);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		return list;
	}
	
	/**
	 * Generates an ArrayList of WaveData objects to be used by a WaveSynth to
	 * generate RGB pixel values and (on request) audio signal values.
	 *
	 * @return an ArrayList of WaveData objects
	 */
	public static ArrayList<WaveData> synthDoubleStop(float funda1, float funda2, float pianoKey1, float pianoKey2, int animSteps) {
		ArrayList<WaveData> list = new ArrayList<WaveData>();
		float detune = 0.0625f;
		//
		// First Stop
		float funda = funda1;
		float pianoKey = pianoKey1;
		float frequency = funda1;
		float amplitude = 0.6f;
		float phase = 0.75f;
		float dc = 0.0f;
		float cycles = 1.0f;
		int waveColor = PixelAudioMapper.composeColor(0, 89, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		int steps = animSteps;
		WaveData wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		frequency = 2 * funda;
		amplitude = 0.4f;
		phase = -0.5f;
		cycles = -2.0f;
		waveColor = PixelAudioMapper.composeColor(89, 199, 55);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		frequency = 4.0f * funda + detune;
		amplitude = 0.3f;
		phase = -0.125f;
		cycles = 3.0f;
		waveColor = PixelAudioMapper.composeColor(89, 110, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		// Second Stop
		funda = funda2;
		pianoKey = pianoKey2;
		frequency = funda2;
		amplitude = 0.6f;
		phase = 0.5f;
		dc = 0.0f;
		cycles = -1.0f;
		waveColor = PixelAudioMapper.composeColor(0, 89, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		//
		frequency = 2 * funda;
		amplitude = 0.4f;
		phase = 0.75f;
		cycles = 2.0f;
		waveColor = PixelAudioMapper.composeColor(89, 199, 55);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		//
		frequency = 4  * funda - detune;
		amplitude = 0.3f;
		phase = 0.125f;
		cycles = -3.0f;
		waveColor = PixelAudioMapper.composeColor(89, 110, 233);
		waveColor = PixelAudioMapper.colorShift(waveColor, (pianoKey % 12) / 12.0f);
		wd = new WaveData(frequency, amplitude, phase, dc, cycles, waveColor, steps);
		list.add(wd);
		return list;
	}


	public static float[] chromaticScale(float funda) {
		float[] chromaScale = new float[12];
		for (int i = 0; i < chromaScale.length; i++) {
			chromaScale[i] = funda;
			funda *= (float)(funda * semitoneFac);
		}
		return chromaScale;
	}

	public static float pianoKeyFrequency(float keyNumber) {
		return (float) (440 * Math.pow(2, (keyNumber - 49) / 12.0));
	}

	public static float frequencyPianoKey(float freq) {
		return 49 + 12 * (float) (Math.log(freq / 440) / Math.log(2));
	}

}
