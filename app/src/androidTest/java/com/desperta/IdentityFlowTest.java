package com.desperta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Device-side contract for the four complete visual identities.
 *
 * <p>The test makes the identity contract explicit: four ids, a retro default, persistent
 * selection, distinct palettes and all major surfaces rendered with the selected identity. The test
 * does not fake scanner or alarm results.
 */
@RunWith(AndroidJUnit4.class)
public final class IdentityFlowTest {
  private static final String BARCODE = "7891035002427";

  private Context context;
  private UiDevice ui;
  private String oldIdentity;
  private String oldFontScale;
  private ActivityScenario<MainActivity> main;

  @Before
  public void setUp() throws Exception {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    oldIdentity = IdentityBridge.currentId(context);
    oldFontScale = ui.executeShellCommand("settings get system font_scale").trim();
    stopAndClear();
    Store.prefs(context).edit().clear().putBoolean(UpdateChecker.KEY_AUTO_CHECK, false).commit();
  }

  @After
  public void tearDown() throws Exception {
    if (main != null) main.close();
    ui.executeShellCommand(
        "settings put system font_scale "
            + (oldFontScale.matches("[0-9.]+") ? oldFontScale : "1.0"));
    stopAndClear();
    Store.prefs(context).edit().clear().putBoolean(UpdateChecker.KEY_AUTO_CHECK, false).commit();
    if (oldIdentity != null && !oldIdentity.isEmpty()) IdentityBridge.set(context, oldIdentity);
  }

  /** Empty and legacy (theme-only) preferences must select the new retro default. */
  @Test
  public void emptyAndLegacyPreferencesDefaultToRetrofuturista() throws Exception {
    Store.prefs(context).edit().clear().putString("theme", "dark").commit();
    main = ActivityScenario.launch(MainActivity.class);
    ui.waitForIdle();

    String current = IdentityBridge.currentId(context);
    assertTrue("default identity must be retro", isRetro(current));
    assertTrue(ui.findObject(new UiSelector().text("Desperta")).waitForExists(3000));
    capture("retro-home.png");
  }

  /**
   * The Settings row must expose every identity and the choice must survive recreation and
   * relaunch.
   */
  @Test
  public void settingsSelectsEachIdentityAndPersistsAcrossRecreateAndRelaunch() throws Exception {
    String[] ids = IdentityBridge.ids();
    String[] names = IdentityBridge.names();
    assertEquals("the selector must expose four identities", 4, ids.length);
    assertTrue("one identity must be retro", containsRetro(ids, names));

    try (ActivityScenario<SettingsActivity> settings =
        ActivityScenario.launch(SettingsActivity.class)) {
      ui.waitForIdle();
      assertTrue(findTextContains("Identidade", 4000));
      for (int i = 0; i < ids.length; i++) {
        String selectedId = ids[i];
        String selectedName = names[i];
        // The single-choice dialog closes after each tap; reopen the settings row for every id.
        clickTextContains("Identidade");
        assertTrue("missing identity option " + selectedName, findTextContains(selectedName, 4000));
        clickTextContains(selectedName);
        clickOptional("Aplicar", "Concluído", "Confirmar", "Salvar", "OK");
        ui.waitForIdle();

        assertEquals(selectedId, IdentityBridge.currentId(context));
        settings.recreate();
        ui.waitForIdle();
        assertTrue(findTextContains("Identidade", 4000));
        assertTrue(
            "settings must show the selected identity after recreate",
            findTextContains(selectedName, 2000));
      }
    }

    try (ActivityScenario<SettingsActivity> relaunched =
        ActivityScenario.launch(SettingsActivity.class)) {
      ui.waitForIdle();
      String lastName = names[names.length - 1];
      assertTrue(findTextContains(lastName, 3000));
      capture("identity-" + safeId(ids[ids.length - 1]) + "-settings-relaunch.png");
    }
  }

