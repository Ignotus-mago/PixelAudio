/*----------------------------------------------------------------*/
/*                                                                */
/*                       FILE I/O METHODS                         */
/*                                                                */
/*----------------------------------------------------------------*/

/**
 * Wrapper method for Processing's selectInput command
 */
public void chooseFile() {
  oldIsAnimating = isAnimating;
  isAnimating = false;
  selectInput("Choose an audio, image, or JSON file: ", "fileSelected");
}

/**
 * callback method for chooseFile(), handles standard audio and image formats for Processing.
 * If a file has been successfully selected, continues with a call to loadAudioFile() or loadImageFile().
 *
 * @param selectedFile    the File the user selected
 */
public void fileSelected(File selectedFile) {
  if (null != selectedFile) {
    String filePath = selectedFile.getAbsolutePath();
    String fileName = selectedFile.getName();
    String fileTag = fileName.substring(fileName.lastIndexOf('.') + 1);
    fileName = fileName.substring(0, fileName.lastIndexOf('.'));
    if (fileTag.equalsIgnoreCase("json")) {
      jsonFileSelectedRead(selectedFile);
    } else if (fileTag.equalsIgnoreCase("mp3") || fileTag.equalsIgnoreCase("wav") || fileTag.equalsIgnoreCase("aif")
      || fileTag.equalsIgnoreCase("aiff")) {
      // we chose an audio file
      audioFile = selectedFile;
      audioFilePath = filePath;
      audioFileName = fileName;
      audioFileTag = fileTag;
      println("----- Selected file " + fileName + "." + fileTag + " at "
        + filePath.substring(0, filePath.length() - fileName.length()));
      loadAudioFile(audioFile);
      // *****>>> NETWORKING <<<***** //
      if (nd != null && isNetSendFileInfo) nd.oscSendFileInfo(filePath, fileName, fileTag);
    } else if (fileTag.equalsIgnoreCase("png") || fileTag.equalsIgnoreCase("jpg")
      || fileTag.equalsIgnoreCase("jpeg")) {
      // we chose an image file
      imageFile = selectedFile;
      imageFilePath = filePath;
      imageFileName = fileName;
      imageFileTag = fileTag;
      loadImageFile(imageFile);
    } else {
      println("----- File is not a recognized format ending with"
        +"\"mp3\", \"wav\", \"aif\", \"aiff\", \"png\", \"jpg\", \"jpeg\" or \"json\" .");
    }
  } else {
    println("----- No audio, image, or JSON file was selected.");
  }
  isAnimating = oldIsAnimating;
}

/**
 * Attempts to load audio data from a selected file into playBuffer, then calls
 * writeAudioToImage() to transcode audio data and write it to mapImage.
 * Resamples files that are recorded with a different sample rate than the current audio output.
 * If you want to load the image file and audio file separately, comment out writeAudioToImage().
 *
 * @param audFile    an audio file
 */
public void loadAudioFile(File audFile) {
  MultiChannelBuffer buff = new MultiChannelBuffer(1024, 1);
  fileSampleRate =  minim.loadFileIntoBuffer(audFile.getAbsolutePath(), buff);
  if (fileSampleRate <= 0) {
    println("-- Unable to load file. File may be empty, wrong format, or damaged.");
    return;
  }
  float sig[];
  if (fileSampleRate != audioOut.sampleRate()) {
    sig = AudioUtility.resampleMonoToOutput(buff.getChannel(0), fileSampleRate, audioOut);
    bufferSampleRate = sampleRate;
  } else {
    sig = Arrays.copyOf(buff.getChannel(0), buff.getBufferSize());
    bufferSampleRate = fileSampleRate;
  }
  this.audioFileLength = sig.length;
  println("---- file sample rate = "+ this.fileSampleRate
    +", buffer sample rate = "+ bufferSampleRate
    +", audio output sample rate = "+ audioOut.sampleRate());
  ensureSamplerReady();
  updateAudioChain(sig);
  if (isLoadToBoth) {
    println("---- loading to both ----");
    writeAudioToImage(audioSignal, mapper, mapImage, chan);
    commitMapImageToBaseImage();
    if (applyColorMapOnLoad) applyColorMapToDisplay(true);
  }
  totalShift = 0;    // reset animation shift when audio is reloaded
}

/**
 * Attempts to load image data from a selected file into mapImage, then calls writeImageToAudio()
 * to transcode HSB brightness channel to audio and writes it to playBuffer and audioSignal.
 *
 * @param imgFile    an image file
 */
