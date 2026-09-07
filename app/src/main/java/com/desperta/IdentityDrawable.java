package com.desperta;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Draws the small visual grammar shared by an identity without replacing view content. */
final class IdentityDrawable extends Drawable {
  static final int BACKGROUND = 0;
  static final int PANEL = 1;
  static final int PRIMARY = 2;
  static final int SECONDARY = 3;
  static final int CRT_INK = 4;
  static final int DIGITAL_BEZEL = 5;

  private final Identity identity;
  private final Bitmap reference;
  private final int mode;
  private final float density;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF rect = new RectF();

  IdentityDrawable(Identity identity, int mode, float density, Bitmap reference) {
    this.identity = identity;
    this.mode = mode;
    this.density = density;
    this.reference = reference;
    paint.setStrokeCap(Paint.Cap.SQUARE);
  }

  @Override
  public void draw(Canvas canvas) {
    rect.set(getBounds());
    if (rect.width() <= 0 || rect.height() <= 0) return;
    if (mode == DIGITAL_BEZEL) {
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(0xFFC4C2BF);
      canvas.drawRect(rect, paint);
      bevel(canvas, rect, true);
      RectF inset = new RectF(rect);
      inset.inset(px(10), px(10));
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(0xFF000018);
      canvas.drawRect(inset, paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(px(4));
      paint.setColor(0xFF444444);
      canvas.drawLine(inset.left, inset.bottom, inset.left, inset.top, paint);
      canvas.drawLine(inset.left, inset.top, inset.right, inset.top, paint);
      paint.setColor(Color.WHITE);
      canvas.drawLine(inset.right, inset.top, inset.right, inset.bottom, paint);
      canvas.drawLine(inset.left, inset.bottom, inset.right, inset.bottom, paint);
      return;
    }
    if (mode == CRT_INK) {
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(Math.max(.7f, px(.32f)));
      paint.setColor(0xAD000000);
      for (float y = 1; y < rect.bottom; y += Math.max(2f, px(1.1f)))
        canvas.drawLine(rect.left, y, rect.right, y, paint);
      return;
    }
    if (mode == BACKGROUND) drawBackground(canvas);
    else if (mode == PANEL) drawPanel(canvas);
    else if (mode == PRIMARY) drawPrimary(canvas);
    else drawSecondary(canvas);
  }

  private void drawBackground(Canvas canvas) {
    canvas.drawColor(identity.bg);
    if (identity.isRetro()) {
      // HomeArtwork owns the large sunrise/orbital illustration.  The shared drawable keeps the
      // page itself quiet so that the reference frame never gets stretched behind the status bar.
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(Color.argb(24, 255, 255, 255));
      canvas.drawRect(0, 0, rect.width(), px(1), paint);
    } else if (identity.isNineties()) {
      // The 90s reference is a flat teal desktop. CRT scanlines belong to Terminal and would
      // make this identity read as a different device immediately.
      paint.setColor(Color.argb(18, 255, 255, 255));
      canvas.drawRect(0, 0, rect.width(), px(1), paint);
    } else if (identity.isMatrix()) {
      // Fine, deterministic vertical streams give the page the dense code-rain texture from the
      // reference while keeping the brightest glyphs sparse enough for the UI to remain readable.
      paint.setTypeface(android.graphics.Typeface.MONOSPACE);
      paint.setTextSize(px(4.5f));
      String[] glyphs = {"0", "0", "8", "0", "0", "1", "0", "1", "0", "8"};
      float columnStep = Math.max(px(10), rect.width() / 30f);
      int columns = Math.max(24, (int) Math.ceil(rect.width() / columnStep));
      for (int column = 0; column < columns; column++) {
        if (column % 4 == 3) continue;
        float x = px(3) + column * columnStep;
        float step = px(6 + (column % 3));
        float offset = -px((column * 17) % 74);
        int phase = (column * 5) % glyphs.length;
        for (float y = offset; y < rect.height() + px(24); y += step) {
          int row = Math.max(0, (int) Math.floor((y - offset) / step));
          int fade = (row * 19 + column * 11) % 34;
          int alpha = 5 + fade / 2;
          if ((row + column) % 13 == 0) alpha = 48;
          paint.setColor(Color.argb(alpha, 28, 190, 118));
          canvas.drawText(glyphs[(phase + row) % glyphs.length], x, y, paint);
        }
      }
    } else {
      // Terminal: fine, low-contrast CRT scanlines and a restrained phosphor top edge.
      paint.setColor(Color.argb(12, 102, 255, 136));
      paint.setStrokeWidth(px(.25f));
      for (float y = px(1); y < rect.height(); y += px(.85f))
        canvas.drawLine(0, y, rect.width(), y, paint);
      paint.setColor(Color.argb(76, 102, 255, 136));
      canvas.drawRect(0, 0, rect.width(), px(1), paint);
    }
  }

  private void drawPanel(Canvas canvas) {
    if (identity.isNineties()) {
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(identity.surface);
      canvas.drawRect(rect, paint);
      bevel(canvas, rect, true);
      return;
    }
    if (identity.isRetro()) {
      if (reference != null) {
        ReferenceArt.drawRetroPlate(
            canvas, reference, rect, false, px(16), identity.surface, paint);
        retroInsetStroke(canvas, Color.rgb(199, 164, 112), px(14));
        return;
      }
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(Color.argb(100, 80, 53, 25));
      canvas.drawRoundRect(
          new RectF(rect.left + px(2), rect.top + px(3), rect.right + px(2), rect.bottom + px(3)),
          px(16),
          px(16),
          paint);
      paint.setColor(identity.surface);
      canvas.drawRoundRect(
          new RectF(rect.left, rect.top, rect.right, rect.bottom - px(1)), px(16), px(16), paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(px(1));
      paint.setColor(Color.rgb(199, 164, 112));
      canvas.drawRoundRect(
          new RectF(rect.left + px(1), rect.top + px(1), rect.right - px(1), rect.bottom - px(2)),
          px(15),
          px(15),
          paint);
      paint.setColor(Color.argb(130, 255, 255, 255));
      canvas.drawRoundRect(
          new RectF(rect.left + px(3), rect.top + px(3), rect.right - px(3), rect.bottom - px(4)),
          px(13),
          px(13),
          paint);
      screw(canvas, rect.left + px(8), rect.top + px(8));
      screw(canvas, rect.right - px(8), rect.top + px(8));
      return;
    }
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(identity.isMatrix() ? Color.argb(168, 2, 8, 5) : identity.surface);
    if (identity.isMatrix()) canvas.drawRoundRect(rect, px(18), px(18), paint);
    else canvas.drawRect(rect, paint);
    if (identity.isTerminal()) {
      terminalFrame(canvas, identity.accent);
      return;
    }
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(px(identity.isMatrix() ? 0.8f : 1));
    paint.setColor(identity.isMatrix() ? Color.argb(110, 125, 200, 154) : identity.accent);
    canvas.drawRoundRect(
        new RectF(rect.left + px(1), rect.top + px(1), rect.right - px(1), rect.bottom - px(1)),
        px(identity.isMatrix() ? 17 : 0),
        px(identity.isMatrix() ? 17 : 0),
        paint);
    if (identity.isMatrix()) {}
  }

  private void drawPrimary(Canvas canvas) {
    if (identity.isRetro()) {
      if (reference != null) {
        ReferenceArt.drawRetroPlate(canvas, reference, rect, true, px(18), identity.accent, paint);
        paint.setAlpha(220);
        ReferenceArt.drawRegion(
            canvas,
            reference,
            new android.graphics.Rect(754, 1670, 823, 1775),
            new RectF(
                rect.right - px(34), rect.top + px(10), rect.right - px(8), rect.bottom - px(10)),
            paint);
        paint.setAlpha(255);
        retroInsetStroke(canvas, Color.rgb(255, 224, 174), px(16));
        return;
      }
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(Color.argb(100, 80, 53, 25));
      canvas.drawRoundRect(
          new RectF(rect.left + px(2), rect.top + px(3), rect.right + px(2), rect.bottom + px(3)),
          px(18),
          px(18),
          paint);
      paint.setColor(identity.accent);
      canvas.drawRoundRect(
          new RectF(rect.left, rect.top, rect.right, rect.bottom - px(1)), px(18), px(18), paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(px(1));
      paint.setColor(Color.rgb(255, 224, 174));
      canvas.drawRoundRect(
          new RectF(rect.left + px(1), rect.top + px(1), rect.right - px(1), rect.bottom - px(2)),
          px(17),
          px(17),
          paint);
      paint.setColor(Color.argb(150, 255, 255, 255));
      canvas.drawRoundRect(
          new RectF(rect.left + px(3), rect.top + px(3), rect.right - px(3), rect.bottom - px(4)),
          px(15),
          px(15),
          paint);
      screw(canvas, rect.left + px(8), rect.top + px(8));
      screw(canvas, rect.right - px(8), rect.top + px(8));
      return;
    }
    if (identity.isMatrix()) {
      RectF rim =
          new RectF(rect.left + px(5), rect.top + px(5), rect.right - px(5), rect.bottom - px(5));
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(0xA0042114);
      canvas.drawRoundRect(rim, px(11), px(11), paint);
      paint.setStyle(Paint.Style.STROKE);
      for (int pass = 12; pass >= 3; pass--) {
        paint.setStrokeWidth(px(pass));
        paint.setColor(Color.argb(7, 53, 255, 145));
        canvas.drawRoundRect(rim, px(11), px(11), paint);
      }
      paint.setStrokeWidth(px(1.7f));
      paint.setColor(0xFF71EDA2);
      canvas.drawRoundRect(rim, px(11), px(11), paint);
      return;
    }
    if (identity.isTerminal()) {
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(identity.bg);
      canvas.drawRect(rect, paint);
      terminalFrame(canvas, identity.accent);
      return;
    }
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(identity.accent);
    canvas.drawRect(rect, paint);
    if (identity.isNineties()) bevel(canvas, rect, false);
    else {
      paint.setColor(Color.argb(100, 255, 255, 255));
      canvas.drawRect(
          rect.left + px(2), rect.top + px(2), rect.right - px(2), rect.top + px(3), paint);
    }
  }

  private void drawSecondary(Canvas canvas) {
    if (identity.isRetro()) {
      if (reference != null) {
        ReferenceArt.drawRetroPlate(
            canvas, reference, rect, false, px(16), identity.surface, paint);
        retroInsetStroke(canvas, Color.rgb(199, 164, 112), px(14));
        return;
      }
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(Color.argb(95, 80, 53, 25));
      canvas.drawRoundRect(
          new RectF(rect.left + px(2), rect.top + px(3), rect.right + px(2), rect.bottom + px(3)),
          px(16),
          px(16),
          paint);
      paint.setColor(identity.surface);
      canvas.drawRoundRect(
          new RectF(rect.left, rect.top, rect.right, rect.bottom - px(1)), px(16), px(16), paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(px(1));
      paint.setColor(Color.rgb(199, 164, 112));
      canvas.drawRoundRect(
          new RectF(rect.left + px(1), rect.top + px(1), rect.right - px(1), rect.bottom - px(2)),
          px(15),
          px(15),
          paint);
      return;
    }
    if (identity.isNineties()) {
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(identity.surface);
      canvas.drawRect(rect, paint);
      bevel(canvas, rect, true);
      return;
    }
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(identity.isMatrix() ? Color.argb(148, 8, 35, 24) : identity.raised);
    if (identity.isMatrix()) canvas.drawRoundRect(rect, px(16), px(16), paint);
    else canvas.drawRect(rect, paint);
    if (identity.isTerminal()) {
      terminalFrame(canvas, identity.accent);
      return;
    }
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(px(identity.isMatrix() ? 0.8f : 1));
    paint.setColor(identity.isRetro() ? Color.rgb(199, 164, 112) : identity.accent);
    if (identity.isMatrix()) {
      paint.setColor(Color.argb(104, 49, 255, 120));
      canvas.drawRoundRect(
          new RectF(rect.left + px(1), rect.top + px(1), rect.right - px(1), rect.bottom - px(1)),
          px(15),
          px(15),
          paint);
    } else {
      canvas.drawRect(
          new RectF(rect.left + px(1), rect.top + px(1), rect.right - px(1), rect.bottom - px(1)),
          paint);
    }
  }

  /** Terminal boxes have one continuous perimeter and a small plus marker at every corner. */
  private void terminalFrame(Canvas canvas, int color) {
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(px(0.9f));
    paint.setColor(Color.argb(210, Color.red(color), Color.green(color), Color.blue(color)));
    RectF outline =
        new RectF(rect.left + px(3), rect.top + px(3), rect.right - px(3), rect.bottom - px(3));
    canvas.drawRect(outline, paint);
    float inset = px(3);
    float arm = px(2.2f);
    float[] xs = {rect.left + inset, rect.right - inset};
    float[] ys = {rect.top + inset, rect.bottom - inset};
    for (float x : xs) {
      for (float y : ys) {
        canvas.drawLine(x - arm, y, x + arm, y, paint);
        canvas.drawLine(x, y - arm, x, y + arm, paint);
      }
    }
  }

  private void bevel(Canvas canvas, RectF value, boolean panel) {
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(px(2));
    paint.setColor(panel ? Color.WHITE : Color.argb(210, 255, 255, 255));
    canvas.drawLine(
        value.left + px(1), value.bottom - px(1), value.left + px(1), value.top + px(1), paint);
    canvas.drawLine(
        value.left + px(1), value.top + px(1), value.right - px(1), value.top + px(1), paint);
    paint.setColor(Color.rgb(58, 63, 69));
    canvas.drawLine(
        value.right - px(1), value.top + px(1), value.right - px(1), value.bottom - px(1), paint);
    canvas.drawLine(
        value.left + px(1), value.bottom - px(1), value.right - px(1), value.bottom - px(1), paint);
  }

  /** Keeps the clean source corners visible while adding a crisp, identity-owned inner edge. */
  private void retroInsetStroke(Canvas canvas, int color, float radius) {
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(px(1));
    paint.setColor(color);
    canvas.drawRoundRect(
        new RectF(rect.left + px(2), rect.top + px(2), rect.right - px(2), rect.bottom - px(2)),
        radius,
        radius,
        paint);
  }

  /** Small layered hardware detail used by every retro plate. */
  private void screw(Canvas canvas, float x, float y) {
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(Color.argb(105, 46, 33, 23));
    canvas.drawCircle(x + px(0.6f), y + px(0.8f), px(2.5f), paint);
    paint.setColor(Color.rgb(247, 224, 182));
    canvas.drawCircle(x, y, px(2.0f), paint);
    paint.setColor(Color.rgb(119, 78, 48));
    canvas.drawCircle(x, y, px(1.15f), paint);
    paint.setColor(Color.argb(190, 255, 249, 221));
    canvas.drawCircle(x - px(0.45f), y - px(0.45f), px(0.35f), paint);
  }

  private float px(float value) {
    return value * density;
  }

  @Override
  public void setAlpha(int alpha) {
    paint.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(android.graphics.ColorFilter filter) {
    paint.setColorFilter(filter);
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
