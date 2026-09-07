package com.desperta;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

/** Original, approved ornamental artwork; alarm values and every control remain native/live. */
final class HomeArtwork extends Drawable {
  private static final Map<String, Bitmap> SOURCES = new HashMap<>();
  private final Bitmap image;
  private final Rect source;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

  HomeArtwork(Context context, String id, Rect source) {
    synchronized (SOURCES) {
      Bitmap bitmap = SOURCES.get(id);
      if (bitmap == null) {
        try (java.io.InputStream stream =
            context.getAssets().open("identities/" + id + "-reference.png")) {
          bitmap = BitmapFactory.decodeStream(stream);
          SOURCES.put(id, bitmap);
        } catch (java.io.IOException error) {
          throw new IllegalStateException("Missing identity artwork", error);
        }
      }
      image = bitmap;
    }
    this.source = source;
  }

  @Override
  public void draw(Canvas canvas) {
    int saved = canvas.save();
    if (source.left == 101 || source.left == 86) {
      Path clip = new Path();
      clip.addOval(new RectF(getBounds()), Path.Direction.CW);
      canvas.clipPath(clip);
    }
    canvas.drawBitmap(image, source, getBounds(), paint);
    canvas.restoreToCount(saved);
  }

  @Override
  public void setAlpha(int alpha) {
    paint.setAlpha(alpha);
    invalidateSelf();
  }