public void loadImageFile(File imgFile) {
  PImage img = loadImage(imgFile.getAbsolutePath());
  // stash information about the image in imgFileWidth, imageFileHeight for future use
  imageFileWidth = img.width;
  imageFileHeight = img.height;
  // calculate w and h for copying image to display (mapImage)
  int w = img.width > mapImage.width ? mapImage.width : img.width;
  int h = img.height > mapImage.height ? mapImage.height : img.height;
  if (chan == PixelAudioMapper.ChannelNames.ALL) {
    if (isBlending) {
      PImage dest = mapImage;
      PImage src = img;
      src.loadPixels();
      for (int i = 0; i < src.pixels.length; i++) {
        int pixel = src.pixels[i];
        src.pixels[i] = setAlphaWithBlack(pixel, 96);
      }
      src.updatePixels();
      dest.blend(src, 0, 0, src.width, src.height, 0, 0, dest.width, dest.height, BLEND);
    } else {
      // copy the image directly using Processing copy command
      mapImage.copy(img, 0, 0, w, h, 0, 0, w, h);
    }
  } else {
    // copy only specified channels of the new image
    PImage mixImage = createImage(w, h, RGB);
    mixImage.copy(mapImage, 0, 0, w, h, 0, 0, w, h);
    img.loadPixels();
    mixImage.loadPixels();
    mixImage.pixels = PixelAudioMapper.pushChannelToPixel(img.pixels, mixImage.pixels, chan);
    mixImage.updatePixels();
    mapImage.copy(mixImage, 0, 0, w, h, 0, 0, w, h);
  }
  if (isLoadToBoth) {
    float[] sig = new float[mapper.getSize()];
    // preserve previous signal for possible crossfade / layering
    float[] prev = audioSignal;
    audioSignal = sig;
    renderMapImageToAudio(PixelAudioMapper.ChannelNames.L);
    updateAudioChain(sig);
  }
  commitMapImageToBaseImage();
  if (applyColorMapOnLoad) applyColorMapToDisplay(true);
}


/**
 * Transcodes audio data in sig[] and writes it to color channel chan of mapImage
 * using the lookup tables in mapper to redirect indexing. Calls mapper.mapSigToImg(),
 * which will throw an IllegalArgumentException if sig.length != img.pixels.length
 * or sig.length != mapper.getSize().
 *
 * @param sig         an array of float, should be audio data in the range [-1.0, 1.0]
 * @param mapper      a PixelAudioMapper
 * @param img         a PImage
 * @param chan        a color channel
 */
public void writeAudioToImage(float[] sig, PixelAudioMapper mapper, PImage img, PixelAudioMapper.ChannelNames chan) {
  // If sig.length == mapper.getSize() == mapImage.width * mapImage.height, we can call safely mapper.mapSigToImg()
  img.loadPixels();
  mapper.mapSigToImg(sig, img.pixels, chan);
  img.updatePixels();
}

/**
 * Sets the alpha channel of an RGBA color, conditionally setting alpha = 0 if all other channels = 0.
 *
 * @param argb     an RGBA color value
 * @param alpha    the desired alpha value to apply to argb
 * @return         the argb color with changed alpha channel value
 */
public int setAlphaWithBlack(int argb, int alpha) {
  int[] c = PixelAudioMapper.rgbaComponents(argb);
  if (c[0] == c[1] && c[1] == c[2] && c[2] == 0) {
    alpha = 0;
  }
  return alpha << 24 | c[0] << 16 | c[1] << 8 | c[2];
}

/**
 * Sets the alpha channel of an RGBA color.
 *
 * @param argb     an RGBA color value
 * @param alpha    the desired alpha value to apply to argb
 * @return         the argb color with changed alpha channel value
 */
public static int setAlpha(int argb, int alpha) {
  return (argb & 0x00FFFFFF) | (alpha << 24);
}

/**
 * This method writes a color channel from an image to playBuffer, fulfilling a
 * central concept of the PixelAudio library: image is sound. Calls mapper.mapImgToSig(),
 * which will throw an IllegalArgumentException if img.pixels.length != sig.length or
 * img.width * img.height != mapper.getWidth() * mapper.getHeight().
 * Sets totalShift = 0 on completion: the image and audio are now in sync. TODO
 *
 * @param img       a PImage, a source of data
 * @param mapper    a PixelAudioMapper, handles mapping between image and audio signal
 * @param sig       an target array of float in audio format
 * @param chan      a color channel
 * @param shift     number of indices to shift
 */
public void writeImageToAudio(PImage img, PixelAudioMapper mapper, float[] sig, PixelAudioMapper.ChannelNames chan, int shift) {
  // If img is the *display* (shifted) image, commit its phase into audio:
  sig = mapper.mapImgToSigShifted(img.pixels, sig, chan, shift);
}

