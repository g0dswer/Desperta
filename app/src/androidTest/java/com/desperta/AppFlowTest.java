package com.desperta;

import static org.junit.Assert.*;

import android.content.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;

/** End-to-end tasks through the Amanhecer interface, including persistence and undo. */
@RunWith(AndroidJUnit4.class)
public class AppFlowTest {
  Context c;
  UiDevice ui;
  ActivityScenario<MainActivity> scenario;

  @Before
  public void setup() {
    c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    c.stopService(new Intent(c, AlarmService.class));
    for (Alarm a : Store.all(c)) Scheduler.cancel(c, a.id);
    Store.prefs(c).edit().clear().putBoolean(UpdateChecker.KEY_AUTO_CHECK, false).commit();
    ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    scenario = ActivityScenario.launch(MainActivity.class);
    ui.waitForIdle();
  }

  @After
  public void cleanup() {
    if (scenario != null) scenario.close();
    c.stopService(new Intent(c, AlarmService.class));
    for (Alarm a : Store.all(c)) Scheduler.cancel(c, a.id);
    Store.prefs(c).edit().clear().putBoolean(UpdateChecker.KEY_AUTO_CHECK, false).commit();
  }

  void click(String text) throws Exception {
    UiSelector selector =
        new UiSelector().textMatches("(?iu)" + java.util.regex.Pattern.quote(text));
    UiObject obj = ui.findObject(selector);
    if (!obj.exists())
      new UiScrollable(new UiSelector().className("android.widget.ScrollView").scrollable(true))
          .scrollIntoView(selector);
    assertTrue("Missing UI: " + text, obj.waitForExists(3000));
    obj.click();
    ui.waitForIdle();
  }

  void desc(String prefix) throws Exception {
    UiSelector selector = new UiSelector().descriptionStartsWith(prefix);
    UiObject obj = ui.findObject(selector);
    if (!obj.exists())
      new UiScrollable(new UiSelector().className("android.widget.ScrollView").scrollable(true))
          .scrollIntoView(selector);
    assertTrue("Missing UI description: " + prefix, obj.waitForExists(3000));
    obj.click();
    ui.waitForIdle();
  }

  void text(String value) throws Exception {
    ui.findObject(new UiSelector().className("android.widget.EditText")).setText(value);
  }

  void addMath(String count) throws Exception {
    click("+ Adicionar missão");
    click("Matemática");
    click("Configurar");
    text(count);
    click("Confirmar");
    click("Adicionar missão");
  }

  @Test
  public void createEditPersistDuplicateSkipDeleteAndUndo() throws Exception {
    click("+ Alarme");
    desc("Nome do alarme:");
    text("Teste manhã");
    click("Confirmar");
    click("Salvar alarme");
    assertEquals(1, Store.all(c).size());
    assertEquals("Teste manhã", Store.all(c).get(0).label);
    scenario.recreate();
    ui.waitForIdle();
    assertTrue(ui.findObject(new UiSelector().text("Teste manhã")).exists());
    desc("Mais opções de Teste manhã");
    click("Duplicar alarme");
    assertEquals(2, Store.all(c).size());
    desc("Mais opções de Teste manhã");
    ui.findObject(new UiSelector().textStartsWith("Pular ")).click();
    ui.waitForIdle();
    assertTrue(Store.all(c).stream().anyMatch(a -> a.skipUntil > 0));
    click("Desfazer");
    assertTrue(Store.all(c).stream().allMatch(a -> a.skipUntil == 0));
    desc("Mais opções de Teste manhã");
    click("Excluir");
    assertEquals(1, Store.all(c).size());
    click("Desfazer");
    assertEquals(2, Store.all(c).size());
  }

  @Test
  public void editorMissionAndCollapsedOptionsPersist() throws Exception {
    click("+ Alarme");
    assertFalse(ui.findObject(new UiSelector().text("Falar a hora")).exists());
    desc("Como desligar,");
    addMath("4");
    desc("Voz,");
    click("Falar a hora");
    click("Falar o nome do alarme");
    click("Salvar alarme");
    Alarm a = Store.all(c).get(0);
    assertEquals("math", a.missions.get(0).type);
    assertEquals(4, a.missions.get(0).count);
    assertTrue(a.timeReminder);
    assertTrue(a.labelReminder);
    desc("Editar alarme Bom dia");
    desc("Voz,");
    assertTrue(ui.findObject(new UiSelector().text("Falar a hora")).isChecked());
  }

  @Test
  public void discardDoesNotCreateAlarm() throws Exception {
    click("+ Alarme");
    ui.pressBack();
    click("Descartar");
    assertTrue(Store.all(c).isEmpty());
  }

  @Test
  public void removedTabsAndSettingsAccessibleFromHeader() throws Exception {
    for (String tab : new String[] {"Sono", "Manhã", "Relatório", "GRATUITO"})
      assertFalse(ui.findObject(new UiSelector().text(tab)).exists());
    desc("Ajustes");
    assertTrue(ui.findObject(new UiSelector().text("Preferências")).waitForExists(3000));
  }