  @Override
  public void setColorFilter(ColorFilter filter) {
    paint.setColorFilter(filter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  static final class Emblem extends View {
    private final Drawable art;

    Emblem(Context context, Identity identity, boolean weekend) {
      super(context);
      art =
          new HomeArtwork(
              context,
              "retro",
              weekend ? new Rect(70, 1290, 214, 1468) : new Rect(70, 1050, 214, 1218));
      setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      int saved = canvas.save();
      Path clip = new Path();
      clip.addOval(new RectF(0, 0, getWidth(), getHeight() * .82f), Path.Direction.CW);
      clip.addRect(
          getWidth() * .3f, getHeight() * .82f, getWidth() * .7f, getHeight(), Path.Direction.CW);
      canvas.clipPath(clip);
      art.setBounds(0, 0, getWidth(), getHeight());
      art.draw(canvas);
      canvas.restoreToCount(saved);
    }
  }

  static final class Gear extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override
    public void draw(Canvas canvas) {
      Rect b = getBounds();
      float cx = b.exactCenterX(),
          cy = b.exactCenterY(),
          r = Math.min(b.width(), b.height()) * .46f;
      Path path = new Path();
      for (int i = 0; i < 32; i++) {
        double angle = (i * 360.0 / 32 - 90) * Math.PI / 180;
        float radius = (i % 4 == 0 || i % 4 == 3) ? r * .78f : r;
        float x = cx + (float) Math.cos(angle) * radius, y = cy + (float) Math.sin(angle) * radius;
        if (i == 0) path.moveTo(x, y);
        else path.lineTo(x, y);
      }
      path.close();
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(Math.max(1f, r * .09f));
      paint.setColor(0xFF8EDFAA);
      canvas.drawPath(path, paint);
      canvas.drawCircle(cx, cy, r * .38f, paint);
    }

    @Override
    public void setAlpha(int a) {
      paint.setAlpha(a);
    }

    @Override
    public void setColorFilter(ColorFilter f) {
      paint.setColorFilter(f);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }

  static final class RetroRail extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final int color;

    RetroRail(Context context, int color) {
      density = context.getResources().getDisplayMetrics().density;
      this.color = color;
    }

    @Override
    public void draw(Canvas canvas) {
      Rect b = getBounds();
      paint.setColor(color);
      canvas.drawRoundRect(
          new RectF(
              b.left + 2 * density,
              b.top + 7 * density,
              b.left + 7 * density,
              b.bottom - 7 * density),
          3 * density,
          3 * density,
          paint);
      paint.setColor(0xFFBEA77D);
      paint.setStrokeWidth(density * .7f);
      canvas.drawLine(
          b.right - 51 * density,
          b.top + 4 * density,
          b.right - 51 * density,
          b.bottom - 4 * density,
          paint);
    }

    @Override
    public void setAlpha(int a) {
      paint.setAlpha(a);
    }

    @Override
    public void setColorFilter(ColorFilter f) {
      paint.setColorFilter(f);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }

  /** Sizes live numeric glyphs by their visible bounds rather than font ascent/leading. */
  // The app uses framework Activities; these views retain framework text/accessibility semantics.
  @android.annotation.SuppressLint("AppCompatCustomView")
  static final class SpaceTime extends TextView {
    private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
    boolean accentColon;

    SpaceTime(Context context) {
      super(context);
      setIncludeFontPadding(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      String value = getText().toString();
      if (value.isEmpty()) return;
      glyph.setShader(null);
      glyph.setTypeface(getTypeface());
      glyph.setTextSize(100);
      glyph.setColor(getCurrentTextColor());
      Rect bounds = new Rect();
      glyph.getTextBounds(value, 0, value.length(), bounds);
      float width = glyph.measureText(value);
      if (width <= 0 || bounds.height() <= 0) return;
      float scale =
          Math.min(
              getWidth()
                  * (Identity.current(getContext()).isTerminal()
                      ? (accentColon ? .73f : .71f)
                      : (accentColon ? .94f : .92f))
                  / width,
              getHeight() * .79f / bounds.height());
      int saved = canvas.save();
      canvas.translate(
          getScrollX()
              + ((getGravity() & android.view.Gravity.HORIZONTAL_GRAVITY_MASK)
                      == android.view.Gravity.CENTER_HORIZONTAL
                  ? (getWidth() - width * scale) / 2
                  : 0),
          getScrollY() + (getHeight() - bounds.height() * scale) / 2 - bounds.top * scale);
      canvas.scale(scale, scale);
      float x = 0;
      Identity identity = Identity.current(getContext());
      if (identity.isTerminal())
        glyph.setShader(
            new LinearGradient(
                0,
                0,
                0,
                3f / scale,
                new int[] {getCurrentTextColor(), getCurrentTextColor(), 0xFF152819},
                new float[] {0, .60f, 1},
                Shader.TileMode.REPEAT));
      for (char ch : value.toCharArray()) {
        String one = String.valueOf(ch);
        glyph.setColor(
            ch == ':' && accentColon && identity.isRetro()
                ? identity.accent
                : getCurrentTextColor());
        canvas.drawText(one, x, 0, glyph);
        x += glyph.measureText(one);
      }
      canvas.restoreToCount(saved);
    }
  }

  /** Live seven-segment display matching the inset clock in the 1990s reference. */
  @android.annotation.SuppressLint("AppCompatCustomView")
  static final class DigitalTime extends TextView {
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    boolean matrix;

    void setMatrix(boolean value) {
      matrix = value;
      invalidate();
    }

    private final int[] segments = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F};

    DigitalTime(Context context) {
      super(context);
      setWillNotDraw(false);
      // TextView still owns the accessible value, but its native glyphs must not compete with the
      // seven-segment face. Remove native font padding/scroll affordances so the custom canvas has
      // a stable drawing area at every identity font scale.
      setTextColor(Color.TRANSPARENT);
      setIncludeFontPadding(false);
      setPadding(0, 0, 0, 0);
      setVerticalScrollBarEnabled(false);
      setHorizontalScrollBarEnabled(false);
      ink.setStyle(Paint.Style.FILL);
      ink.setDither(true);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int count) {
      super.onTextChanged(text, start, before, count);
      postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas c) {
      String value = getText() == null ? "--:--" : getText().toString().trim();
      if (value.length() != 5 || value.charAt(2) != ':') value = "--:--";

      // View.draw() translates the canvas by -scrollY for a TextView. Auto-size and large-font
      // layout can leave that scroll offset non-zero even though this view draws its own glyphs;
      // cancel it before positioning the clock, otherwise the entire face is clipped above the
      // navy inset (the reported blank-clock failure).
      c.save();
      c.translate(getScrollX(), getScrollY());
      float units = matrix ? 302f : 280f;
      float scale =
          Math.min((getWidth() - 24f) / units, (getHeight() - 24f) / 100f)
              * (matrix
                  ? (getHeight() / getResources().getDisplayMetrics().density > 80 ? .82f : .98f)
                  : .74f);
      if (scale <= 0f) {
        c.restore();
        return;
      }
      c.translate(
          (getWidth() - units * scale) / 2f, (getHeight() - (matrix ? 77f : 100f) * scale) / 2f);
      c.scale(scale, matrix ? scale * .77f : scale);
      ink.setColor(matrix ? 0xFF94F2B5 : 0xFF00A7AA);
      if (matrix) ink.setShadowLayer(5f, 0f, 0f, 0x8846FF8F);
      float x = 0f;
      for (char character : value.toCharArray()) {
        if (character == ':') {
          c.drawRect(x + 3f, 25f, x + 11f, 34f, ink);
          c.drawRect(x + 3f, 66f, x + 11f, 75f, ink);
          x += 22f;
          continue;
        }
        c.save();
        c.translate(x, 0f);
        if (matrix && character >= '0' && character <= '9') {
          c.scale(.8f, 1f);
          matrixDigit(c, character);
        } else if (character >= '0' && character <= '9') {
          int mask = segments[character - '0'];
          if ((mask & 1) != 0) horizontal(c, 6f, 0f);
          if ((mask & 2) != 0) vertical(c, 52f, 5f);
          if ((mask & 4) != 0) vertical(c, 52f, 53f);
          if ((mask & 8) != 0) horizontal(c, 6f, 90f);
          if ((mask & 16) != 0) vertical(c, 0f, 53f);
          if ((mask & 32) != 0) vertical(c, 0f, 5f);
          if ((mask & 64) != 0) horizontal(c, 6f, 45f);
          if (matrix && character == '0') {
            Path slash = new Path();
            slash.moveTo(8, 82);
            slash.lineTo(14, 86);
            slash.lineTo(54, 16);
            slash.lineTo(48, 12);
            slash.close();
            c.drawPath(slash, ink);
          }
        } else if (character == '-') {
          horizontal(c, 6f, 45f);
        }
        c.restore();
        x += matrix ? 70f : 64f;
      }
      c.restore();
    }

    private void matrixDigit(Canvas canvas, char digit) {
      ink.setStyle(Paint.Style.STROKE);
      ink.setStrokeWidth(9f);
      ink.setStrokeJoin(Paint.Join.BEVEL);
      ink.setStrokeCap(Paint.Cap.SQUARE);
      Path glyph = new Path();
      if (digit == '0') {
        glyph.moveTo(10, 5);
        glyph.lineTo(51, 5);
        glyph.lineTo(57, 12);
        glyph.lineTo(57, 88);
        glyph.lineTo(51, 95);
        glyph.lineTo(10, 95);
        glyph.lineTo(4, 88);
        glyph.lineTo(4, 12);
        glyph.close();
        canvas.drawPath(glyph, ink);
        ink.setStrokeWidth(5f);
        canvas.drawLine(6, 90, 55, 10, ink);
      } else if (digit == '3') {
        glyph.moveTo(6, 5);
        glyph.lineTo(49, 5);
        glyph.lineTo(57, 14);
        glyph.lineTo(57, 39);
        glyph.lineTo(47, 50);
        glyph.lineTo(57, 61);
        glyph.lineTo(57, 86);
        glyph.lineTo(49, 95);
        glyph.lineTo(6, 95);
        canvas.drawPath(glyph, ink);
        canvas.drawLine(28, 50, 47, 50, ink);
      } else if (digit == '7') {
        glyph.moveTo(3, 5);
        glyph.lineTo(56, 5);
        glyph.lineTo(8, 95);
        canvas.drawPath(glyph, ink);
      } else {
        int mask = segments[digit - '0'];
        if ((mask & 1) != 0) canvas.drawLine(9, 5, 51, 5, ink);
        if ((mask & 2) != 0) canvas.drawLine(56, 10, 56, 45, ink);
        if ((mask & 4) != 0) canvas.drawLine(56, 55, 56, 90, ink);
        if ((mask & 8) != 0) canvas.drawLine(9, 95, 51, 95, ink);
        if ((mask & 16) != 0) canvas.drawLine(4, 55, 4, 90, ink);
        if ((mask & 32) != 0) canvas.drawLine(4, 10, 4, 45, ink);
        if ((mask & 64) != 0) canvas.drawLine(9, 50, 51, 50, ink);
      }
      ink.setStyle(Paint.Style.FILL);
    }

    private void horizontal(Canvas c, float x, float y) {
      Path p = new Path();
      p.moveTo(x + 5, y);
      p.lineTo(x + 40, y);
      p.lineTo(x + 45, y + 5);
      p.lineTo(x + 40, y + 10);
      p.lineTo(x + 5, y + 10);
      p.lineTo(x, y + 5);
      p.close();
      c.drawPath(p, ink);
    }

    private void vertical(Canvas c, float x, float y) {
      Path p = new Path();
      p.moveTo(x + 5, y);
      p.lineTo(x + 10, y + 5);
      p.lineTo(x + 10, y + 35);
      p.lineTo(x + 5, y + 40);
      p.lineTo(x, y + 35);
      p.lineTo(x, y + 5);
      p.close();
      c.drawPath(p, ink);
    }
  }
}