/**
 * Writes a specified channel of mapImage to audioSignal.
 *
 * @param chan    the selected color channel
 */
public void renderMapImageToAudio(PixelAudioMapper.ChannelNames chan) {
  writeImageToAudio(mapImage, mapper, audioSignal, chan, totalShift);
}

/**
 * Writes the mapImage, which may change with animation, to the baseImage, a reference image
 * that usually only changes when a new file is loaded.
 */
public void commitMapImageToBaseImage() {
  baseImage = mapImage.copy();
  totalShift = 0;
}

/**
 * Copies the supplied PImage to mapImage and baseImage, sets totalShift to 0 (the images are identical).
 * @param img
 */
public void commitNewBaseImage(PImage img) {
  baseImage = img.copy();
  mapImage = img.copy();
  totalShift = 0;
}

/**
 * Writes baseImage to mapImage with an index position offset of totalShift.
 */
public void refreshMapImageFromBase() {
  mapImage.loadPixels();
  mapper.copyPixelsAlongPathShifted(baseImage.pixels, mapImage.pixels, totalShift);
  mapImage.updatePixels();
}


// ------------- JSON FILE I/O -------------

void saveGestureJSON(boolean saveSession) {
  if (saveSession) selectOutput("Select a JSON Session file to write to:", "jsonFileSelectedWrite");
  else selectOutput("Select a JSON Brush file to write to:", "jsonFileSelectedWrite");
}

/**
 * Save the curve and config data from  the current activeBrush.
 *
 * @param jsonFile    initially, the file to save to (naming conventions may change the full name)
 */
public void jsonFileSelectedWrite(File jsonFile) {
  if (jsonFile == null) {
    println("Window was closed or cancelled.");
    return;
  }
  if (isSaveSession) {
    saveSessionJSON(jsonFile);
    return;
  }
  if (activeBrush == null) {
    println("-- No active brush found.");
    return;
  }
  AudioBrushFileNamer.Result files = AudioBrushFileNamer.build(jsonFile);
  File gestureFile = files.gestureFile;
  File configFile  = files.configFile;
  try {
    // ---------- config ----------
    GestureGranularConfig.Builder cfg = activeBrush.cfg().copy();
    GestureGranularConfigIO.Meta configMeta = new GestureGranularConfigIO.Meta();
    configMeta.id = "1";
    configMeta.linkedGesturePath = files.linkedGesturePath;
    if (activeBrush instanceof GranularBrush)
      configMeta.instrumentType =
        GestureGranularConfigIO.InstrumentType.GRANULAR;
    else
      configMeta.instrumentType =
        GestureGranularConfigIO.InstrumentType.SAMPLER;
    GestureGranularConfigIO.write(configFile, cfg, configMeta);
    // ---------- gesture ----------
    PACurveMakerIO.Meta meta = new PACurveMakerIO.Meta();
    meta.id = "1";
    meta.name = files.baseName;
    meta.includeStyle = false;
    meta.linkedConfigPath = files.linkedConfigPath;
    PACurveMakerIO.write(gestureFile, activeBrush.curve(), meta);
  }
  catch (Exception e) {
    println("--->> Error writing JSON: " + e.getMessage());
  }
}

public void saveSessionJSON(File jsonFile) {
  if (jsonFile == null) {
    println("Window was closed or cancelled.");
    return;
  }
  AudioBrushFileNamer.Result files = AudioBrushFileNamer.build(jsonFile);
  File sessionFile = files.sessionFile;
  List<AudioBrush> allBrushes = new ArrayList<>();
  allBrushes.addAll(granularBrushes);
  allBrushes.addAll(samplerBrushes);
  if (allBrushes.isEmpty()) {
    println("-- No brushes to save.");
    return;
  }
  AudioBrushSessionIO.SessionMeta meta = new AudioBrushSessionIO.SessionMeta();
  meta.id = files.baseName;
  meta.name = files.baseName;
  meta.description = "DeadBodyWorkFlow session";
  meta.audioFilePath = audioFilePath;
  meta.audioFileName = audioFileName;
  meta.audioFileTag = audioFileTag;

  AudioBrushSessionIO.BrushAdapter<AudioBrush> adapter =
    new AudioBrushSessionIO.BrushAdapter<>() {

    @Override
      public PACurveMaker curveOf(AudioBrush b) {
      return b.curve();
    }

    @Override
      public GestureGranularConfig.Builder configOf(AudioBrush b) {
      return b.cfg();
    }

    @Override
      public GestureGranularConfigIO.InstrumentType instrumentTypeOf(AudioBrush b) {
      return (b instanceof GranularBrush)
        ? GestureGranularConfigIO.InstrumentType.GRANULAR
        : GestureGranularConfigIO.InstrumentType.SAMPLER;
    }

    @Override
      public String idOf(AudioBrush b) {
      int idx;
      if (b instanceof GranularBrush) {
        idx = granularBrushes.indexOf(b);
        return "granular_" + idx;
      } else {
        idx = samplerBrushes.indexOf(b);
        return "sampler_" + idx;
      }
    }

    @Override
      public String nameOf(AudioBrush b) {
      return idOf(b);
    }

    @Override
      public boolean includeStyle(AudioBrush b) {
      return false;
    }
  };

  try {
    AudioBrushSessionIO.writeSession(
      sessionFile,
      allBrushes,
      adapter,
      meta,
      "gestures",
      "configs"
      );
    println("-- Saved session to " + sessionFile.getAbsolutePath());
  }
  catch (IOException e) {
    println("--->> Error writing session JSON: " + e.getMessage());
  }
}

