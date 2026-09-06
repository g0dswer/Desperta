package com.desperta;

import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.Size;
import com.journeyapps.barcodescanner.camera.CenterCropStrategy;

/** Portrait camera scanner with a framing window and a hardware-backed torch control. */
public final class AlarmCaptureActivity extends CaptureActivity {
  private DecoratedBarcodeView scanner;
  private ImageButton torchButton;
  private boolean torchOn;
  private boolean hasFlash;

  @Override protected DecoratedBarcodeView initializeContent() {
    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(Color.BLACK);
    scanner = new DecoratedBarcodeView(this);
    scanner.getBarcodeView().setPreviewScalingStrategy(new CenterCropStrategy());
    scanner.getViewFinder().setVisibility(View.INVISIBLE);
    scanner.getStatusView().setVisibility(View.GONE);
    root.addView(scanner, new FrameLayout.LayoutParams(-1, -1));

    BarcodeFinderView finder = new BarcodeFinderView(this);
    root.addView(finder, new FrameLayout.LayoutParams(-1, -1));
    TextView hint = new TextView(this);
    hint.setText("Posicione o QR/código de barras dentro do retângulo");
    hint.setTextColor(Color.WHITE);
    hint.setTextSize(22);
    hint.setTypeface(null, Typeface.BOLD);
    hint.setGravity(Gravity.CENTER);
    FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(-1, dp(96));
    hintParams.setMargins(dp(28), 0, dp(28), 0);
    root.addView(hint, hintParams);

    torchButton = new ImageButton(this);
    torchButton.setBackgroundColor(Color.TRANSPARENT);
    torchButton.setFocusable(false);
    torchButton.setPadding(dp(10), dp(10), dp(10), dp(10));
    hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    torchButton.setEnabled(hasFlash);
    torchButton.setAlpha(hasFlash ? 1f : 0.4f);
    torchButton.setOnClickListener(v -> {
      if (torchOn) scanner.setTorchOff(); else scanner.setTorchOn();
    });
    FrameLayout.LayoutParams torchParams = new FrameLayout.LayoutParams(dp(60), dp(60));
    torchParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    root.addView(torchButton, torchParams);
    updateTorch();

    finder.setFrameListener((width, height) -> {
      scanner.getBarcodeView().setFramingRectSize(new Size(width, height));
      hintParams.topMargin = Math.max(dp(16), (root.getHeight() - height) / 2 - dp(110));
      hint.setLayoutParams(hintParams);
      torchParams.topMargin = (root.getHeight() + height) / 2 + dp(22);
      torchButton.setLayoutParams(torchParams);
    });
    scanner.setTorchListener(new DecoratedBarcodeView.TorchListener() {
      @Override public void onTorchOn() { torchOn = true; updateTorch(); }
      @Override public void onTorchOff() { torchOn = false; updateTorch(); }
    });
    setContentView(root);
    return scanner;
  }

  private void updateTorch() {
    torchButton.setContentDescription(!hasFlash ? "Lanterna indisponível neste aparelho" : torchOn ? "Desligar lanterna" : "Acender lanterna");
    torchButton.setImageDrawable(new TorchIcon(torchOn));
  }
  private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

  private static class TorchIcon extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean on;
    TorchIcon(boolean on) { this.on = on; }
    @Override public void draw(Canvas canvas) {
      canvas.save();
      canvas.translate(getBounds().left, getBounds().top);
      canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
      paint.setColor(Color.WHITE); paint.setStyle(Paint.Style.FILL);
      Path bolt = new Path(); bolt.moveTo(13, 1); bolt.lineTo(4, 14); bolt.lineTo(11, 14); bolt.lineTo(10, 23); bolt.lineTo(21, 9); bolt.lineTo(14, 9); bolt.close(); canvas.drawPath(bolt, paint);
      if (!on) { paint.setStrokeWidth(2.5f); paint.setStrokeCap(Paint.Cap.ROUND); canvas.drawLine(3, 3, 21, 21, paint); }
      canvas.restore();
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
  }
}