  /**
   * Every identity receives a real home/editor/settings/library/mission/ring traversal. Screenshots
   * are retained in the app's external files directory so the release owner can inspect each
   * surface visually.
   */
  @Test
  public void everyIdentityRendersAllMajorSurfaces() throws Exception {
    seedRepresentativeAlarms();
    String[] ids = IdentityBridge.ids();
    String[] names = IdentityBridge.names();
    assertEquals(4, ids.length);
    Map<String, String> paletteSignatures = new HashMap<>();

    for (int i = 0; i < ids.length; i++) {
      String id = ids[i];
      String slug = safeId(id);
      IdentityBridge.set(context, id);
      if (main != null) {
        main.close();
        main = null;
      }
      main = ActivityScenario.launch(MainActivity.class);
      ui.waitForIdle();
      assertEquals(id, IdentityBridge.currentId(context));
      main.onActivity(activity -> assertWindowIdentity(activity, id));
      paletteSignatures.put(id, IdentityBridge.paletteSignature(context));

      assertTrue(ui.findObject(new UiSelector().text("Trabalho")).waitForExists(3000));
      assertTrue(ui.findObject(new UiSelector().text("Fim de semana")).waitForExists(3000));
      capture("identity-" + slug + "-home.png");

      clickDescription("Mais opções de Trabalho");
      capture("identity-" + slug + "-menu.png");
      ui.pressBack();
      ui.waitForIdle();

      clickDescription("Editar alarme Trabalho");
      assertTrue(ui.findObject(new UiSelector().text("Seu alarme")).waitForExists(3000));
      capture("identity-" + slug + "-editor.png");
      clickDescription("Nome do alarme: Trabalho");
      assertTrue(ui.findObject(new UiSelector().className("android.widget.EditText")).exists());
      capture("identity-" + slug + "-dialog.png");
      ui.pressBack();
      ui.waitForIdle();
      clickText("Salvar alarme");

      clickDescription("Ajustes");
      assertTrue(findTextContains("Identidade", 4000));
      assertEquals(id, IdentityBridge.currentId(context));
      capture("identity-" + slug + "-settings.png");
      ui.pressBack();
      ui.waitForIdle();

      Intent libraryIntent =
          new Intent(context, BarcodeLibraryActivity.class)
              .putExtra("editing", false)
              .putStringArrayListExtra("targets", new ArrayList<>(Arrays.asList(BARCODE)));
      try (ActivityScenario<BarcodeLibraryActivity> library =
          ActivityScenario.launchActivityForResult(libraryIntent)) {
        ui.waitForIdle();
        assertTrue(ui.findObject(new UiSelector().text(BARCODE)).waitForExists(3000));
        library.onActivity(activity -> assertWindowIdentity(activity, id));
        capture("identity-" + slug + "-library.png");
      }

      Intent missionIntent =
          new Intent(context, MissionActivity.class)
              .putExtra(MissionActivity.EXTRA_TYPE, "math")
              .putExtra(MissionActivity.EXTRA_MODE, "solve")
              .putExtra(MissionActivity.EXTRA_TARGET, "2+2")
              .putExtra(MissionActivity.EXTRA_COUNT, 1);
      try (ActivityScenario<MissionActivity> mission = ActivityScenario.launch(missionIntent)) {
        ui.waitForIdle();
        assertTrue(ui.findObject(new UiSelector().text("Matemática")).waitForExists(3000));
        assertTrue(ui.findObject(new UiSelector().text("Verificar resposta")).exists());
        mission.onActivity(activity -> assertWindowIdentity(activity, id));
        capture("identity-" + slug + "-mission.png");
      }

      Alarm work = findAlarm("Trabalho");
      assertNotNull(work);
      Intent ringIntent =
          new Intent(context, RingActivity.class)
              .putExtra(Scheduler.EXTRA_ALARM_ID, work.id)
              .putExtra(Scheduler.EXTRA_PREVIEW, true);
      try (ActivityScenario<RingActivity> ring = ActivityScenario.launch(ringIntent)) {
        ui.waitForIdle();
        assertTrue(ui.findObject(new UiSelector().text("Prévia do alarme")).waitForExists(3000));
        assertTrue(ui.findObject(new UiSelector().text("Escanear código")).exists());
        ring.onActivity(activity -> assertWindowIdentity(activity, id));
        capture("identity-" + slug + "-ring.png");
      }

      main.close();
      main = null;
    }

    assertEquals(
        "each identity must have its own visual palette",
        4,
        new HashSet<>(paletteSignatures.values()).size());
    assertTrue("identity names should be available for visual review", names.length == ids.length);
  }

