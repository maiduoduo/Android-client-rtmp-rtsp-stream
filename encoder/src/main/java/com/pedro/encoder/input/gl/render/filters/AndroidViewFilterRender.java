package com.pedro.encoder.input.gl.render.filters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.View;
import androidx.annotation.RequiresApi;
import com.pedro.encoder.R;
import com.pedro.encoder.utils.gl.GlUtil;
import com.pedro.encoder.utils.gl.TranslateTo;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by pedro on 4/02/18.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AndroidViewFilterRender extends BaseFilterRender {

  //rotation matrix
  private final float[] squareVertexDataFilter = {
      // X, Y, Z, U, V
      -1f, -1f, 0f, 0f, 0f, //bottom left
      1f, -1f, 0f, 1f, 0f, //bottom right
      -1f, 1f, 0f, 0f, 1f, //top left
      1f, 1f, 0f, 1f, 1f, //top right
  };

  private int program = -1;
  private int aPositionHandle = -1;
  private int aTextureHandle = -1;
  private int uMVPMatrixHandle = -1;
  private int uSTMatrixHandle = -1;
  private int uSamplerHandle = -1;
  private int uSamplerViewHandle = -1;

  private int[] viewId = new int[1];
  private View view;
  private SurfaceTexture surfaceTexture;
  private Surface surface;
  private Handler mainHandler;

  private int rotation;
  private float positionX, positionY;
  private float scaleX = 1f, scaleY = 1f;
  private float viewX, viewY;

  public AndroidViewFilterRender() {
    squareVertex = ByteBuffer.allocateDirect(squareVertexDataFilter.length * FLOAT_SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer();
    squareVertex.put(squareVertexDataFilter).position(0);
    Matrix.setIdentityM(MVPMatrix, 0);
    Matrix.setIdentityM(STMatrix, 0);
    mainHandler = new Handler(Looper.getMainLooper());
  }

  @Override
  protected void initGlFilter(Context context) {
    String vertexShader = GlUtil.getStringFromRaw(context, R.raw.simple_vertex);
    String fragmentShader = GlUtil.getStringFromRaw(context, R.raw.android_view_fragment);

    program = GlUtil.createProgram(vertexShader, fragmentShader);
    aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
    aTextureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
    uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
    uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix");
    uSamplerHandle = GLES20.glGetUniformLocation(program, "uSampler");
    uSamplerViewHandle = GLES20.glGetUniformLocation(program, "uSamplerView");

    GlUtil.createExternalTextures(1, viewId, 0);
    surfaceTexture = new SurfaceTexture(viewId[0]);
    surface = new Surface(surfaceTexture);
  }

  @Override
  protected void drawFilter() {
    surfaceTexture.setDefaultBufferSize(getPreviewWidth(), getPreviewHeight());
    if (view != null) {
      mainHandler.post(new Runnable() {
        @Override
        public void run() {
          Canvas canvas = surface.lockCanvas(null);
          canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
          canvas.translate(positionX, positionY);
          canvas.rotate(rotation, viewX / 2f, viewY / 2f);
          float scaleFactorX = (float) getPreviewWidth() / (float) view.getWidth();
          float scaleFactorY = (float) getPreviewHeight() / (float) view.getHeight();
          canvas.scale(scaleX * scaleFactorX, scaleY * scaleFactorY);
          view.draw(canvas);
          surface.unlockCanvasAndPost(canvas);
        }
      });
    }
    surfaceTexture.updateTexImage();

    GLES20.glUseProgram(program);

    squareVertex.position(SQUARE_VERTEX_DATA_POS_OFFSET);
    GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
        SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex);
    GLES20.glEnableVertexAttribArray(aPositionHandle);

    squareVertex.position(SQUARE_VERTEX_DATA_UV_OFFSET);
    GLES20.glVertexAttribPointer(aTextureHandle, 2, GLES20.GL_FLOAT, false,
        SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex);
    GLES20.glEnableVertexAttribArray(aTextureHandle);

    GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, MVPMatrix, 0);
    GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, STMatrix, 0);

    GLES20.glUniform1i(uSamplerHandle, 4);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, previousTexId);
    //android view
    GLES20.glUniform1i(uSamplerViewHandle, 5);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, viewId[0]);
  }

  @Override
  public void release() {
    GLES20.glDeleteProgram(program);
  }

  public View getView() {
    return view;
  }

  public void setView(final View view) {
    this.view = view;
    if (view != null) {
      view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
      viewX = view.getMeasuredWidth();
      viewY = view.getMeasuredHeight();
    }
  }

  /**
   *
   * @param x Position in percent
   * @param y Position in percent
   */
  public void setPosition(float x, float y) {
    int previewX = getPreviewWidth();
    int previewY = getPreviewHeight();
    this.positionX = previewX * x / 100f;
    this.positionY = previewY * y / 100f;
  }

  public void setPosition(TranslateTo positionTo) {
    int previewX = getPreviewWidth();
    int previewY = getPreviewHeight();
    switch (positionTo) {
      case TOP:
        this.positionX = previewX / 2f - (viewX / 2f);
        this.positionY = 0f;
        break;
      case LEFT:
        this.positionX = 0;
        this.positionY = previewY / 2f - (viewY / 2f);
        break;
      case RIGHT:
        this.positionX = previewX - viewX;
        this.positionY = previewY / 2f - (viewY / 2f);
        break;
      case BOTTOM:
        this.positionX = previewX / 2f - (viewX / 2f);
        this.positionY = previewY - viewY;
        break;
      case CENTER:
        this.positionX = previewX / 2f - (viewX / 2f);
        this.positionY = previewY / 2f - (viewY / 2f);
        break;
      case TOP_RIGHT:
        this.positionX = previewX - viewX;
        this.positionY = 0;
        break;
      case BOTTOM_LEFT:
        this.positionX = 0;
        this.positionY = previewY - viewY;
        break;
      case BOTTOM_RIGHT:
        this.positionX = previewX - viewX;
        this.positionY = previewY - viewY;
        break;
      case TOP_LEFT:
      default:
        this.positionX = 0;
        this.positionY = 0;
        break;
    }
  }

  public void setRotation(int rotation) {
    if (rotation < 0) {
      this.rotation = 0;
    } else if (rotation > 360) {
      this.rotation = 360;
    } else {
      this.rotation = rotation;
    }
  }

  public void setScale(float scaleX, float scaleY) {
    this.scaleX = scaleX;
    this.scaleY = scaleY;
  }
}
