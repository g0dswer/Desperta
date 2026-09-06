package com.desperta;

import static org.junit.Assert.*;

import android.content.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppFlowTest {
  Context c;
  UiDevice ui;
  ActivityScenario<MainActivity> scenario;

  @Before
  public void setup() throws Exception {
    c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    for (Alarm a : Store.all(c)) Scheduler.cancel(c, a.id);
    c.getSharedPreferences("desperta", 0).edit().clear().commit();
    ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    scenario = ActivityScenario.launch(MainActivity.class);
    ui.waitForIdle();
  }

  @After
  public void cleanup() {
    if (scenario != null) scenario.close();
  }

  void click(String text) throws Exception {
    UiObject obj =
        ui.findObject(new UiSelector().textMatches("(?iu)" + java.util.regex.Pattern.quote(text)));
    if (!obj.exists()) {
      UiScrollable scroll = new UiScrollable(new UiSelector().scrollable(true));
      scroll.scrollTextIntoView(text);
    }
    assertTrue("Missing UI: " + text, obj.waitForExists(3000));
    obj.click();
    ui.waitForIdle();
  }

  @Test
  public void createEditPersistDuplicateSkipDelete() throws Exception {
    click("＋  Novo alarme");
    click("☀  Bom dia");
    ui.findObject(new UiSelector().className("android.widget.EditText")).setText("Teste manhã");
    click("Confirmar");
    click("Salvar alarme");
    assertEquals(1, Store.all(c).size());
    assertEquals("Teste manhã", Store.all(c).get(0).label);
    scenario.recreate();
    ui.waitForIdle();
    assertTrue(ui.findObject(new UiSelector().text("Teste manhã")).exists());
    click("Editar  ·  Mais opções");
    click("Duplicar alarme");
    assertEquals(2, Store.all(c).size());
    ui.findObject(new UiSelector().text("Editar  ·  Mais opções").instance(0)).click();
    click("Pular uma vez");
    assertTrue(Store.all(c).stream().anyMatch(a -> a.skipUntil > 0));
    ui.findObject(new UiSelector().text("Editar  ·  Mais opções").instance(0)).click();
    click("Excluir");
    click("Excluir");
    assertEquals(1, Store.all(c).size());
  }

  @Test
  public void editorMissionAndOptionsPersist() throws Exception {
    click("＋  Novo alarme");
    click("＋  Adicionar missão");
    click("Matemática");
    ui.findObject(new UiSelector().className("android.widget.EditText")).setText("4");
    click("Confirmar");
    click("Falar a hora");
    click("Falar o nome do alarme");
    click("Salvar alarme");
    Alarm a = Store.all(c).get(0);
    assertEquals("math", a.missions.get(0).type);
    assertEquals(4, a.missions.get(0).count);
    assertTrue(a.timeReminder);
    assertTrue(a.labelReminder);
  }

  @Test
  public void discardDoesNotCreateAlarm() throws Exception {
    click("＋  Novo alarme");
    ui.pressBack();
    click("Descartar");
    assertTrue(Store.all(c).isEmpty());
  }

  @Test
  public void tabsExcludedAndSettingsOpen() throws Exception {
    assertFalse(ui.findObject(new UiSelector().text("Sono")).exists());
    assertFalse(ui.findObject(new UiSelector().text("Manhã")).exists());
    assertFalse(ui.findObject(new UiSelector().text("Relatório")).exists());
    click("Ajustes");
    assertTrue(ui.wait(Until.hasObject(By.pkg("com.desperta")), 3000));
  }

  @Test
  public void allAlarmSwitchesSnoozeWallpaperAndMissionLimitPersist() throws Exception {
    click("＋  Novo alarme");
    ui.findObject(new UiSelector().description("Dia 0")).click();
    ui.waitForIdle();
    new UiScrollable(new UiSelector().scrollable(true)).scrollTextIntoView("Volume: 80%");
    android.graphics.Rect volumeBounds =
        ui.findObject(new UiSelector().className("android.widget.SeekBar")).getBounds();
    ui.click(volumeBounds.left + volumeBounds.width() / 10, volumeBounds.centerY());
    click("Vibração");
    click("Falar a hora");
    click("Falar a previsão do tempo");
    click("Falar o nome do alarme");
    click("Efeito de volume máximo");
    click("Despertar gradual: 60 s");
    click("30 segundos");
    click("Confirmar despertar: desligado");
    click("3 minutos");
    click("Soneca: 10 min · 1 vezes");
    ui.findObject(new UiSelector().className("android.widget.EditText")).setText("7");
    click("Confirmar");
    ui.findObject(new UiSelector().className("android.widget.EditText")).setText("2");
    click("Confirmar");
    click("Papel de parede: Aurora");
    click("Oceano");
    for (int i = 0; i < 5; i++) {
      click("＋  Adicionar missão");
      click("Matemática");
      click("Confirmar");
    }
    assertFalse(ui.findObject(new UiSelector().text("＋  Adicionar missão")).exists());
    click("Salvar alarme");
    Alarm a = Store.all(c).get(0);
    assertEquals(126, a.days);
    assertTrue(a.volume < 30);
    assertFalse(a.vibrate);
    assertTrue(a.timeReminder);
    assertTrue(a.weatherReminder);
    assertTrue(a.labelReminder);
    assertTrue(a.extraLoud);
    assertEquals(30, a.gentleSeconds);
    assertEquals(3, a.wakeCheckMinutes);
    assertEquals(7, a.snoozeMinutes);
    assertEquals(2, a.snoozeLimit);
    assertEquals("Oceano", a.wallpaper);
    assertEquals(5, a.missions.size());
  }

  @Test
  public void enableSwitchPersistsBothDirections() throws Exception {
    click("＋  Novo alarme");
    click("Salvar alarme");
    ui.findObject(new UiSelector().description("Ativar Bom dia")).click();
    ui.waitForIdle();
    assertFalse(Store.all(c).get(0).enabled);
    ui.findObject(new UiSelector().description("Ativar Bom dia")).click();
    ui.waitForIdle();
    assertTrue(Store.all(c).get(0).enabled);
  }
}