  @Test
  public void everyIdentityCanCreateAlarmFromItsHomeAction() throws Exception {
    for (String id : IdentityBridge.ids()) {
      stopAndClear();
      IdentityBridge.set(context, id);
      main = ActivityScenario.launch(MainActivity.class);
      ui.waitForIdle();
      clickDescription("Adicionar alarme");
      clickText("Salvar alarme");
      assertEquals(
          "home create action must persist an alarm in " + id, 1, Store.all(context).size());
      main.close();
      main = null;
    }
  }

  /** Captures only the four home references so visual review is independent of deeper flows. */
  @Test
  public void captureFourHomeReferences() throws Exception {
    seedRepresentativeAlarms();
    ui.executeShellCommand("settings put system font_scale 1.0");
    String requested = InstrumentationRegistry.getArguments().getString("captureIdentity", "");
    for (String id : IdentityBridge.ids()) {
      if (!requested.isEmpty() && !requested.equals(id)) continue;
      IdentityBridge.set(context, id);
      if (main != null) {
        main.close();
        main = null;
      }
      main = ActivityScenario.launch(MainActivity.class);
      ui.waitForIdle();
      Thread.sleep(400L); // Capture after the finite Activity enter transition finishes.
      capture("identity-" + safeId(id) + "-home.png");
    }
  }

  /** The live 1990s seven-segment clock must paint inside its real rendered home bounds. */
  @Test
  public void ninetiesDigitalClockRendersAndUpdatesFromAlarmTime() throws Exception {
    seedRepresentativeAlarms();
    IdentityBridge.set(context, Identity.NINETIES);
    main = ActivityScenario.launch(MainActivity.class);
    ui.waitForIdle();

    Alarm work = findAlarm("Trabalho");
    assertNotNull(work);
    assertEquals(7, work.hour);
    assertEquals(30, work.minute);
    Rect firstBounds = digitalClockBounds("07:30");
    Bitmap first = captureBitmap("nineties-clock-0730.png");
    long firstSignature;
    try {
      firstSignature = digitalInkSignature(first, firstBounds);
    } finally {
      first.recycle();
    }

    // Change the persisted alarm and recreate the real home so the clock is rebuilt from Store;
    // this exercises the same data/UI path as a user editing an alarm.
    work.hour = 8;
    work.minute = 45;
    Store.save(context, work);
    main.recreate();
    ui.waitForIdle();
    Rect secondBounds = digitalClockBounds("08:45");
    Bitmap second = captureBitmap("nineties-clock-0845.png");
    long secondSignature;
    try {
      secondSignature = digitalInkSignature(second, secondBounds);
    } finally {
      second.recycle();
    }
    assertNotEquals(
        "changing the persisted alarm time must change the rendered seven-segment face",
        firstSignature,
        secondSignature);
  }

  /** All four home identities keep the alarm enabled toggle native and persistent. */
  @Test
  public void alarmTogglePersistsAcrossAllIdentitiesIncludingTerminalTextToggle() throws Exception {
    seedRepresentativeAlarms();
    for (String id : IdentityBridge.ids()) {
      IdentityBridge.set(context, id);
      if (main != null) {
        main.close();
        main = null;
      }
      main = ActivityScenario.launch(MainActivity.class);
      ui.waitForIdle();

      Alarm work = findAlarm("Trabalho");
      assertNotNull(work);
      assertTrue("fixture alarm must start enabled", work.enabled);
      clickDescription("Ativar Trabalho");
      waitForAlarmEnabled(work.id, false);
      assertFalse(Store.get(context, work.id).enabled);
      if (Identity.TERMINAL.equals(id)) {
        assertTrue(ui.findObject(new UiSelector().text("[PAUSADO]")).waitForExists(3000));
      }

      main.recreate();
      ui.waitForIdle();
      assertFalse(
          "disabled state must survive home recreation for " + id,
          Store.get(context, work.id).enabled);
      clickDescription("Ativar Trabalho");
      waitForAlarmEnabled(work.id, true);
      assertTrue(Store.get(context, work.id).enabled);
      if (Identity.TERMINAL.equals(id)) {
        assertTrue(ui.findObject(new UiSelector().text("[ATIVO]")).waitForExists(3000));
      }
    }
  }