  @Test
  public void allAlarmSwitchesSnoozeWallpaperAndMissionLimitPersist() throws Exception {
    click("+ Alarme");
    desc("Domingo");
    desc("Som,");
    new UiScrollable(new UiSelector().className("android.widget.ScrollView").scrollable(true))
        .scrollTextIntoView("Volume: 80%");
    android.graphics.Rect bounds =
        ui.findObject(new UiSelector().className("android.widget.SeekBar")).getBounds();
    ui.click(bounds.left + bounds.width() / 10, bounds.centerY());
    click("Vibração");
    click("Efeito de volume máximo");
    click("Despertar gradual: 60 s");
    click("30 segundos");
    desc("Voz,");
    click("Falar a hora");
    click("Falar a previsão do tempo");
    click("Falar o nome do alarme");
    desc("Soneca,");
    click("Confirmar despertar: desligado");
    click("3 minutos");
    click("Soneca: 10 min · 1 vez");
    text("7");
    click("Confirmar");
    text("2");
    click("Confirmar");
    desc("Aparência,");
    click("Papel de parede: Aurora");
    click("Oceano");
    desc("Como desligar,");
    for (int i = 0; i < 5; i++) addMath("3");
    assertFalse(ui.findObject(new UiSelector().text("+ Adicionar missão")).exists());
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
    click("+ Alarme");
    click("Salvar alarme");
    desc("Ativar Bom dia");
    assertFalse(Store.all(c).get(0).enabled);
    desc("Ativar Bom dia");
    assertTrue(Store.all(c).get(0).enabled);
  }

  @Test
  public void presetsAndMissionReorderingSurviveRecreation() throws Exception {
    click("+ Alarme");
    click("Repetir · Todos os dias  ›");
    click("Dias úteis");
    desc("Como desligar,");
    addMath("2");
    click("+ Adicionar missão");
    click("Digitação");
    click("Configurar");
    text("Acordei");
    click("Confirmar");
    click("Adicionar missão");
    click("2. Digitação  ›");
    click("Mover para cima");
    scenario.recreate();
    ui.waitForIdle();
    click("Salvar alarme");
    Alarm saved = Store.all(c).get(0);
    assertEquals(62, saved.days);
    assertEquals("typing", saved.missions.get(0).type);
    assertEquals("Acordei", saved.missions.get(0).target);
    assertEquals("math", saved.missions.get(1).type);
  }

  @Test
  public void tryingCandidateDoesNotAddItUntilConfirmed() throws Exception {
    click("+ Alarme");
    desc("Como desligar,");
    click("+ Adicionar missão");
    click("Digitação");
    click("Configurar");
    text("Acordei");
    click("Confirmar");
    click("Experimentar");
    // Cancel the actual mission; the configured candidate must remain reviewable but unsaved.
    ui.pressBack();
    ui.waitForIdle();
    scenario.onActivity(a -> assertTrue(a.draft.missions.isEmpty()));
    click("Adicionar missão");
    click("Salvar alarme");
    assertEquals(1, Store.all(c).get(0).missions.size());
    assertNull(Store.getSession(c));
  }

  @Test
  public void personalTemplatesCreateIndependentAlarms() throws Exception {
    click("Usar um modelo");
    click("Trabalho · Seg–Sex, 07:00");
    click("Salvar alarme");
    desc("Mais opções de Trabalho");
    click("Salvar como modelo");
    text("Minha rotina");
    click("Confirmar");
    click("Usar um modelo");
    click("Minha rotina");
    click("Salvar alarme");
    List<Alarm> list = Store.all(c);
    assertEquals(2, list.size());
    assertNotEquals(list.get(0).id, list.get(1).id);
    assertTrue(list.stream().allMatch(a -> a.hour == 7 && a.minute == 0 && a.days == 62));
  }

  @Test
  public void nextOnlyEditPreservesWeeklyClockAndCanBeUndone() throws Exception {
    click("+ Alarme");
    click("Salvar alarme");
    desc("Mais opções de Bom dia");
    click("Só na próxima vez");
    UiObject minute =
        ui.findObject(new UiSelector().className("android.widget.NumberPicker").instance(1));
    android.graphics.Rect bounds = minute.getBounds();
    ui.swipe(bounds.centerX(), bounds.centerY() + 60, bounds.centerX(), bounds.centerY() - 60, 25);
    ui.waitForIdle();
    click("Aplicar");
    Alarm changed = Store.all(c).get(0);
    assertEquals(7, changed.hour);
    assertEquals(30, changed.minute);
    assertTrue(changed.nextOverrideAt > System.currentTimeMillis());
    click("Desfazer");
    assertEquals(0, Store.all(c).get(0).nextOverrideAt);
  }
}
