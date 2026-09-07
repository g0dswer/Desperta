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
 * Full-screen portrait scanner. It owns the decode loop so a wrong code can be explained while
 * the camera remains open; the stock CaptureActivity closes after the first decode.
 */
public final class AlarmCaptureActivity extends Activity {
  public static final String EXTRA_REGISTER_MODE = "desperta_register_mode";
  public static final String EXTRA_EXPECTED_TARGET = "desperta_expected_target";
  public static final String EXTRA_EXPECTED_TARGETS = "desperta_expected_targets";

  private static final int REQUEST_CAMERA_PERMISSION = 701;
  private static final int FG = Color.rgb(255, 246, 231);
  private static final int MUTED = Color.rgb(173, 185, 201);
  private static final int SUCCESS = Color.rgb(132, 213, 176);
  private static final int ERROR = Color.rgb(255, 173, 176);

  private final Handler handler = new Handler();
  private DecoratedBarcodeView scanner;
  private BarcodeFinderView finder;
  private FrameLayout root;
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

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
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
    Window window = getWindow();
    window.setStatusBarColor(Color.BLACK);
    window.setNavigationBarColor(Color.BLACK);
    window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    if (android.os.Build.VERSION.SDK_INT >= 28) {
      window.setNavigationBarDividerColor(Color.BLACK);
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
    root.setBackgroundColor(Color.BLACK);

    scanner = new DecoratedBarcodeView(this);
    scanner.getBarcodeView().setPreviewScalingStrategy(new CenterCropStrategy());
    scanner.getViewFinder().setVisibility(View.INVISIBLE);
    scanner.getStatusView().setVisibility(View.GONE);
    root.addView(scanner, new FrameLayout.LayoutParams(-1, -1));

    finder = new BarcodeFinderView(this);
    root.addView(finder, new FrameLayout.LayoutParams(-1, -1));

    headline = label(registerMode ? "Novo código" : solvePrompt(), 21, FG);
    headline.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    headline.setGravity(Gravity.CENTER);
    headline.setContentDescription(registerMode ? "Novo código" : fullSolvePrompt());
    if (!registerMode && expectedTargets.size() > 1) {
      headline.setClickable(true);
      headline.setOnClickListener(view -> showFullCodeList());
    }
    root.addView(headline, new FrameLayout.LayoutParams(-1, dp(58)));

    instruction = label("Posicione o QR/código de barras dentro do retângulo", 15, FG);
    instruction.setGravity(Gravity.CENTER);
    root.addView(instruction, new FrameLayout.LayoutParams(-1, dp(42)));

    feedback = label("", 15, MUTED);
    feedback.setGravity(Gravity.CENTER);
    feedback.setPadding(dp(18), 0, dp(18), 0);
    root.addView(feedback, new FrameLayout.LayoutParams(-1, dp(58)));

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
    root.addView(torchButton, new FrameLayout.LayoutParams(dp(60), dp(60)));
    torchLabel = label("Lanterna", 13, FG);
    torchLabel.setGravity(Gravity.CENTER);
    root.addView(torchLabel, new FrameLayout.LayoutParams(dp(120), dp(28)));
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
    scanner.initializeFromIntent(getIntent());
    scanner.decodeContinuous(new ResultCallback());
  }

  private void positionOverlays() {
    if (root == null || frameHeight <= 0 || root.getHeight() <= 0) return;
    int frameTop = (root.getHeight() - frameHeight) / 2;
    int frameBottom = frameTop + frameHeight;
    setTopMargin(headline, Math.max(dp(14), frameTop - dp(122)));
    setTopMargin(instruction, Math.max(dp(14), frameTop - dp(58)));
    FrameLayout.LayoutParams torchParams =
        (FrameLayout.LayoutParams) torchButton.getLayoutParams();
    torchParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    torchParams.topMargin = frameBottom + dp(12);
    torchButton.setLayoutParams(torchParams);
    FrameLayout.LayoutParams torchLabelParams =
        (FrameLayout.LayoutParams) torchLabel.getLayoutParams();
    torchLabelParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    torchLabelParams.topMargin = frameBottom + dp(68);
    torchLabel.setLayoutParams(torchLabelParams);
    FrameLayout.LayoutParams feedbackParams =
        (FrameLayout.LayoutParams) feedback.getLayoutParams();
    feedbackParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    feedbackParams.topMargin = frameBottom + dp(94);
    feedback.setLayoutParams(feedbackParams);
  }

  private void setTopMargin(View view, int topMargin) {
    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
    params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
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
    new android.app.AlertDialog.Builder(this)
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
      feedback.setText("Código lido, mas ele não está selecionado. Tente outro.");
      feedback.setTextColor(ERROR);
      return;
    }
    finishing = true;
    finder.setFeedbackState(BarcodeFinderView.FeedbackState.SUCCESS);
    feedback.setText(registerMode ? "Código registrado" : "Código aceito");
    feedback.setTextColor(SUCCESS);
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
        !hasFlash ? "Lanterna indisponível neste aparelho" : torchOn ? "Desligar lanterna" : "Acender lanterna");
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
      feedback.setText("A câmera é necessária para ler este código.");
      feedback.setTextColor(ERROR);
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

  private static final class TorchIcon extends android.graphics.drawable.Drawable {
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
      paint.setColor(FG);
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