  /** Home creation, editing, menu duplication and the fixed save action remain usable at 130%. */
  @Test
  public void homeControlsAndEditorRemainUsableAtLargeFont() throws Exception {
    ui.executeShellCommand("settings put system font_scale 1.3");
    IdentityBridge.set(context, retroId());
    main = ActivityScenario.launch(MainActivity.class);
    ui.waitForIdle();

    clickDescription("Adicionar alarme");
    clickDescription("Nome do alarme: Bom dia");
    ui.findObject(new UiSelector().className("android.widget.EditText")).setText("Fonte grande");
    clickText("Confirmar");
    Rect saveBounds = ui.findObject(new UiSelector().text("Salvar alarme")).getVisibleBounds();
    assertTrue(
        "save action must remain at least 48dp high at 130% font",
        saveBounds.height() / context.getResources().getDisplayMetrics().density >= 48);
    clickText("Salvar alarme");
    assertNotNull(findAlarm("Fonte grande"));

    clickDescription("Mais opções de Fonte grande");
    clickText("Duplicar alarme");
    // The dialog callback may be asynchronous; allow the persisted copy to settle before asserting.
    long deadline = System.currentTimeMillis() + 3000;
    while (Store.all(context).size() < 2 && System.currentTimeMillis() < deadline) {
      Thread.sleep(50L);
    }
    assertEquals(2, Store.all(context).size());
    capture("retro-home-font130.png");
  }

  /** The live portrait scanner is themed for every identity while the QR fixture is kept open. */
  @Test
  public void scannerSurfaceUsesEachIdentity() throws Exception {
    grantCamera();
    for (String id : IdentityBridge.ids()) {
      IdentityBridge.set(context, id);
      ActivityScenario<AlarmCaptureActivity> scanner = null;
      try {
        Intent launch =
            new Intent(context, AlarmCaptureActivity.class)
                .setAction("com.google.zxing.client.android.SCAN")
                // The configured fixture is QR/EAN; DATA_MATRIX keeps this visual test on the
                // live camera surface instead of completing it during launch.
                .putExtra("SCAN_FORMATS", "DATA_MATRIX");
        scanner = ActivityScenario.launchActivityForResult(launch);
        assertTrue(
            "scanner hint missing for " + id,
            ui.wait(
                Until.hasObject(By.text("Posicione o QR/código de barras dentro do retângulo")),
                10_000));
        assertTrue(ui.wait(Until.hasObject(By.text("Lanterna")), 10_000));
        // ActivityScenario.onActivity waits for instrumentation idle. The scanner continuously
        // receives camera/layout callbacks, so inspect the resumed activity directly on main
        // instead of waiting for a quiescent camera surface.
        assertResumedActivityIdentity(AlarmCaptureActivity.class, id);
        capture("identity-" + safeId(id) + "-scanner.png");
      } finally {
        if (scanner != null) scanner.close();
      }
    }
  }

  /** Each identity must keep the real QR camera completion path able to stop a scheduled alarm. */
  @Test
  public void realQrMissionStopsScheduledAlarmForEachIdentity() throws Exception {
    grantCamera();
    String fixture =
        InstrumentationRegistry.getArguments()
            .getString("barcodeFixtureValue", "DESPERTA-ACORDAR-2026");
    String[] ids = IdentityBridge.ids();
    for (int i = 0; i < ids.length; i++) {
      String id = ids[i];
      IdentityBridge.set(context, id);
      Alarm alarm = new Alarm();
      alarm.id = 68010 + i;
      alarm.enabled = true;
      alarm.days = 0;
      alarm.snoozeLimit = 0;
      alarm.missions.add(new Alarm.Mission("barcode", fixture, 1));
      Store.save(context, alarm);
      int historyBefore = Store.history(context).length();
      try (ActivityScenario<MainActivity> launcher = ActivityScenario.launch(MainActivity.class)) {
        launcher.onActivity(activity -> AlarmService.start(activity, alarm.id, false));
        long deadline = System.currentTimeMillis() + 30_000L;
        while ((Store.history(context).length() <= historyBefore
                || Store.getSession(context) != null)
            && System.currentTimeMillis() < deadline) {
          Thread.sleep(100L);
        }
        assertTrue(
            "QR mission did not finish for " + id, Store.history(context).length() > historyBefore);
        assertEquals(null, Store.getSession(context));
        assertFalse(Store.get(context, alarm.id).enabled);
      } finally {
        context.stopService(new Intent(context, AlarmService.class));
        Scheduler.cancel(context, alarm.id);
        Store.delete(context, alarm.id);
      }
    }
  }

