package com.desperta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.RectF;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies the real portrait scanner surface without allowing the QR fixture to auto-complete. */
@RunWith(AndroidJUnit4.class)
public class ScannerAppearanceTest {
  private Context context;
  private UiDevice ui;
  private ActivityScenario<AlarmCaptureActivity> scenario;

  @Before
  public void setUp() throws Exception {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    try (ParcelFileDescriptor command =
        InstrumentationRegistry.getInstrumentation()
            .getUiAutomation()
            .executeShellCommand(
                "pm grant " + context.getPackageName() + " android.permission.CAMERA")) {
      new FileInputStream(command.getFileDescriptor()).readAllBytes();
    }
  }

  @After
  public void tearDown() {
    if (scenario != null) scenario.close();
  }

  @Test
  public void scannerUsesPortraitFullCameraWithLandscapeFramePortugueseHintAndTorchControl()
      throws Exception {
    Intent launch =
        new Intent(context, AlarmCaptureActivity.class)
            .setAction("com.google.zxing.client.android.SCAN")
            // The configured camera fixture is a QR code.  Filtering it out keeps this appearance
            // test on the scanner surface instead of finishing the activity during launch.
            .putExtra("SCAN_FORMATS", "DATA_MATRIX");
    scenario = ActivityScenario.launchActivityForResult(launch);

    assertTrue(
        ui.wait(
            Until.hasObject(By.text("Posicione o QR/código de barras dentro do retângulo")),
            10_000));
    assertTrue(ui.wait(Until.hasObject(By.descContains("lanterna")), 10_000));

    final View[] finderHolder = new View[1];
    final DecoratedBarcodeView[] scannerHolder = new DecoratedBarcodeView[1];
    final ImageButton[] torchHolder = new ImageButton[1];
    final TextView[] hintHolder = new TextView[1];
    final int[] orientation = new int[1];
    final int[] requestedOrientation = new int[1];
    scenario.onActivity(
        activity -> {
          orientation[0] =
              activity.getResources().getConfiguration().orientation;
          requestedOrientation[0] = activity.getRequestedOrientation();
          View root = activity.getWindow().getDecorView();
          finderHolder[0] = findView(root, BarcodeFinderView.class);
          scannerHolder[0] = findView(root, DecoratedBarcodeView.class);
          torchHolder[0] = findView(root, ImageButton.class);
          hintHolder[0] = findViewWithText(root, "Posicione o QR/código de barras dentro do retângulo");
        });

    assertEquals(Configuration.ORIENTATION_PORTRAIT, orientation[0]);
    assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, requestedOrientation[0]);
    assertNotNull("The framing overlay must be part of the live view hierarchy", finderHolder[0]);
    assertNotNull("The live camera preview must be part of the view hierarchy", scannerHolder[0]);
    assertNotNull("The torch control must be part of the live view hierarchy", torchHolder[0]);
    assertNotNull("The Portuguese scanner hint must be part of the live view hierarchy", hintHolder[0]);
    assertTrue(finderHolder[0].getWidth() > 0);
    assertTrue(finderHolder[0].getHeight() > 0);
    assertEquals(finderHolder[0].getWidth(), scannerHolder[0].getWidth());
    assertEquals(finderHolder[0].getHeight(), scannerHolder[0].getHeight());

    Field frameField = BarcodeFinderView.class.getDeclaredField("frame");
    frameField.setAccessible(true);
    RectF frame = (RectF) frameField.get(finderHolder[0]);
    assertTrue("Portrait surface should contain a wider scan window", frame.width() > frame.height());
    assertEquals(0.63f, frame.height() / frame.width(), 0.05f);
    assertEquals(finderHolder[0].getWidth() / 2f, frame.centerX(), 2f);
    assertEquals(finderHolder[0].getHeight() * 0.5f, frame.centerY(), 4f);
    assertTrue(frame.left > 0 && frame.right < finderHolder[0].getWidth());
    assertTrue(frame.top > 0 && frame.bottom < finderHolder[0].getHeight());
    assertTrue("The hint must be above the scan rectangle", hintHolder[0].getBottom() <= frame.top);
    assertTrue("The torch must be below the scan rectangle", torchHolder[0].getTop() >= frame.bottom);

    ui.waitForIdle();
    ui.takeScreenshot(new java.io.File(context.getExternalFilesDir(null), "scanner.png"));

    UiObject torch = ui.findObject(new UiSelector().className("android.widget.ImageButton"));
    assertTrue(torch.waitForExists(2000));
    if (context.getPackageManager().hasSystemFeature("android.hardware.camera.flash")) {
      assertTrue(ui.findObject(new UiSelector().description("Acender lanterna")).exists());
      torch.click();
      ui.waitForIdle();
      assertTrue(ui.wait(Until.hasObject(By.desc("Desligar lanterna")), 2000));
      ui.findObject(new UiSelector().description("Desligar lanterna")).click();
      ui.waitForIdle();
      assertTrue(ui.wait(Until.hasObject(By.desc("Acender lanterna")), 2000));
    } else {
      assertTrue(ui.findObject(new UiSelector().description("Lanterna indisponível neste aparelho")).exists());
      assertTrue("Unavailable torch must be disabled", !torch.isEnabled());
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends View> T findView(View root, Class<T> type) {
    if (type.isInstance(root)) return (T) root;
    if (!(root instanceof ViewGroup)) return null;
    ViewGroup group = (ViewGroup) root;
    for (int i = 0; i < group.getChildCount(); i++) {
      T match = findView(group.getChildAt(i), type);
      if (match != null) return match;
    }
    return null;
  }

  private TextView findViewWithText(View root, String value) {
    if (root instanceof TextView && value.contentEquals(((TextView) root).getText())) {
      return (TextView) root;
    }
    if (!(root instanceof ViewGroup)) return null;
    ViewGroup group = (ViewGroup) root;
    for (int i = 0; i < group.getChildCount(); i++) {
      TextView match = findViewWithText(group.getChildAt(i), value);
      if (match != null) return match;
    }
    return null;
  }
}
