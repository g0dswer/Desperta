package com.desperta;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import java.io.File;
import org.junit.*;
import org.junit.runner.RunWith;

/** Usability regression: larger text and light dialogs must retain usable, visible actions. */
@RunWith(AndroidJUnit4.class)
public class AppearanceFlowTest {
  Context context;
  UiDevice ui;
  ActivityScenario<MainActivity> scenario;
  String oldScale;
  String oldIdentity;

  @Before
  public void setup() throws Exception {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    for (Alarm a : Store.all(context)) Scheduler.cancel(context, a.id);
    context.stopService(new Intent(context, AlarmService.class));
    oldIdentity = Store.prefs(context).getString(Identity.KEY, null);
    Store.prefs(context).edit().clear().putBoolean(UpdateChecker.KEY_AUTO_CHECK, false).commit();
    oldScale = ui.executeShellCommand("settings get system font_scale").trim();
  }

  @After
  public void cleanup() throws Exception {
    if (scenario != null) scenario.close();
    ui.executeShellCommand(
        "settings put system font_scale " + (oldScale.matches("[0-9.]+") ? oldScale : "1.0"));
    for (Alarm a : Store.all(context)) Scheduler.cancel(context, a.id);
    Store.prefs(context).edit().clear().commit();
    if (oldIdentity != null) Identity.set(context, oldIdentity);
  }

  void click(String label) throws Exception {
    UiSelector sel = new UiSelector().textMatches("(?iu)" + java.util.regex.Pattern.quote(label));
    UiObject obj = ui.findObject(sel);
    if (!obj.exists())
      new UiScrollable(new UiSelector().className("android.widget.ScrollView").scrollable(true))
          .scrollIntoView(sel);
    assertTrue(label, obj.waitForExists(3000));
    obj.click();
    ui.waitForIdle();
  }

  void desc(String prefix) throws Exception {
    UiSelector sel = new UiSelector().descriptionStartsWith(prefix);
    UiObject obj = ui.findObject(sel);
    if (!obj.exists())
      new UiScrollable(new UiSelector().className("android.widget.ScrollView").scrollable(true))
          .scrollIntoView(sel);
    assertTrue(prefix, obj.waitForExists(3000));
    obj.click();
    ui.waitForIdle();
  }

  void capture(String name) {
    assertTrue(ui.takeScreenshot(new File(context.getExternalFilesDir(null), name)));
  }

  @Test
  public void retroIdentityAtLargeFontKeepsEditorAndSaveUsable() throws Exception {
    Identity.set(context, Identity.RETRO);
    ui.executeShellCommand("settings put system font_scale 1.3");
    scenario = ActivityScenario.launch(MainActivity.class);
    ui.waitForIdle();
    desc("Adicionar alarme");
    capture("retro-editor-light-large.png");
    desc("Nome do alarme:");
    ui.findObject(new UiSelector().className("android.widget.EditText")).setText("Letras grandes");
    capture("retro-dialog-light-large.png");
    click("Confirmar");
    UiObject save = ui.findObject(new UiSelector().text("Salvar alarme"));
    Rect bounds = save.getVisibleBounds();
    float density = context.getResources().getDisplayMetrics().density;
    assertTrue("Save target must stay at least48dp", bounds.height() / density >= 48);
    click("Salvar alarme");
    assertEquals("Letras grandes", Store.all(context).get(0).label);
    desc("Ajustes");
    assertTrue(ui.findObject(new UiSelector().text("Preferências")).waitForExists(3000));
  }

  @Test
  public void retroEditorAndNextAlarmRemainClearAfterSaving() throws Exception {
    Identity.set(context, Identity.RETRO);
    Alarm alarm = new Alarm();
    alarm.label = "Trabalho";
    alarm.days = 62;
    alarm.missions.add(new Alarm.Mission("barcode", "7891035002427", 1));
    Store.save(context, alarm);
    scenario = ActivityScenario.launch(MainActivity.class);
    ui.waitForIdle();
    capture("retro-alarmes.png");
    desc("Editar alarme Trabalho");
    capture("retro-editor.png");
    desc("Como desligar,");
    assertTrue(ui.findObject(new UiSelector().textContains("7891035002427")).exists());
    click("Salvar alarme");
    assertEquals("7891035002427", Store.all(context).get(0).missions.get(0).target);
  }
}
