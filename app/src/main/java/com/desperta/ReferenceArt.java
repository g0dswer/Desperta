package com.desperta;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Small, static identity-art loader. It deliberately exposes only image regions and frame strips:
 * dynamic alarm names, times, counts, and actions are always drawn by Android views on top.
 */
public final class ReferenceArt {
  private static final String DIRECTORY = "identities/";
  private static final Map<String, Bitmap> CACHE = new HashMap<>();
  private static final Map<String, Bitmap> TEXTURE_CACHE = new HashMap<>();

  private ReferenceArt() {}

  /**
   * Loads a full-screen identity reference when it is bundled; returns null for a build without it.
   */
  public static Bitmap bitmap(Context context, String identityId) {
    if (context == null || identityId == null) return null;
    synchronized (CACHE) {
      if (CACHE.containsKey(identityId)) return CACHE.get(identityId);
      Bitmap value = null;
      String path = DIRECTORY + identityId + "-reference.png";
      try (InputStream input = context.getAssets().open(path)) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        value = BitmapFactory.decodeStream(input, null, options);
      } catch (Exception ignored) {
        // Reference art is optional so an APK built before the assets are copied stays functional.
      }
      CACHE.put(identityId, value);
      return value;
    }
  }

  /**
   * Returns a drawable for a static ornament or logo region from the current identity reference.
   */
  public static Drawable assetRegion(Context context, Rect source) {
    if (context == null) return null;
    return assetRegion(context, Identity.current(context).id, source);
  }

  /** Returns a drawable for a static ornament or logo region from a named identity reference. */
  public static Drawable assetRegion(Context context, String identityId, Rect source) {
    Bitmap sourceBitmap = bitmap(context, identityId);
    if (sourceBitmap == null || source == null) return null;
    Rect safe = clamp(sourceBitmap, source);
    if (safe.isEmpty()) return null;
    Bitmap region =
        Bitmap.createBitmap(sourceBitmap, safe.left, safe.top, safe.width(), safe.height());
    return new BitmapDrawable(context.getResources(), region);
  }

  /** Source rectangle for the clean hero/card frame visible in the reference screenshots. */
  public static Rect cardSource(Bitmap bitmap) {
    if (bitmap == null) return new Rect();
    return clamp(bitmap, new Rect(16, 568, 838, 997));
  }

  /** Source rectangle for the clean orange primary-action frame in the reference screenshots. */
  public static Rect primarySource(Bitmap bitmap) {
    if (bitmap == null) return new Rect();
    return clamp(bitmap, new Rect(17, 1647, 838, 1804));
  }

  /**
   * Draws a Retro plate from clean source pixels only. The center is tiled from a blank texture
   * patch and the four corners/outer edges come from the unlettered perimeter of the reference; no
   * title, timer, plus sign, or other dynamic copy is ever copied into a live control.
   */
  public static void drawRetroPlate(
      Canvas canvas,
      Bitmap bitmap,
      RectF destination,
      boolean primary,
      float radius,
      int fallback,
      Paint paint) {
    if (canvas == null
        || bitmap == null
        || destination == null
        || paint == null
        || destination.isEmpty()) return;
    Rect source = primary ? primarySource(bitmap) : cardSource(bitmap);
    if (source.isEmpty()) return;
    Rect textureSource = primary ? new Rect(310, 1666, 326, 1682) : new Rect(62, 665, 78, 681);
    Rect texture = clamp(bitmap, textureSource);
    if (texture.isEmpty()) {
      paint.setStyle(Paint.Style.FILL);
      paint.setShader(null);
      paint.setColor(fallback);
      canvas.drawRoundRect(destination, radius, radius, paint);
      return;
    }

    float corner = Math.min(65f, Math.min(destination.width(), destination.height()) / 2f);
    int sourceCorner = Math.min(65, Math.min(source.width(), source.height()) / 2);
    float edge = Math.min(22f, Math.min(destination.width(), destination.height()) / 8f);
    int save = canvas.save();
    Path clip = new Path();
    clip.addRoundRect(destination, radius, radius, Path.Direction.CW);
    canvas.clipPath(clip);

    Bitmap textureBitmap = textureBitmap(bitmap, texture);
    paint.setStyle(Paint.Style.FILL);
    paint.setShader(
        new BitmapShader(textureBitmap, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR));
    paint.setColor(Color.WHITE);
    canvas.drawRoundRect(destination, radius, radius, paint);
    paint.setShader(null);

    // Thin edge strips avoid the text-bearing middle of the source screenshot. Corners retain
    // the four small screws and the characteristic rounded metal contour.
    drawPart(
        canvas,
        bitmap,
        new Rect(source.left, source.top, source.left + sourceCorner, source.top + sourceCorner),
        new RectF(
            destination.left, destination.top, destination.left + corner, destination.top + corner),
        paint);
    drawPart(
        canvas,
        bitmap,
        new Rect(source.right - sourceCorner, source.top, source.right, source.top + sourceCorner),
        new RectF(
            destination.right - corner,
            destination.top,
            destination.right,
            destination.top + corner),
        paint);
    drawPart(
        canvas,
        bitmap,
        new Rect(
            source.left, source.bottom - sourceCorner, source.left + sourceCorner, source.bottom),
        new RectF(
            destination.left,
            destination.bottom - corner,
            destination.left + corner,
            destination.bottom),
        paint);
    drawPart(
        canvas,
        bitmap,
        new Rect(
            source.right - sourceCorner, source.bottom - sourceCorner, source.right, source.bottom),
        new RectF(
            destination.right - corner,
            destination.bottom - corner,
            destination.right,
            destination.bottom),
        paint);

    int edgePx = Math.max(1, (int) edge);
    drawPart(
        canvas,
        bitmap,
        new Rect(
            source.left + sourceCorner,
            source.top,
            source.right - sourceCorner,
            source.top + edgePx),
        new RectF(
            destination.left + corner,
            destination.top,
            destination.right - corner,
            destination.top + edge),
        paint);
    drawPart(
        canvas,
        bitmap,
        new Rect(
            source.left + sourceCorner,
            source.bottom - edgePx,
            source.right - sourceCorner,
            source.bottom),
        new RectF(
            destination.left + corner,
            destination.bottom - edge,
            destination.right - corner,
            destination.bottom),
        paint);
    drawPart(
        canvas,
        bitmap,
        new Rect(
            source.left,
            source.top + sourceCorner,
            source.left + edgePx,
            source.bottom - sourceCorner),
        new RectF(
            destination.left,
            destination.top + corner,
            destination.left + edge,
            destination.bottom - corner),
        paint);
    drawPart(
        canvas,
        bitmap,
        new Rect(
            source.right - edgePx,
            source.top + sourceCorner,
            source.right,
            source.bottom - sourceCorner),
        new RectF(
            destination.right - edge,
            destination.top + corner,
            destination.right,
            destination.bottom - corner),
        paint);
    canvas.restoreToCount(save);
  }

  /**
   * Draws a nine-slice frame with a sampled/fixed center. Only the outer strips are copied from the
   * source, which prevents any reference screenshot text from becoming part of a live control.
   */
  public static void drawFrame(
      Canvas canvas,
      Bitmap bitmap,
      Rect source,
      RectF destination,
      int left,
      int top,
      int right,
      int bottom,
      int centerColor,
      Paint paint) {
    if (canvas == null || bitmap == null || source == null || destination == null || paint == null)
      return;
    Rect src = clamp(bitmap, source);
    if (src.isEmpty() || destination.width() <= 0 || destination.height() <= 0) return;
    int srcLeft = Math.max(1, Math.min(left, src.width() / 3));
    int srcTop = Math.max(1, Math.min(top, src.height() / 3));
    int srcRight = Math.max(1, Math.min(right, src.width() / 3));
    int srcBottom = Math.max(1, Math.min(bottom, src.height() / 3));
    float dstLeft = Math.min(px(srcLeft, destination.width()), destination.width() / 3f);
    float dstTop = Math.min(px(srcTop, destination.height()), destination.height() / 3f);
    float dstRight = Math.min(px(srcRight, destination.width()), destination.width() / 3f);
    float dstBottom = Math.min(px(srcBottom, destination.height()), destination.height() / 3f);

    paint.setStyle(Paint.Style.FILL);
    paint.setColor(centerColor);
    canvas.drawRect(destination, paint);
    RectF[] dst = {
      new RectF(
          destination.left, destination.top, destination.left + dstLeft, destination.top + dstTop),
      new RectF(
          destination.right - dstRight,
          destination.top,
          destination.right,
          destination.top + dstTop),
      new RectF(
          destination.left,
          destination.bottom - dstBottom,
          destination.left + dstLeft,
          destination.bottom),
      new RectF(
          destination.right - dstRight,
          destination.bottom - dstBottom,
          destination.right,
          destination.bottom),
      new RectF(
          destination.left + dstLeft,
          destination.top,
          destination.right - dstRight,
          destination.top + dstTop),
      new RectF(
          destination.left + dstLeft,
          destination.bottom - dstBottom,
          destination.right - dstRight,
          destination.bottom),
      new RectF(
          destination.left,
          destination.top + dstTop,
          destination.left + dstLeft,
          destination.bottom - dstBottom),
      new RectF(
          destination.right - dstRight,
          destination.top + dstTop,
          destination.right,
          destination.bottom - dstBottom)
    };
    Rect[] srcParts = {
      new Rect(src.left, src.top, src.left + srcLeft, src.top + srcTop),
      new Rect(src.right - srcRight, src.top, src.right, src.top + srcTop),
      new Rect(src.left, src.bottom - srcBottom, src.left + srcLeft, src.bottom),
      new Rect(src.right - srcRight, src.bottom - srcBottom, src.right, src.bottom),
      new Rect(src.left + srcLeft, src.top, src.right - srcRight, src.top + srcTop),
      new Rect(src.left + srcLeft, src.bottom - srcBottom, src.right - srcRight, src.bottom),
      new Rect(src.left, src.top + srcTop, src.left + srcLeft, src.bottom - srcBottom),
      new Rect(src.right - srcRight, src.top + srcTop, src.right, src.bottom - srcBottom)
    };
    paint.setFilterBitmap(true);
    for (int i = 0; i < dst.length; i++) {
      if (!dst[i].isEmpty() && !srcParts[i].isEmpty())
        canvas.drawBitmap(bitmap, srcParts[i], dst[i], paint);
    }
  }

  /**
   * Same nine-slice treatment as {@link #drawFrame}, clipped to a rounded glass boundary. Matrix
   * uses this instead of drawing a rectangular bitmap first: source texture can remain visible at
   * the edge while the center and all corners stay inside the actual card shape.
   */
  public static void drawFrameClipped(
      Canvas canvas,
      Bitmap bitmap,
      Rect source,
      RectF destination,
      float radius,
      int left,
      int top,
      int right,
      int bottom,
      int centerColor,
      Paint paint) {
    if (canvas == null || destination == null || destination.isEmpty()) return;
    Path clip = new Path();
    clip.addRoundRect(destination, radius, radius, Path.Direction.CW);
    int save = canvas.save();
    canvas.clipPath(clip);
    drawFrame(canvas, bitmap, source, destination, left, top, right, bottom, centerColor, paint);
    canvas.restoreToCount(save);
  }

  /** Draws one decorative source region, used for the unlettered grille on the retro CTA frame. */
  public static void drawRegion(
      Canvas canvas, Bitmap bitmap, Rect source, RectF destination, Paint paint) {
    if (canvas == null || bitmap == null || source == null || destination == null || paint == null)
      return;
    Rect safe = clamp(bitmap, source);
    if (safe.isEmpty() || destination.isEmpty()) return;
    paint.setFilterBitmap(true);
    canvas.drawBitmap(bitmap, safe, destination, paint);
  }

  private static Bitmap textureBitmap(Bitmap source, Rect region) {
    String key =
        System.identityHashCode(source)
            + ":"
            + region.left
            + ":"
            + region.top
            + ":"
            + region.right
            + ":"
            + region.bottom;
    synchronized (TEXTURE_CACHE) {
      Bitmap cached = TEXTURE_CACHE.get(key);
      if (cached != null && !cached.isRecycled()) return cached;
      Bitmap created =
          Bitmap.createBitmap(source, region.left, region.top, region.width(), region.height());
      TEXTURE_CACHE.put(key, created);
      return created;
    }
  }

  private static void drawPart(
      Canvas canvas, Bitmap bitmap, Rect source, RectF destination, Paint paint) {
    Rect safe = clamp(bitmap, source);
    if (safe.isEmpty() || destination.isEmpty()) return;
    paint.setShader(null);
    paint.setFilterBitmap(true);
    canvas.drawBitmap(bitmap, safe, destination, paint);
  }

  /** Samples a reference pixel safely, falling back when the optional art is unavailable. */
  public static int sample(Bitmap bitmap, int x, int y, int fallback) {
    if (bitmap == null || x < 0 || y < 0 || x >= bitmap.getWidth() || y >= bitmap.getHeight())
      return fallback;
    return bitmap.getPixel(x, y);
  }

  private static Rect clamp(Bitmap bitmap, Rect source) {
    return new Rect(
        Math.max(0, Math.min(source.left, bitmap.getWidth())),
        Math.max(0, Math.min(source.top, bitmap.getHeight())),
        Math.max(0, Math.min(source.right, bitmap.getWidth())),
        Math.max(0, Math.min(source.bottom, bitmap.getHeight())));
  }

  // Insets are in source pixels; preserve proportions when a destination is smaller than the art.
  private static float px(int sourceInset, float destinationSize) {
    return Math.min(sourceInset, Math.max(1f, destinationSize / 3f));
  }
}