void loadGestureJSON() {
  oldIsAnimating = isAnimating;
  isAnimating = false;
  selectInput("Choose a JSON file: ", "jsonFileSelectedRead");
}

public void jsonFileSelectedRead(File jsonFile) {
  if (jsonFile == null) return;
  try {
    AudioBrushSessionLoader.LoadResult result = AudioBrushSessionLoader.load(jsonFile);
    switch (result.type) {
    case GESTURE:
    case CONFIG:
      {
        PACurveMaker curve = result.brush.curve;
        GestureGranularConfig.Builder cfg =
          (result.brush.config != null) ? result.brush.config : gConfig.copy();
        if (curve == null) {
          println("-- No gesture data found in selected file or linked file.");
          return;
        }
        makeBrush(curve, cfg, result.brush.instrumentType);
        break;
      }
    case SESSION:
      {
        if (isReplaceBrushes) {
          granularBrushes.clear();
          samplerBrushes.clear();
        }
        for (AudioBrushSessionLoader.BrushData bd : result.brushes) {
          if (bd.curve == null) continue;
          GestureGranularConfig.Builder cfg =
            (bd.config != null) ? bd.config : gConfig.copy();
          makeBrush(bd.curve, cfg, bd.instrumentType);
        }
        if (result.sessionMeta != null) {
          audioFilePath = result.sessionMeta.audioFilePath;
          audioFileName = result.sessionMeta.audioFileName;
          audioFileTag = result.sessionMeta.audioFileTag;
          println("----->>> Audio file for "+ jsonFile.getName() +" is "+ audioFileName +"."+ audioFileTag);
        }
        break;
      }
    default:
      println("-- Unsupported JSON type.");
    }
    if (activeBrush != null) {
      if (activeBrush instanceof GranularBrush) {
        setMode(DrawingMode.DRAW_EDIT_GRANULAR);
        controlWindow.setTitle("Granular Synth");
      } else {
        setMode(DrawingMode.DRAW_EDIT_SAMPLER);
        controlWindow.setTitle("Sampler Synth");
      }
    }
    activeBrush = null;
  }
  catch (IOException e) {
    println("--->> There was an error reading the JSON file " + jsonFile + ", " + e.getMessage());
  }
}

void chooseGestureLibraryFolder() {
  selectFolder("Choose a gesture library folder:", "gestureLibraryFolderSelected");
}

public void gestureLibraryFolderSelected(File folder) {
  if (folder == null) {
    println("Folder selection cancelled.");
    return;
  }
  if (isReplaceBrushes) {
    granularBrushes.clear();
    samplerBrushes.clear();
  }
  AudioBrushLibraryLoader.LoadResult result =
    AudioBrushLibraryLoader.loadGestureLibrary(folder);
  int loadedCount = 0;
  for (AudioBrushLibraryLoader.BrushData bd : result.brushes) {
    if (bd.curve == null) continue;
    GestureGranularConfig.Builder cfg =
      (bd.config != null) ? bd.config : gConfig.copy();
    GestureGranularConfigIO.InstrumentType type =
      (bd.instrumentType != null)
      ? bd.instrumentType : GestureGranularConfigIO.InstrumentType.GRANULAR;
    makeBrush(bd.curve, cfg, type);
    loadedCount++;
  }
  println("-- Loaded " + loadedCount + " brush(es) from library folder: " + folder.getAbsolutePath());
  for (String msg : result.messages) {
    println("-- " + msg);
  }
}
