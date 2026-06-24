As of June 24, 2026, we have these examples, in the suggested order for learning about PixelAudio:

- **LookupTables**: an introduction to a core concept in PixelAudio, lookup tables. Start here. 
- **Starter**: basics of creating a PixelMapGen instance and plugging it into a PixelAudioMapper.
- **SimpleAnimation**: a simple way to animate a bitmap using PixelAudioMapper.
- **MultiGenDemo**: chain PixelMapGens together to generate a large image.
- **MultiGenLookupTables**: lookup tables in MultiGens, a useful place to test your MultiGenCode.
- **TransformPimage** (optional): introduces the affine transforms available in the BitmapTransform class.
- **Tutorial Folder**
   1. **TutorialOne_01_FileIO**: Load audio and image files, turn images into sound, sound into images.
   2. **TutorialOne_02_Animation**: Add animation and video output.
   3. **TutorialOne_03_Drawing**: Create interactive drawing tools to trigger audio events.
   4. **TutorialOne_04_Network**: Communicate with Max and other media apps with UDP. 
   5. **TutorialOne_05_GesturePlayground**: Explore granular and sampler synthesis with a GUI.
   6. **TutorialOne_06_WindowBuffer**: Use a windowed buffer to interact with a large audio file.
- **LoadAudioToImage**: read an audio file, transcode it into an image and then play audio samples. 
- **LoadImageToAudio**: read, write, play, and mix channels of images and audio files. 
- **BuildFromPathGenDemo**: read and write data from a PixelMapGen in JSON format. 
- **SimpleWaveSynth**: basics of creating a WaveSynth animation. 
- **JSONWaveSynth**: read and write JSON data to configure a WaveSynth.
- **BigWaveSynth** use a WaveSynth with a large bitmap mapped to a MultiGen. 
- **BigWaveSynthAudio** use a WaveSynth as an additive audio synthesizer, with droning and detuning.
- **WaveSynthEditor** use a GUI to load, edit, listen to, save and output WaveSynth data to video.
- **WaveSynthSequencer** use WaveSynth as an audio synthesizer to play sequences of notes.
- **ArgosyMixer**: create Argosy patterns with a GUI, play them as audio, export animations to video.
- **AudioCapture**: capture streaming or live audio and turn it into a bitmap.
- **Bagatelle**: a live performance application with a GUI, presets, JSON i/o, UDP networking, etc.

The tutorial is designed to get you up and running with creating your own sampling audio synth, where you draw on an image to generate non-linear samples from an audio file. You can load your own audio and image files instead of the ones in the examples. Outside the tutorial sequence, the ArgosyMixer and WaveSynthEditor can also be used without digging into the code. Each provides a GUI for exploring additive audio synthesis that also acts as a color organ (WaveSynth) and pattern generation that can produce both control pulses and audio (Argosy). ArgosyMixer and WaveSynthEditor can also output video animation files. 

**Bagatelle** is a live performance application with a GUI, presets, JSON i/o  for brushstroke and audio synthesis settings, UDP communications and wide range of options. Offered as an experimental sketch, it shows some of the capabilities of PixelAudio for performance. 

If you are interested in combining PixelAudio output with other applications, the AudioCapture sketch provides information about audio signal routing for MacOS, and TutorialOne_04_Networking provides some clues about communicating with other applications using UDP. If you want to capture audio and video from PixelAudio examples, I suggest OBS (Open Broadcast System) as one way to do this.

You can open library examples with the Processing File menu->Examples... command. 
Look for PixelAudio in the Contributed LIbraries in the Java Examples. 
To modify an example sketch, first save it to your sketches folder. 
Files opened from Contributed Libraries can only be saved to new sketches. 

All of the Processing example sketches except for AudioCapture were developed as Java classes in Eclipse. The Javadocs for the Java examples are repeated in the Processing sketches in a format modified for Processing. It may prove useful to open the Javadocs for a sketch that you are running in Processing--you won't have to scroll in Processing to find instructions and key commands. The javadocs for Package `net.paulhertz.pixelaudio.example` list all the example code, with links to each Java example.