  /** The updater is a full identity-aware surface, including its action controls and font tree. */
  @Test
  public void updaterSurfaceUsesEachIdentity() throws Exception {
    Store.prefs(context).edit().putBoolean(UpdateChecker.KEY_AUTO_CHECK, false).commit();
    for (String id : IdentityBridge.ids()) {
      IdentityBridge.set(context, id);
      try (ActivityScenario<UpdateActivity> updater =
          ActivityScenario.launch(UpdateActivity.class)) {
        ui.waitForIdle();
        assertTrue(ui.findObject(new UiSelector().text("Atualizações")).waitForExists(5000));
        updater.onActivity(activity -> assertWindowIdentity(activity, id));
        capture("identity-" + safeId(id) + "-updater.png");
      }
    }
  }

  private void grantCamera() throws Exception {
    try (ParcelFileDescriptor command =
        InstrumentationRegistry.getInstrumentation()
            .getUiAutomation()
            .executeShellCommand(
                "pm grant " + context.getPackageName() + " android.permission.CAMERA")) {
      new FileInputStream(command.getFileDescriptor()).readAllBytes();
    }
  }

  private void seedRepresentativeAlarms() {
    Alarm work = new Alarm();
    work.label = "Trabalho";
    work.hour = 7;
    work.minute = 30;
    work.days = 62;
    work.enabled = true;
    work.missions.add(new Alarm.Mission("barcode", BARCODE, 1));
    Store.save(context, work);

    Alarm weekend = new Alarm();
    weekend.label = "Fim de semana";
    weekend.hour = 9;
    weekend.minute = 0;
    weekend.days = 65;
    weekend.enabled = false;
    weekend.missions.add(new Alarm.Mission("math", "2+2", 1));
    Store.save(context, weekend);
  }

  private Alarm findAlarm(String label) {
    for (Alarm alarm : Store.all(context)) if (label.equals(alarm.label)) return alarm;
    return null;
  }

  private Rect digitalClockBounds(String expectedText) {
    AtomicReference<Rect> result = new AtomicReference<>();
    main.onActivity(
        activity -> {
          HomeArtwork.DigitalTime clock = findDigitalTime(activity.getWindow().getDecorView());
          assertNotNull("nineties home must contain a live DigitalTime", clock);
          assertEquals(expectedText, clock.getText().toString());
          Rect bounds = new Rect();
          assertTrue(
              "DigitalTime must have visible screen bounds", clock.getGlobalVisibleRect(bounds));
          result.set(bounds);
        });
    assertNotNull("DigitalTime bounds were not captured", result.get());
    return result.get();
  }

