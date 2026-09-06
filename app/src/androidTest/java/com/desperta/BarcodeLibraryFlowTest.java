package com.desperta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Device-side coverage for the QR/barcode library screen and its alarm-editor boundary. */
@RunWith(AndroidJUnit4.class)
public class BarcodeLibraryFlowTest {
  private static final String FIXTURE = "DESPERTA-ACORDAR-2026";
  private Context context;
  private UiDevice ui;

  @Before
  public void setUp() throws Exception {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    try (ParcelFileDescriptor command =
        InstrumentationRegistry.getInstrumentation()
            .getUiAutomation()
            .executeShellCommand(
                "pm grant " + context.getPackageName() + " android.permission.CAMERA")) {
      new java.io.FileInputStream(command.getFileDescriptor()).readAllBytes();
    }
    for (Alarm alarm : Store.all(context)) Scheduler.cancel(context, alarm.id);
    context.stopService(new Intent(context, AlarmService.class));
    Store.prefs(context).edit().clear().commit();
  }

  @After
  public void tearDown() {
    context.stopService(new Intent(context, AlarmService.class));
    for (Alarm alarm : Store.all(context)) Scheduler.cancel(context, alarm.id);
    Store.prefs(context).edit().clear().commit();
  }

  @Test
  public void migratesLegacyTargetsAndReturnsMultipleSelectedCodes() throws Exception {
    Alarm first = alarm(73101, "7891035002427");
    Alarm second = alarm(73102, "DESPERTA-ACORDAR-2026");
    Store.save(context, first);
    Store.save(context, second);

    try (ActivityScenario<BarcodeLibraryActivity> scenario = launchLibrary(null, false)) {
      assertTrue(waitForText("7891035002427", 3000));
      assertTrue(waitForText(FIXTURE, 3000));
      assertTrue(Store.prefs(context).getBoolean("barcode_library_migrated", false));

      clickText("7891035002427");
      clickText(FIXTURE);
      clickText("Concluir");
      waitForDestroyed(scenario);

      assertEquals(Activity.RESULT_OK, scenario.getResult().getResultCode());
      ArrayList<String> selected =
          scenario.getResult().getResultData().getStringArrayListExtra("targets");
      assertNotNull(selected);
      assertEquals(Arrays.asList("7891035002427", FIXTURE), selected);
    }
  }

  @Test
  public void deselectingOneCodeLeavesTheOtherSelected() throws Exception {
    Alarm alarm = alarm(73103, "7891035002427");
    Store.save(context, alarm);
    Store.prefs(context)
        .edit()
        .putString("barcode_library", "[\"7891035002427\",\"DESPERTA-ACORDAR-2026\"]")
        .putBoolean("barcode_library_migrated", true)
        .commit();

    ArrayList<String> incoming = new ArrayList<>(Arrays.asList("7891035002427", FIXTURE));
    try (ActivityScenario<BarcodeLibraryActivity> scenario = launchLibrary(incoming, false)) {
      UiObject second = ui.findObject(new UiSelector().text(FIXTURE));
      assertTrue(second.waitForExists(3000));
      assertTrue(second.isChecked());
      second.click();
      ui.waitForIdle();
      assertFalse(ui.findObject(new UiSelector().text(FIXTURE)).isChecked());
      assertTrue(ui.findObject(new UiSelector().text("7891035002427")).isChecked());
      clickText("Concluir");
      waitForDestroyed(scenario);

      ArrayList<String> selected =
          scenario.getResult().getResultData().getStringArrayListExtra("targets");
      assertEquals(Arrays.asList("7891035002427"), selected);
    }
  }

