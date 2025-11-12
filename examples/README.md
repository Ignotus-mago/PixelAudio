As of November 12, 2025, we have these examples, in the suggested order for learning about PixelAudio:

- **LookupTables**: an introduction to a core concept in PixelAudio, lookup tables. Start here. 
- **Starter**: basics of creating a PixelMapGen instance and plugging it into a PixelAudioMapper.
- **QuickAnimation**: a simple way to animate a bitmap using PixelAudioMapper.
- **TransformPimage**: introduces the affine transforms available in the BitmapTransform class.
- **MultiGenDemo**: chain PixelMapGens together to generate a large image.
- **MultiGenLookupTables**: lookup tables in MultiGens, a useful place to test your MultiGenCode.
- **Tutorial Folder**
   1. **TutorialOne_00_BeginHere**: Basics of creating a MultiGen and a PixelAudioMapper.
   2. **TutorialOne_01_FileIO**: Load audio and image files, turn images into sound, sound into images.
   3. **TutorialOne_02_SkipThis**: Just skip this, it's a placeholder.
   4. **TutorialOne_03_Animation**: Add animation and video output.
   5. **TutorialOne_04_Drawing**: Create interactive drawing tools to trigger audio events.
   6. **TutorialOne_05_UDP**: Communicate with Max and other media apps with UDP. 
   7. **TutorialOne_06_WindowBuffer**: Use a windowed buffer to move through and interact with a large audio file.
- **LoadAudioToImage**: read an audio file, trancode it into an image and then play audio samples. 
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

You can open library examples with the Processing File menu->Examples... command. 
Look for PixelAudio in the Look for PixelAudio in the Contributed LIbraries in the Java Examples. 
This is the recommended way to explore PixelAudio features. 
To modify an example sketch, save it to your sketches folder. 
Files opened from Contributed Libraries can only be saved to new sketches. 
