package com.desperta;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.Size;
import com.journeyapps.barcodescanner.camera.CenterCropStrategy;
import java.util.ArrayList;

/**
 * Full-screen portrait scanner. It owns the decode loop so a wrong code can be explained while the
 * camera remains open; the stock CaptureActivity closes after the first decode.
 */
public final class AlarmCaptureActivity extends Activity {
  public static final String EXTRA_REGISTER_MODE = "desperta_register_mode";
  public static final String EXTRA_EXPECTED_TARGET = "desperta_expected_target";
  public static final String EXTRA_EXPECTED_TARGETS = "desperta_expected_targets";

  private static final int REQUEST_CAMERA_PERMISSION = 701;
  private final Handler handler = new Handler();
  private DecoratedBarcodeView scanner;
  private BarcodeFinderView finder;
  private FrameLayout root;
  private FrameLayout overlays;
  private TextView headline;
  private TextView instruction;
  private TextView feedback;
  private TextView torchLabel;
  private ImageButton torchButton;
  private boolean torchOn;
  private boolean hasFlash;
  private boolean registerMode;
  private String expectedTarget;
  private final ArrayList<String> expectedTargets = new ArrayList<>();
  private boolean finishing;
  private int frameHeight;
  private Identity theme;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    theme = Identity.current(this);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    configureWindow();
    readMissionIntent();
    buildContent();
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this, new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }
  }

  private void configureWindow() {
    if (theme == null) theme = Identity.current(this);
    theme.applyWindow(this);
    Window window = getWindow();
    window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    if (android.os.Build.VERSION.SDK_INT >= 28) {
      window.setNavigationBarDividerColor(theme.bg);
    }
  }

  private void readMissionIntent() {
    Intent launch = getIntent();
    registerMode = launch.getBooleanExtra(EXTRA_REGISTER_MODE, false);
    expectedTarget = launch.getStringExtra(EXTRA_EXPECTED_TARGET);
    ArrayList<String> incoming = launch.getStringArrayListExtra(EXTRA_EXPECTED_TARGETS);
    if (incoming != null) {
      for (String value : incoming) {
        if (value != null && !value.isEmpty() && !expectedTargets.contains(value)) {
          expectedTargets.add(value);
        }
      }
    }
    String[] incomingArray = launch.getStringArrayExtra(EXTRA_EXPECTED_TARGETS);
    if (incomingArray != null) {
      for (String value : incomingArray) {
        if (value != null && !value.isEmpty() && !expectedTargets.contains(value)) {
          expectedTargets.add(value);
        }
      }
    }
    if (expectedTargets.isEmpty() && expectedTarget != null && !expectedTarget.isEmpty()) {
      expectedTargets.add(expectedTarget);
    }
  }

  private void buildContent() {
    root = new FrameLayout(this);
    // Keep the live camera surface untouched. Identity styling is applied only to controls layered
    // above it, never to the camera preview itself.
    root.setBackgroundColor(Color.TRANSPARENT);

    scanner = new DecoratedBarcodeView(this);
    scanner.getBarcodeView().setPreviewScalingStrategy(new CenterCropStrategy());
    scanner.getViewFinder().setVisibility(View.INVISIBLE);
    scanner.getStatusView().setVisibility(View.GONE);
    root.addView(scanner, new FrameLayout.LayoutParams(-1, -1));

    finder = new BarcodeFinderView(this);
    root.addView(finder, new FrameLayout.LayoutParams(-1, -1));

    overlays = new FrameLayout(this);
    root.addView(overlays, new FrameLayout.LayoutParams(-1, -1));

    headline = label(registerMode ? "Novo código" : solvePrompt(), 21, theme.fg);
    headline.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    theme.styleText(headline, true);
    headline.setGravity(Gravity.CENTER);
    headline.setContentDescription(registerMode ? "Novo código" : fullSolvePrompt());
    if (!registerMode && expectedTargets.size() > 1) {
      headline.setClickable(true);
      headline.setOnClickListener(view -> showFullCodeList());
    }
    // Keep scanner copy and instructions readable over any camera image without tinting the
    // preview itself. Compact identity panels are confined to the labels outside the finder.
    headline.setBackground(theme.panel(this));
    headline.setPadding(dp(18), 0, dp(18), 0);
    headline.setMinWidth(dp(220));
    headline.setMaxWidth(dp(360));
    overlays.addView(headline, new FrameLayout.LayoutParams(-2, dp(58)));

    instruction = label("Posicione o QR/código de barras dentro do retângulo", 15, theme.fg);
    instruction.setGravity(Gravity.CENTER);
    instruction.setBackground(theme.panel(this));
    instruction.setPadding(dp(16), 0, dp(16), 0);
    instruction.setMinWidth(dp(240));
    instruction.setMaxWidth(dp(360));
    overlays.addView(instruction, new FrameLayout.LayoutParams(-2, dp(42)));

    feedback = label("", 15, theme.muted);
    feedback.setVisibility(View.INVISIBLE);
    feedback.setGravity(Gravity.CENTER);
    feedback.setPadding(dp(18), 0, dp(18), 0);
    feedback.setBackground(theme.panel(this));
    feedback.setMinWidth(dp(220));
    feedback.setMaxWidth(dp(360));
    overlays.addView(feedback, new FrameLayout.LayoutParams(-2, dp(58)));

    torchButton = new ImageButton(this);
    torchButton.setBackgroundColor(Color.TRANSPARENT);
    torchButton.setFocusable(false);
    torchButton.setPadding(dp(10), dp(10), dp(10), dp(10));
    hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    torchButton.setEnabled(hasFlash);
    torchButton.setAlpha(hasFlash ? 1f : 0.4f);
    torchButton.setOnClickListener(
        view -> {
          if (torchOn) scanner.setTorchOff();
          else scanner.setTorchOn();
        });
    overlays.addView(torchButton, new FrameLayout.LayoutParams(dp(60), dp(60)));
    torchLabel = label("Lanterna", 13, theme.fg);
    torchLabel.setGravity(Gravity.CENTER);
    torchLabel.setBackground(theme.panel(this));
    torchLabel.setPadding(dp(12), 0, dp(12), 0);
    torchLabel.setMinWidth(dp(90));
    overlays.addView(torchLabel, new FrameLayout.LayoutParams(-2, dp(28)));
    updateTorch();

    finder.setFrameListener(
        (width, height) -> {
          frameHeight = height;
          scanner.getBarcodeView().setFramingRectSize(new Size(width, height));
          positionOverlays();
        });
    root.addOnLayoutChangeListener(
        (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
            positionOverlays());
    scanner.setTorchListener(
        new DecoratedBarcodeView.TorchListener() {
          @Override
          public void onTorchOn() {
            torchOn = true;
            updateTorch();
          }

          @Override
          public void onTorchOff() {
            torchOn = false;
            updateTorch();
          }
        });
    setContentView(root);
    theme.applyTree(overlays);
    scanner.initializeFromIntent(getIntent());
    scanner.decodeContinuous(new ResultCallback());
  }

  private void positionOverlays() {
    if (root == null || frameHeight <= 0 || root.getHeight() <= 0) return;
    int frameTop = (root.getHeight() - frameHeight) / 2;
    int frameBottom = frameTop + frameHeight;
    setTopMargin(headline, Math.max(dp(14), frameTop - dp(122)));
    setTopMargin(instruction, Math.max(dp(14), frameTop - dp(58)));
    setTopMargin(torchButton, frameBottom + dp(12));
    setTopMargin(torchLabel, frameBottom + dp(68));
    setTopMargin(feedback, frameBottom + dp(94));
  }

  private void setTopMargin(View view, int topMargin) {
    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
    int gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    if (params.gravity == gravity && params.topMargin == topMargin) return;
    params.gravity = gravity;
    params.topMargin = topMargin;
    view.setLayoutParams(params);
  }

  private String solvePrompt() {
    if (expectedTargets.isEmpty()) return "Leitor de código";
    if (expectedTargets.size() == 1) {
      return "Escaneie: " + shortenCode(expectedTargets.get(0));
    }
    if (expectedTargets.size() > 2) {
      return "Escaneie um dos " + expectedTargets.size() + " códigos";
    }
    StringBuilder value = new StringBuilder("Escaneie um destes: ");
    for (int i = 0; i < expectedTargets.size(); i++) {
      if (i > 0) value.append(" · ");
      value.append(shortenCode(expectedTargets.get(i)));
    }
    return value.toString();
  }

  private String fullSolvePrompt() {
    if (expectedTargets.isEmpty()) return "Leitor de código";
    StringBuilder value = new StringBuilder("Códigos selecionados: ");
    for (int i = 0; i < expectedTargets.size(); i++) {
      if (i > 0) value.append(", ");
      value.append(expectedTargets.get(i));
    }
    return value.toString();
  }

  private void showFullCodeList() {
    if (expectedTargets.isEmpty()) return;
    StringBuilder value = new StringBuilder();
    for (String code : expectedTargets) {
      if (value.length() > 0) value.append('\n');
      value.append(code);
    }
    theme
        .dialog(this)
        .setTitle("Códigos selecionados")
        .setMessage(value.toString())
        .setPositiveButton("Fechar", null)
        .show();
  }

  private String shortenCode(String value) {
    if (value == null) return "código";
    return value.length() <= 18 ? value : value.substring(0, 15) + "…";
  }

  private boolean matchesExpected(String contents) {
    if (registerMode || expectedTargets.isEmpty()) return true;
    for (String accepted : expectedTargets) {
      if (MissionLogic.barcodeMatches(accepted, contents)) return true;
    }
    return expectedTarget != null && MissionLogic.barcodeMatches(expectedTarget, contents);
  }

  private void handleDecoded(BarcodeResult result) {
    if (finishing || result == null || result.getText() == null || result.getText().isEmpty()) {
      return;
    }
    String contents = result.getText();
    if (!matchesExpected(contents)) {
      finder.setFeedbackState(BarcodeFinderView.FeedbackState.ERROR);
      feedback.setVisibility(View.VISIBLE);
      feedback.setText("Código lido, mas ele não está selecionado. Tente outro.");
      feedback.setTextColor(theme.error);
      return;
    }
    finishing = true;
    finder.setFeedbackState(BarcodeFinderView.FeedbackState.SUCCESS);
    feedback.setVisibility(View.VISIBLE);
    feedback.setText(registerMode ? "Código registrado" : "Código aceito");
    feedback.setTextColor(theme.positive);
    root.performHapticFeedback(
        android.os.Build.VERSION.SDK_INT >= 30
            ? HapticFeedbackConstants.CONFIRM
            : HapticFeedbackConstants.VIRTUAL_KEY);
    scanner.pause();
    Intent answer = new Intent();
    answer.putExtra("SCAN_RESULT", contents);
    answer.putExtra(
        "SCAN_RESULT_FORMAT",
        result.getBarcodeFormat() == null ? "" : result.getBarcodeFormat().toString());
    answer.putExtra("SCAN_RESULT_BYTES", result.getRawBytes());
    setResult(RESULT_OK, answer);
    finish();
  }

  private final class ResultCallback implements BarcodeCallback {
    @Override
    public void barcodeResult(BarcodeResult result) {
      runOnUiThread(() -> handleDecoded(result));
    }
  }

  private void updateTorch() {
    torchButton.setContentDescription(
        !hasFlash
            ? "Lanterna indisponível neste aparelho"
            : torchOn ? "Desligar lanterna" : "Acender lanterna");
    torchButton.setImageDrawable(new TorchIcon(torchOn));
  }

  private TextView label(String text, float size, int color) {
    TextView view = new TextView(this);
    view.setText(text);
    view.setTextSize(size);
    view.setTextColor(color);
    return view;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (scanner != null
        && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        && !finishing) {
      scanner.resume();
    }
  }

  @Override
  protected void onPause() {
    if (scanner != null) scanner.pause();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (scanner != null) {
      try {
        scanner.pauseAndWait();
      } catch (RuntimeException ignored) {
        // The camera may not have been opened yet when permission was denied.
      }
    }
    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode != REQUEST_CAMERA_PERMISSION) return;
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      if (scanner != null) scanner.resume();
    } else {
      feedback.setVisibility(View.VISIBLE);
      feedback.setText("A câmera é necessária para ler este código.");
      feedback.setTextColor(theme.error);
    }
  }

  @Override
  public void onBackPressed() {
    if (scanner != null) scanner.pause();
    // Keep a non-null result intent so IntentIntegrator can parse a cancelled scan consistently
    // across Android versions.
    setResult(RESULT_CANCELED, new Intent());
    finish();
  }

  private final class TorchIcon extends android.graphics.drawable.Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean on;

    TorchIcon(boolean on) {
      this.on = on;
    }

    @Override
    public void draw(Canvas canvas) {
      canvas.save();
      canvas.translate(getBounds().left, getBounds().top);
      canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
      paint.setColor(theme.fg);
      paint.setStyle(Paint.Style.FILL);
      Path bolt = new Path();
      bolt.moveTo(13, 1);
      bolt.lineTo(4, 14);
      bolt.lineTo(11, 14);
      bolt.lineTo(10, 23);
      bolt.lineTo(21, 9);
      bolt.lineTo(14, 9);
      bolt.close();
      canvas.drawPath(bolt, paint);
      if (!on) {
        paint.setStrokeWidth(2.5f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(3, 3, 21, 21, paint);
      }
      canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
      paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
      paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }
}