  @Test
  public void reviewAndDeleteOnlyAffectsTheLibraryWhileOtherAlarmKeepsItsCode() throws Exception {
    Alarm first = alarm(73104, "7891035002427");
    Alarm second = alarm(73105, FIXTURE);
    Store.save(context, first);
    Store.save(context, second);
    Store.prefs(context).edit().putBoolean("barcode_library_migrated", false).commit();

    try (ActivityScenario<BarcodeLibraryActivity> scenario =
        launchLibrary(new ArrayList<>(Arrays.asList("7891035002427")), true)) {
      clickDescription("Opções do código 7891035002427");
      clickText("Revisar código");
      assertTrue(waitForText("7891035002427", 2000));
      clickText("Fechar");

      clickDescription("Opções do código 7891035002427");
      clickText("Excluir código");
      clickText("Excluir");
      assertTrue(ui.wait(Until.gone(By.text("7891035002427")), 3000));
      assertTrue(ui.findObject(new UiSelector().text(FIXTURE)).exists());
      assertEquals(
          Arrays.asList("7891035002427"),
          Store.get(context, first.id).missions.get(0).acceptedCodes());
      assertEquals(
          Arrays.asList(FIXTURE), Store.get(context, second.id).missions.get(0).acceptedCodes());
    }
  }

  /** Uses the configured imagefile camera fixture and verifies registration through the library. */
  @Test
  public void registeringThroughLibraryAddsTheScannedCode() throws Exception {
    try (ActivityScenario<BarcodeLibraryActivity> scenario = launchLibrary(null, false)) {
      clickText("＋  Adicionar código");
      assertTrue(waitForText(FIXTURE, 30000));
      assertTrue(waitForText("Código cadastrado e selecionado.", 3000));
    }
  }

  /** Uses the configured imagefile camera fixture and verifies the library preview callback. */
  @Test
  public void previewThroughLibraryConfirmsTheSelectedCode() throws Exception {
    Store.prefs(context)
        .edit()
        .putString("barcode_library", "[\"DESPERTA-ACORDAR-2026\"]")
        .putBoolean("barcode_library_migrated", true)
        .commit();
    try (ActivityScenario<BarcodeLibraryActivity> scenario =
        launchLibrary(new ArrayList<>(Arrays.asList(FIXTURE)), false)) {
      clickText("Testar leitura");
      assertTrue(waitForText("Leitura confirmada. Este código conclui a missão.", 30000));
    }
  }

  private ActivityScenario<BarcodeLibraryActivity> launchLibrary(
      List<String> targets, boolean editing) {
    Intent intent = new Intent(context, BarcodeLibraryActivity.class).putExtra("editing", editing);
    if (targets != null) intent.putStringArrayListExtra("targets", new ArrayList<>(targets));
    ActivityScenario<BarcodeLibraryActivity> scenario =
        ActivityScenario.launchActivityForResult(intent);
    ui.waitForIdle();
    return scenario;
  }

  private Alarm alarm(int id, String code) {
    Alarm alarm = new Alarm();
    alarm.id = id;
    alarm.days = 0;
    alarm.enabled = false;
    alarm.missions.add(new Alarm.Mission("barcode", code, 1));
    return alarm;
  }

  private void clickText(String text) throws Exception {
    UiObject object = ui.findObject(new UiSelector().textMatches("(?iu)" + java.util.regex.Pattern.quote(text)));
    assertTrue("Missing UI text: " + text, object.waitForExists(5000));
    object.click();
    ui.waitForIdle();
  }

  private void clickDescription(String description) throws Exception {
    UiObject object = ui.findObject(new UiSelector().description(description));
    assertTrue("Missing UI description: " + description, object.waitForExists(5000));
    object.click();
    ui.waitForIdle();
  }

  private boolean waitForText(String text, long timeoutMs) {
    return ui.wait(Until.hasObject(By.text(text)), timeoutMs);
  }

  private void waitForDestroyed(ActivityScenario<?> scenario) {
    long deadline = SystemClock.elapsedRealtime() + 5000;
    while (scenario.getState() != androidx.lifecycle.Lifecycle.State.DESTROYED
        && SystemClock.elapsedRealtime() < deadline) {
      SystemClock.sleep(100);
    }
    assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, scenario.getState());
  }
}
