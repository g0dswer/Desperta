package com.desperta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Covers the real NumberPicker hour/minute scrolling and persistence through the editor. */
@RunWith(AndroidJUnit4.class)
public class MainActivityWheelFlowTest {
  private Context context;
  private UiDevice ui;
  private ActivityScenario<MainActivity> scenario;

  @Before
  public void setUp() throws Exception {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    for (Alarm alarm : Store.all(context)) Scheduler.cancel(context, alarm.id);
    context.stopService(new android.content.Intent(context, AlarmService.class));
    Store.prefs(context).edit().clear().putBoolean(UpdateChecker.KEY_AUTO_CHECK, false).commit();
    scenario = ActivityScenario.launch(MainActivity.class);
    ui.waitForIdle();
  }

  @After
  public void tearDown() {
    if (scenario != null) scenario.close();
    context.stopService(new android.content.Intent(context, AlarmService.class));
    for (Alarm alarm : Store.all(context)) Scheduler.cancel(context, alarm.id);
    Store.prefs(context).edit().clear().putBoolean(UpdateChecker.KEY_AUTO_CHECK, false).commit();
  }

  @Test
  public void scrollingBothWheelsChangesTheDraftAndSurvivesSaveAndReopen() throws Exception {
    click("+ Alarme");
    UiSelector pickerSelector = new UiSelector().className("android.widget.NumberPicker");
    UiObject hours = ui.findObject(pickerSelector.instance(0));
    UiObject minutes = ui.findObject(pickerSelector.instance(1));
    assertTrue(hours.waitForExists(3000));
    assertTrue(minutes.waitForExists(3000));
    assertTrue(ui.findObject(new UiSelector().className("android.widget.NumberPicker")).exists());

    int[] before = draftTime();
    scrollPicker(hours);
    int[] afterHour = draftTime();
    assertTrue("A rolagem da hora não alterou o valor", before[0] != afterHour[0]);

    scrollPicker(minutes);
    int[] afterMinute = draftTime();
    assertEquals(afterHour[0], afterMinute[0]);
    assertTrue("A rolagem dos minutos não alterou o valor", before[1] != afterMinute[1]);

    click("Salvar alarme");
    Alarm saved = Store.all(context).get(0);
    assertEquals(afterMinute[0], saved.hour);
    assertEquals(afterMinute[1], saved.minute);

    scenario.close();
    scenario = ActivityScenario.launch(MainActivity.class);
    ui.waitForIdle();
    ui.findObject(new UiSelector().descriptionStartsWith("Editar alarme Bom dia")).click();
    ui.waitForIdle();
    int[] reopened = draftTime();
    assertEquals(afterMinute[0], reopened[0]);
    assertEquals(afterMinute[1], reopened[1]);
  }

  @Test
  public void barcodeTargetsSelectedInLibraryPersistThroughEditorSaveAndReopen() throws Exception {
    Store.prefs(context)
        .edit()
        .putString("barcode_library", "[\"7891035002427\",\"DESPERTA-ACORDAR-2026\"]")
        .putBoolean("barcode_library_migrated", true)
        .commit();

    click("+ Alarme");
    ui.findObject(new UiSelector().descriptionStartsWith("Como desligar,")).click();
    click("+ Adicionar missão");
    click("QR / Código de barras");
    click("Configurar");
    click("7891035002427");
    click("DESPERTA-ACORDAR-2026");
    click("Concluir");
    assertTrue(ui.findObject(new UiSelector().textContains("7891035002427")).waitForExists(3000));
    click("Salvar alarme");

    scenario.close();
    scenario = ActivityScenario.launch(MainActivity.class);
    ui.waitForIdle();
    ui.findObject(new UiSelector().descriptionStartsWith("Editar alarme Bom dia")).click();
    ui.waitForIdle();
    ui.findObject(new UiSelector().descriptionStartsWith("Como desligar,")).click();
    click("1. QR / Código de barras  ›");
    click("Configurar");
    assertTrue(ui.findObject(new UiSelector().text("7891035002427")).isChecked());
    assertTrue(ui.findObject(new UiSelector().text("DESPERTA-ACORDAR-2026")).isChecked());
  }

  private int[] draftTime() {
    int[] value = new int[2];
    scenario.onActivity(
        activity -> {
          assertTrue(activity.draft != null);
          value[0] = activity.draft.hour;
          value[1] = activity.draft.minute;
        });
    return value;
  }

  private void scrollPicker(UiObject picker) throws Exception {
    Rect bounds = picker.getBounds();
    boolean moved =
        ui.swipe(
            bounds.centerX(), bounds.centerY() + 90, bounds.centerX(), bounds.centerY() - 90, 24);
    assertTrue("A rolagem do NumberPicker não foi enviada", moved);
    ui.waitForIdle();
    SystemClock.sleep(150);
  }

  private void click(String text) throws Exception {
    UiObject object =
        ui.findObject(
            text.equals("+ Alarme")
                ? new UiSelector().description("Adicionar alarme")
                : new UiSelector().textMatches("(?iu)" + java.util.regex.Pattern.quote(text)));
    assertTrue("Missing UI text: " + text, object.waitForExists(5000));
    object.click();
    ui.waitForIdle();
  }
}
