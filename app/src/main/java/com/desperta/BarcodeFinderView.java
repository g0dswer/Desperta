package com.desperta;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Portrait scanner overlay with a landscape framing window and dimmed outside area. */
public final class BarcodeFinderView extends View {
  public enum FeedbackState {
    NEUTRAL,
    ERROR,
    SUCCESS
  }

  public interface FrameListener {
    void onFrameChanged(int width, int height);
  }

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF frame = new RectF();
  private final Identity theme;
  private FrameListener frameListener;
  private int frameWidth;
  private int frameHeight;
  private FeedbackState feedbackState = FeedbackState.NEUTRAL;

  public BarcodeFinderView(Context context) {
    super(context);
    theme = Identity.current(context);
    setWillNotDraw(false);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  public void setFrameListener(FrameListener listener) {
    frameListener = listener;
    if (frameWidth > 0 && frameHeight > 0 && frameListener != null) {
      frameListener.onFrameChanged(frameWidth, frameHeight);
    }
  }

  /** Updates the border without interrupting the camera stream after a decoded result. */
  public void setFeedbackState(FeedbackState state) {
    feedbackState = state == null ? FeedbackState.NEUTRAL : state;
    invalidate();
  }

  @Override
  protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
    super.onSizeChanged(width, height, oldWidth, oldHeight);
    int horizontalMargin = dp(36);
    frameWidth = Math.max(dp(220), Math.min(width - horizontalMargin * 2, dp(340)));
    frameHeight = Math.max(dp(128), Math.round(frameWidth * 0.63f));
    float centerY = height * 0.5f;
    frame.set(
        (width - frameWidth) / 2f,
        centerY - frameHeight / 2f,
        (width + frameWidth) / 2f,
        centerY + frameHeight / 2f);
    if (frameListener != null) frameListener.onFrameChanged(frameWidth, frameHeight);
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (frame.isEmpty()) return;

    paint.setStyle(Paint.Style.FILL);
    paint.setColor(0xB8000000);
    canvas.drawRect(0, 0, getWidth(), frame.top, paint);
    canvas.drawRect(0, frame.bottom, getWidth(), getHeight(), paint);
    canvas.drawRect(0, frame.top, frame.left, frame.bottom, paint);
    canvas.drawRect(frame.right, frame.top, getWidth(), frame.bottom, paint);

    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(dp(4));
    if (feedbackState == FeedbackState.SUCCESS) {
      paint.setColor(theme.positive);
    } else if (feedbackState == FeedbackState.ERROR) {
      paint.setColor(theme.error);
    } else {
      paint.setColor(theme.accent);
    }
    canvas.drawRoundRect(frame, dp(6), dp(6), paint);
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private float dp(float value) {
    return value * getResources().getDisplayMetrics().density;
  }
}