  private HomeArtwork.DigitalTime findDigitalTime(View view) {
    if (view instanceof HomeArtwork.DigitalTime) return (HomeArtwork.DigitalTime) view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        HomeArtwork.DigitalTime found = findDigitalTime(group.getChildAt(i));
        if (found != null) return found;
      }
    }
    return null;
  }

  private Bitmap captureBitmap(String name) {
    File destination = new File(context.getExternalFilesDir(null), name);
    assertTrue("screenshot failed: " + name, ui.takeScreenshot(destination));
    Bitmap bitmap = BitmapFactory.decodeFile(destination.getAbsolutePath());
    assertNotNull("screenshot could not be decoded: " + name, bitmap);
    assertTrue(bitmap.getWidth() > 0 && bitmap.getHeight() > 0);
    return bitmap;
  }

  private long digitalInkSignature(Bitmap bitmap, Rect bounds) {
    Rect clipped = new Rect(bounds);
    assertTrue(
        "DigitalTime bounds must intersect the screenshot",
        clipped.intersect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
    long signature = 17L;
    int inkPixels = 0;
    for (int y = clipped.top; y < clipped.bottom; y++) {
      for (int x = clipped.left; x < clipped.right; x++) {
        int pixel = bitmap.getPixel(x, y);
        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);
        // The seven-segment face is teal; the inset itself is navy. Allow antialiasing while
        // excluding the background and the surrounding identity artwork.
        if (red < 80 && green >= 80 && blue >= 80 && green > red + 50 && blue > red + 50) {
          inkPixels++;
          signature = signature * 31L + (((long) y << 32) ^ (x & 0xffffffffL));
        }
      }
    }
    assertTrue("DigitalTime rendered no visible teal segments", inkPixels > 20);
    return signature * 31L + inkPixels;
  }

  private void waitForAlarmEnabled(int id, boolean expected) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 3000L;
    Alarm current;
    do {
      current = Store.get(context, id);
      if (current != null && current.enabled == expected) return;
      Thread.sleep(50L);
    } while (System.currentTimeMillis() < deadline);
    assertNotNull("alarm disappeared while toggling", current);
    assertEquals(expected, current.enabled);
  }

  private void stopAndClear() {
    context.stopService(new Intent(context, AlarmService.class));
    for (Alarm alarm : Store.all(context)) Scheduler.cancel(context, alarm.id);
    Store.prefs(context).edit().remove(Store.ALARMS_KEY).remove(Store.ACTIVE_SESSION_KEY).commit();
  }

  private void clickText(String text) throws Exception {
    UiObject object =
        ui.findObject(new UiSelector().textMatches("(?iu)" + java.util.regex.Pattern.quote(text)));
    if (!object.exists()) {
      UiSelector selector =
          new UiSelector().textMatches("(?iu)" + java.util.regex.Pattern.quote(text));
      new UiScrollable(new UiSelector().className("android.widget.ScrollView").scrollable(true))
          .scrollIntoView(selector);
    }
    assertTrue("Missing UI text: " + text, object.waitForExists(4000));
    object.click();
    ui.waitForIdle();
  }

  private boolean findTextContains(String text, long timeoutMs) {
    return ui.wait(Until.hasObject(By.textContains(text)), timeoutMs);
  }

  private void clickTextContains(String text) throws Exception {
    UiObject object = ui.findObject(new UiSelector().textContains(text));
    if (!object.exists()) {
      new UiScrollable(new UiSelector().className("android.widget.ScrollView").scrollable(true))
          .scrollIntoView(new UiSelector().textContains(text));
    }
    assertTrue("Missing UI text containing: " + text, object.waitForExists(4000));
    object.click();
    ui.waitForIdle();
  }

  private void clickDescription(String description) throws Exception {
    UiObject object = ui.findObject(new UiSelector().descriptionStartsWith(description));
    if (!object.exists()) {
      new UiScrollable(new UiSelector().className("android.widget.ScrollView").scrollable(true))
          .scrollIntoView(new UiSelector().descriptionStartsWith(description));
    }
    assertTrue("Missing UI description: " + description, object.waitForExists(4000));
    object.click();
    ui.waitForIdle();
  }

  private void clickOptional(String... labels) throws Exception {
    for (String label : labels) {
      UiObject object = ui.findObject(new UiSelector().text(label));
      if (object.exists() && object.isEnabled()) {
        object.click();
        ui.waitForIdle();
        return;
      }
    }
  }

  private void capture(String name) {
    File destination = new File(context.getExternalFilesDir(null), name);
    assertTrue("screenshot failed: " + name, ui.takeScreenshot(destination));
    assertTrue(destination.exists());
    Bitmap bitmap = BitmapFactory.decodeFile(destination.getAbsolutePath());
    assertNotNull("screenshot could not be decoded: " + name, bitmap);
    assertTrue(bitmap.getWidth() > 0 && bitmap.getHeight() > 0);
    bitmap.recycle();
  }

  private String safeId(String id) {
    return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
  }

  private void assertWindowIdentity(Activity activity, String id) {
    Identity expected = Identity.current(context);
    assertEquals(id, expected.id);
    // Android 15 enforces edge-to-edge for targetSdk 35 and reports transparent system-bar
    // colors. The app's identity is drawn beneath those bars; exact color assertions remain
    // meaningful on API levels where the window owns the bar color.
    if (Build.VERSION.SDK_INT < 35) {
      assertEquals(
          "status bar must use the selected identity",
          expected.bg,
          activity.getWindow().getStatusBarColor());
      assertEquals(
          "navigation bar must use the selected identity",
          expected.bg,
          activity.getWindow().getNavigationBarColor());
    }
    assertTrue(
        "selected identity body font was not applied",
        containsTypeface(activity.getWindow().getDecorView(), expected.bodyFont));
    if (containsButton(activity.getWindow().getDecorView())) {
      assertTrue(
          "selected identity display font was not applied",
          containsTypeface(activity.getWindow().getDecorView(), expected.displayFont));
    }
  }

  private void assertResumedActivityIdentity(Class<? extends Activity> type, String id) {
    AtomicReference<Activity> resumed = new AtomicReference<>();
    AtomicReference<AssertionError> failure = new AtomicReference<>();
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              for (Activity candidate :
                  ActivityLifecycleMonitorRegistry.getInstance()
                      .getActivitiesInStage(Stage.RESUMED)) {
                if (type.isInstance(candidate)) {
                  resumed.set(candidate);
                  break;
                }
              }
              Activity activity = resumed.get();
              if (activity == null) {
                failure.set(new AssertionError("No resumed " + type.getSimpleName()));
                return;
              }
              try {
                assertWindowIdentity(activity, id);
              } catch (AssertionError error) {
                failure.set(error);
              }
            });
    if (failure.get() != null) throw failure.get();
    assertNotNull("No resumed " + type.getSimpleName(), resumed.get());
  }

  private boolean containsTypeface(View view, android.graphics.Typeface expected) {
    if (view instanceof TextView) {
      android.graphics.Typeface actual = ((TextView) view).getTypeface();
      if (actual != null
          && expected != null
          && (actual == expected
              || actual.equals(expected)
              || actual.equals(android.graphics.Typeface.create(expected, actual.getStyle())))) {
        return true;
      }
    }
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        if (containsTypeface(group.getChildAt(i), expected)) return true;
      }
    }
    return false;
  }

  private boolean containsButton(View view) {
    if (view instanceof Button) return true;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        if (containsButton(group.getChildAt(i))) return true;
      }
    }
    return false;
  }

  private boolean isRetro(String id) {
    return id != null && id.toLowerCase(Locale.ROOT).contains("retro");
  }

  private boolean containsRetro(String[] ids, String[] names) {
    for (String id : ids) if (isRetro(id)) return true;
    for (String name : names) if (isRetro(name)) return true;
    return false;
  }

  private String retroId() {
    for (String id : IdentityBridge.ids()) if (isRetro(id)) return id;
    return Identity.RETRO;
  }

  static final class IdentityBridge {
    static String[] ids() {
      return Identity.IDS.clone();
    }

    static String[] names() {
      return Identity.NAMES.clone();
    }

    static String currentId(Context context) {
      return Identity.current(context).id;
    }

    static void set(Context context, String id) {
      Identity.set(context, id);
    }

    static String paletteSignature(Context context) {
      Identity current = Identity.current(context);
      StringBuilder signature = new StringBuilder();
      signature.append("bg=").append(current.bg).append(';');
      signature.append("surface=").append(current.surface).append(';');
      signature.append("raised=").append(current.raised).append(';');
      signature.append("fg=").append(current.fg).append(';');
      signature.append("muted=").append(current.muted).append(';');
      signature.append("accent=").append(current.accent).append(';');
      signature.append("onAccent=").append(current.onAccent).append(';');
      signature.append("positive=").append(current.positive).append(';');
      signature.append("error=").append(current.error).append(';');
      assertNotNull(current.bodyFont);
      assertNotNull(current.displayFont);
      signature.append("body=").append(current.bodyFont).append(';');
      signature.append("display=").append(current.displayFont).append(';');
      return signature.toString();
    }
  }
}
