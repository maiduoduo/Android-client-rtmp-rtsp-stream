package com.pedro.rtplibrary.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.view.Surface;
import com.pedro.encoder.input.gl.SurfaceManager;
import com.pedro.encoder.input.gl.render.ManagerRender;
import com.pedro.encoder.input.gl.render.filters.BaseFilterRender;
import com.pedro.encoder.input.video.FpsLimiter;
import com.pedro.encoder.utils.gl.GlUtil;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Created by pedro on 4/03/18.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class OffScreenGlThread
    implements GlInterface, Runnable, SurfaceTexture.OnFrameAvailableListener {

  private final Context context;
  private Thread thread = null;
  private boolean frameAvailable = false;
  private boolean running = true;
  private boolean initialized = false;

  private SurfaceManager surfaceManager = null;
  private SurfaceManager surfaceManagerEncoder = null;

  private ManagerRender textureManager = null;

  private final Semaphore semaphore = new Semaphore(0);
  private final BlockingQueue<Filter> filterQueue = new LinkedBlockingQueue<>();
  private final Object sync = new Object();
  private int encoderWidth, encoderHeight;
  private boolean loadAA = false;
  private int streamRotation;
  private boolean muteVideo = false;
  private boolean isStreamHorizontalFlip = false;
  private boolean isStreamVerticalFlip = false;

  private boolean AAEnabled = false;
  private FpsLimiter fpsLimiter = new FpsLimiter();
  //used with camera
  private TakePhotoCallback takePhotoCallback;

  public OffScreenGlThread(Context context) {
    this.context = context;
  }

  @Override
  public void init() {
    if (!initialized) textureManager = new ManagerRender();
    textureManager.setCameraFlip(false, false);
    initialized = true;
  }

  @Override
  public void setEncoderSize(int width, int height) {
    this.encoderWidth = width;
    this.encoderHeight = height;
  }

  @Override
  public void muteVideo() {
    muteVideo = true;
  }

  @Override
  public void unMuteVideo() {
    muteVideo = false;
  }

  @Override
  public boolean isVideoMuted() {
    return muteVideo;
  }

  @Override
  public void setFps(int fps) {
    fpsLimiter.setFPS(fps);
  }

  @Override
  public SurfaceTexture getSurfaceTexture() {
    return textureManager.getSurfaceTexture();
  }

  @Override
  public Surface getSurface() {
    return textureManager.getSurface();
  }

  @Override
  public void addMediaCodecSurface(Surface surface) {
    synchronized (sync) {
      surfaceManagerEncoder = new SurfaceManager(surface, surfaceManager);
    }
  }

  @Override
  public void removeMediaCodecSurface() {
    synchronized (sync) {
      if (surfaceManagerEncoder != null) {
        surfaceManagerEncoder.release();
        surfaceManagerEncoder = null;
      }
    }
  }

  @Override
  public void takePhoto(TakePhotoCallback takePhotoCallback) {
    this.takePhotoCallback = takePhotoCallback;
  }

  @Override
  public void setFilter(int filterPosition, BaseFilterRender baseFilterRender) {
    filterQueue.add(new Filter(filterPosition, baseFilterRender));
  }

  @Override
  public void setFilter(BaseFilterRender baseFilterRender) {
    setFilter(0, baseFilterRender);
  }

  @Override
  public void enableAA(boolean AAEnabled) {
    this.AAEnabled = AAEnabled;
    loadAA = true;
  }

  @Override
  public void setRotation(int rotation) {
    textureManager.setCameraRotation(rotation);
  }

  @Override
  public void setStreamRotation(int rotation) {
    streamRotation = rotation;
  }

  @Override
  public void setIsStreamHorizontalFlip(boolean flip) {
    isStreamHorizontalFlip = flip;
  }

  @Override
  public void setIsStreamVerticalFlip(boolean flip) {
    isStreamVerticalFlip = flip;
  }

  @Override
  public boolean isAAEnabled() {
    return textureManager != null && textureManager.isAAEnabled();
  }

  @Override
  public void start() {
    synchronized (sync) {
      thread = new Thread(this);
      running = true;
      thread.start();
      semaphore.acquireUninterruptibly();
    }
  }

  @Override
  public void stop() {
    synchronized (sync) {
      if (thread != null) {
        thread.interrupt();
        try {
          thread.join(100);
        } catch (InterruptedException e) {
          thread.interrupt();
        }
        thread = null;
      }
      running = false;
    }
  }

  private void releaseSurfaceManager() {
    if (surfaceManager != null) {
      surfaceManager.release();
      surfaceManager = null;
    }
  }

  @Override
  public void run() {
    releaseSurfaceManager();
    surfaceManager = new SurfaceManager();
    surfaceManager.makeCurrent();
    textureManager.initGl(context, encoderWidth, encoderHeight, encoderWidth, encoderHeight);
    textureManager.getSurfaceTexture().setOnFrameAvailableListener(this);
    semaphore.release();
    try {
      while (running) {
        if (frameAvailable) {
          frameAvailable = false;
          surfaceManager.makeCurrent();
          textureManager.updateFrame();
          textureManager.drawOffScreen();
          textureManager.drawScreen(encoderWidth, encoderHeight, false, 0, 0, true, false, false);
          surfaceManager.swapBuffer();

          synchronized (sync) {
            if (surfaceManagerEncoder != null && !fpsLimiter.limitFPS()) {
              surfaceManagerEncoder.makeCurrent();
              if (muteVideo) {
                textureManager.drawScreen(0, 0, false, 0, streamRotation, false,
                    isStreamVerticalFlip, isStreamHorizontalFlip);
              } else {
                textureManager.drawScreen(encoderWidth, encoderHeight, false, 0, streamRotation,
                    false, isStreamVerticalFlip, isStreamHorizontalFlip);
              }
              //Necessary use surfaceManagerEncoder because preview manager size in background is 1x1.
              if (takePhotoCallback != null) {
                takePhotoCallback.onTakePhoto(
                    GlUtil.getBitmap(encoderWidth, encoderHeight, encoderWidth, encoderHeight));
                takePhotoCallback = null;
              }
              surfaceManagerEncoder.swapBuffer();
            }
          }
          if (!filterQueue.isEmpty()) {
            Filter filter = filterQueue.take();
            textureManager.setFilter(filter.getPosition(), filter.getBaseFilterRender());
          } else if (loadAA) {
            textureManager.enableAA(AAEnabled);
            loadAA = false;
          }
        }
      }
    } catch (InterruptedException ignore) {
      Thread.currentThread().interrupt();
    } finally {
      textureManager.release();
      releaseSurfaceManager();
    }
  }

  @Override
  public void onFrameAvailable(SurfaceTexture surfaceTexture) {
    synchronized (sync) {
      frameAvailable = true;
      sync.notifyAll();
    }
  }
}
