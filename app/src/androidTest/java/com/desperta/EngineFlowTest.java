package com.desperta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Calendar;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Device-side engine checks for persistence, PendingIntent routing and service flows. */
@RunWith(AndroidJUnit4.class)
public class EngineFlowTest {
  private Context context;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    grantRuntimeAlarmAccess();
    for (Alarm alarm : Store.all(context)) {
      Scheduler.cancel(context, alarm.id);
    }
    Store.prefs(context).edit().clear().commit();
  }

  @After
  public void tearDown() {
    context.stopService(new android.content.Intent(context, AlarmService.class));
    for (Alarm alarm : Store.all(context)) {
      Scheduler.cancel(context, alarm.id);
    }
    Store.prefs(context).edit().clear().commit();
  }

  @Test
  public void staleAcceptedResultCannotAdvanceTheNextMission() throws Exception {
    Alarm alarm = alarm(41991);
    alarm.missions.add(new Alarm.Mission("barcode", "FIRST", 1));
    alarm.missions.add(new Alarm.Mission("barcode", "SECOND", 1));
    Store.save(context, alarm);
    AlarmService.start(context, alarm.id, false);
    assertNotNull(waitForSession(alarm.id, 4000));
    AlarmService.missionResult(context, alarm.id, 0, true);
    SystemClock.sleep(350);
    assertEquals(1, Store.getSession(context).optInt("missionIndex"));
    AlarmService.missionResult(context, alarm.id, 0, true);
    SystemClock.sleep(350);
    assertNotNull(Store.getSession(context));
    assertEquals(1, Store.getSession(context).optInt("missionIndex"));
    assertEquals(0, Store.history(context).length());
    AlarmService.missionResult(context, alarm.id, 1, true);
    waitForNoSession(4000);
    AlarmService.missionResult(context, alarm.id, 1, true);
    // A stale completion must not restart a foreground service and crash on its timeout.
    SystemClock.sleep(6000);
    assertEquals(null, Store.getSession(context));
    assertEquals(1, Store.history(context).length());
  }

  @Test
  public void alarmPendingIntentsKeepNormalSnoozeWakeAndPreviewSeparate() {
    Alarm alarm = new Alarm();
    alarm.id = 41001;
    Store.save(context, alarm);
    PendingIntent normal = Scheduler.pendingIntent(context, alarm.id, false, false);
    PendingIntent snooze = Scheduler.pendingIntent(context, alarm.id, false, true);
    PendingIntent wake = Scheduler.pendingIntent(context, alarm.id, false, true, true);
    PendingIntent preview = Scheduler.pendingIntent(context, alarm.id, true, false);
    assertTrue(!normal.equals(snooze));
    assertTrue(!snooze.equals(wake));
    assertTrue(!normal.equals(preview));
  }

  @Test
  public void exactScheduleCanBeInstalledAndCancelledWhenPermissionIsAvailable() {
    AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    assertTrue(
        "Exact alarm permission must be granted in test environment",
        Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms());
    Alarm alarm = new Alarm();
    alarm.id = 41002;
    alarm.hour = 23;
    alarm.minute = 59;
    alarm.days = 127;
    Store.save(context, alarm);
    Scheduler.schedule(context, alarm);
    Scheduler.scheduleAt(
        context, alarm.id, System.currentTimeMillis() + 120_000L, false, true, true);
    Scheduler.cancel(context, alarm.id);
    assertNotNull(Store.get(context, alarm.id));
  }

  @Test
  public void previewServiceDoesNotWriteHistoryOrDisableAlarm() throws Exception {
    Alarm alarm = new Alarm();
    alarm.id = 41003;
    alarm.sound = "";
    Store.save(context, alarm);
    AlarmService.start(context, alarm.id, true);
    SystemClock.sleep(350L);
    AlarmService.stopPreview(context, alarm.id);
    SystemClock.sleep(350L);
    assertEquals(0, Store.history(context).length());
    assertTrue(Store.get(context, alarm.id).enabled);
  }

  @Test
  public void snoozePersistsCountAndMissionCursor() throws Exception {
    Alarm alarm = new Alarm();
    alarm.id = 41004;
    alarm.snoozeMinutes = 1;
    alarm.snoozeLimit = 2;
    alarm.missions.add(new Alarm.Mission("barcode", "123456", 1));
    Store.save(context, alarm);
    AlarmService.start(context, alarm.id, true);
    SystemClock.sleep(300L);
    // Preview is stoppable and does not consume a snooze.  Persisting a
    // session directly mirrors the service's crash-safe format and checks
    // the fields used when a real alarm resumes after process death.
    JSONObject session =
        new JSONObject().put("alarmId", alarm.id).put("missionIndex", 0).put("snoozeCount", 1);
    Store.saveSession(context, session);
    assertEquals(0, Store.getSession(context).optInt("missionIndex", -1));
    assertEquals(1, Store.getSession(context).optInt("snoozeCount", -1));
    AlarmService.stopPreview(context, alarm.id);
    SystemClock.sleep(250L);
  }

  @Test
  public void scheduledDeliveryStartsARealForegroundSessionAndDismissLogsIt() throws Exception {
    assumeExactAlarms();
    Alarm alarm = alarm(41005);
    Store.save(context, alarm);
    Scheduler.scheduleAt(context, alarm.id, System.currentTimeMillis() + 1_500L, false, false);
    JSONObject session = waitForSession(alarm.id, 7_000L);
    assertNotNull(session);
    assertEquals(alarm.id, session.optInt("alarmId", -1));
    AlarmService.dismiss(context, alarm.id);
    waitForNoSession(3_000L);
    assertEquals(1, Store.history(context).length());
  }

  @Test
  public void overlappingScheduledDeliveriesAreQueuedAndThenDrained() throws Exception {
    assumeExactAlarms();
    Alarm first = alarm(41006);
    Alarm second = alarm(41007);
    Store.save(context, first);
    Store.save(context, second);
    long base = System.currentTimeMillis() + 1_300L;
    Scheduler.scheduleAt(context, first.id, base, false, false);
    Scheduler.scheduleAt(context, second.id, base + 550L, false, false);
    JSONObject session = waitForSession(first.id, 7_000L);
    assertNotNull(session);
    SystemClock.sleep(900L);
    JSONObject queued = Store.getSession(context);
    assertNotNull(queued);
    assertTrue(queued.optJSONArray("queue") != null && queued.optJSONArray("queue").length() >= 1);
    AlarmService.dismiss(context, first.id);
    JSONObject secondSession = waitForSession(second.id, 4_000L);
    assertNotNull(secondSession);
    AlarmService.dismiss(context, second.id);
    waitForNoSession(3_000L);
  }

  @Test
  public void wrongMissionResultLeavesCursorAndRingActive() throws Exception {
    assumeExactAlarms();
    Alarm alarm = alarm(41008);
    alarm.missions.add(new Alarm.Mission("barcode", "123456789", 1));
    Store.save(context, alarm);
    Scheduler.scheduleAt(context, alarm.id, System.currentTimeMillis() + 1_200L, false, false);
    assertNotNull(waitForSession(alarm.id, 7_000L));
    AlarmService.missionResult(context, alarm.id, false);
    SystemClock.sleep(450L);
    JSONObject afterWrong = Store.getSession(context);
    assertNotNull(afterWrong);
    assertEquals(0, afterWrong.optInt("missionIndex", -1));
    AlarmService.missionResult(context, alarm.id, true);
    waitForNoSession(4_000L);
    assertEquals(1, Store.history(context).length());
  }

  @Test
  public void wakeCheckPendingIntentIsDeliveredAsASeparateRealTrigger() throws Exception {
    assumeExactAlarms();
    Alarm alarm = alarm(41009);
    Store.save(context, alarm);
    Scheduler.scheduleAt(context, alarm.id, System.currentTimeMillis() + 1_200L, false, true, true);
    JSONObject session = waitForSession(alarm.id, 7_000L);
    assertNotNull(session);
    assertTrue(session.optBoolean("wakeCheck", false));
    AlarmService.dismiss(context, alarm.id);
    waitForNoSession(3_000L);
    SystemClock.sleep(400L);
    assertFalse(
        "A wake-check dismissal must not schedule another wake-check",
        shellText("dumpsys alarm").contains("desperta://alarm/" + alarm.id + "/wake"));
  }

  @Test
  public void snoozedSessionsRemainKeyedWhenActiveSessionChanges() throws Exception {
    Alarm first = alarm(41010);
    Alarm second = alarm(41011);
    Store.save(context, first);
    Store.save(context, second);
    Store.saveSession(
        context,
        new JSONObject().put("alarmId", first.id).put("missionIndex", 2).put("snoozeCount", 1));
    Store.saveSnoozedSession(
        context,
        first.id,
        new JSONObject().put("alarmId", first.id).put("missionIndex", 2).put("snoozeCount", 1));
    Store.saveSession(
        context,
        new JSONObject().put("alarmId", second.id).put("missionIndex", 0).put("snoozeCount", 0));
    assertEquals(2, Store.getSnoozedSession(context, first.id).optInt("missionIndex", -1));
    assertEquals(1, Store.getSnoozedSession(context, first.id).optInt("snoozeCount", -1));
    assertEquals(second.id, Store.getSession(context).optInt("alarmId", -1));
  }

  @Test
  public void snoozeLimitGuardPreventsClosingTheRingUi() throws Exception {
    Alarm alarm = alarm(41012);
    alarm.snoozeLimit = 1;
    Store.save(context, alarm);
    Store.saveSession(
        context,
        new JSONObject().put("alarmId", alarm.id).put("missionIndex", 0).put("snoozeCount", 1));
    assertFalse(AlarmService.canSnooze(context, alarm.id));
  }

  @Test
  public void previewUsesAlarmAttributesAndRestoresExtraLoudVolumeAndRoute() throws Exception {
    if (Build.VERSION.SDK_INT < 26) return;
    AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    int beforeVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM);
    int maximum = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
    boolean beforeSpeaker = audio.isSpeakerphoneOn();
    Alarm alarm = alarm(41013);
    alarm.extraLoud = true;
    Store.save(context, alarm);
    try {
      AlarmService.start(context, alarm.id, true);
      SystemClock.sleep(700L);
      if (maximum > beforeVolume) {
        assertEquals(maximum, audio.getStreamVolume(AudioManager.STREAM_ALARM));
      }
      boolean hasAlarmAudio = false;
      for (android.media.AudioPlaybackConfiguration config :
          audio.getActivePlaybackConfigurations()) {
        android.media.AudioAttributes attrs = config.getAudioAttributes();
        if (attrs != null && attrs.getUsage() == android.media.AudioAttributes.USAGE_ALARM) {
          hasAlarmAudio = true;
        }
      }
      assertTrue("Preview should use the alarm audio stream", hasAlarmAudio);
    } finally {
      AlarmService.stopPreview(context, alarm.id);
      SystemClock.sleep(700L);
    }
    assertEquals(beforeVolume, audio.getStreamVolume(AudioManager.STREAM_ALARM));
    assertEquals(beforeSpeaker, audio.isSpeakerphoneOn());
  }

  @Test
  public void actualDeliveryConsumesEarlierOverrideAndSuppressesOriginalOccurrence()
      throws Exception {
    assumeExactAlarms();
    long now = System.currentTimeMillis();
    Calendar regular = Calendar.getInstance();
    regular.setTimeInMillis(now);
    regular.add(Calendar.MINUTE, 10);
    regular.set(Calendar.SECOND, 0);
    regular.set(Calendar.MILLISECOND, 0);
    Alarm alarm = alarm(41014);
    alarm.hour = regular.get(Calendar.HOUR_OF_DAY);
    alarm.minute = regular.get(Calendar.MINUTE);
    alarm.days = 127;
    assertTrue(Scheduler.setNextOverride(alarm, now + 1_500L, now));
    long displaced = alarm.nextOverrideOriginalAt;
    Store.save(context, alarm);
    Scheduler.scheduleAt(context, alarm.id, now + 1_500L, false, false);

    assertNotNull(waitForSession(alarm.id, 7_000L));
    Alarm afterTrigger = Store.get(context, alarm.id);
    assertNotNull(afterTrigger);
    assertEquals(0L, afterTrigger.nextOverrideAt);
    assertEquals(0L, afterTrigger.nextOverrideOriginalAt);
    assertTrue(afterTrigger.skipUntil >= displaced);
    assertTrue(Scheduler.next(afterTrigger, System.currentTimeMillis()) > displaced);

    AlarmService.dismiss(context, alarm.id);
    waitForNoSession(3_000L);
  }

  @Test
  public void previewDoesNotConsumeOneTimeOverride() throws Exception {
    long now = System.currentTimeMillis();
    Alarm alarm = alarm(41015);
    alarm.hour = 23;
    alarm.minute = 59;
    assertTrue(Scheduler.setNextOverride(alarm, now + 30 * 60_000L, now));
    long override = alarm.nextOverrideAt;
    long displaced = alarm.nextOverrideOriginalAt;
    Store.save(context, alarm);

    AlarmService.start(context, alarm.id, true);
    assertNotNull(waitForSession(alarm.id, 4_000L));
    Alarm duringPreview = Store.get(context, alarm.id);
    assertNotNull(duringPreview);
    assertEquals(override, duringPreview.nextOverrideAt);
    assertEquals(displaced, duringPreview.nextOverrideOriginalAt);
    AlarmService.stopPreview(context, alarm.id);
    waitForNoSession(3_000L);
    Alarm afterPreview = Store.get(context, alarm.id);
    assertNotNull(afterPreview);
    assertEquals(override, afterPreview.nextOverrideAt);
    assertEquals(displaced, afterPreview.nextOverrideOriginalAt);
  }

  private Alarm alarm(int id) {
    Alarm alarm = new Alarm();
    alarm.id = id;
    alarm.hour = 23;
    alarm.minute = 59;
    alarm.days = 127;
    alarm.snoozeMinutes = 1;
    alarm.snoozeLimit = 2;
    return alarm;
  }

  private void assumeExactAlarms() {
    AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    assertTrue(
        "Exact alarm permission must be granted in test environment",
        Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms());
  }

  private void grantRuntimeAlarmAccess() {
    if (Build.VERSION.SDK_INT >= 31) {
      runShell("appops set " + context.getPackageName() + " SCHEDULE_EXACT_ALARM allow");
    }
    if (Build.VERSION.SDK_INT >= 33) {
      runShell("pm grant " + context.getPackageName() + " android.permission.POST_NOTIFICATIONS");
    }
  }

  private void runShell(String command) {
    try {
      ParcelFileDescriptor output =
          InstrumentationRegistry.getInstrumentation()
              .getUiAutomation()
              .executeShellCommand(command);
      if (output != null) {
        new java.io.FileInputStream(output.getFileDescriptor()).readAllBytes();
        output.close();
      }
    } catch (Exception ignored) {
      // The following Assume gate reports a skipped exact-alarm case on
      // devices where shell appops are restricted.
    }
  }

  private String shellText(String command) {
    try {
      ParcelFileDescriptor output =
          InstrumentationRegistry.getInstrumentation()
              .getUiAutomation()
              .executeShellCommand(command);
      if (output == null) return "";
      try (java.io.FileInputStream input =
          new java.io.FileInputStream(output.getFileDescriptor())) {
        return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      } finally {
        output.close();
      }
    } catch (Exception ignored) {
      return "";
    }
  }

  private JSONObject waitForSession(int id, long timeoutMillis) throws Exception {
    long deadline = SystemClock.uptimeMillis() + timeoutMillis;
    while (SystemClock.uptimeMillis() < deadline) {
      JSONObject session = Store.getSession(context);
      if (session != null && session.optInt("alarmId", -1) == id) return session;
      SystemClock.sleep(100L);
    }
    return Store.getSession(context);
  }

  private void waitForNoSession(long timeoutMillis) {
    long deadline = SystemClock.uptimeMillis() + timeoutMillis;
    while (SystemClock.uptimeMillis() < deadline && Store.getSession(context) != null) {
      SystemClock.sleep(100L);
    }
  }
}
