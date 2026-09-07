package com.desperta;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Foreground owner of alarm playback and the active wake-up session.
 *
 * <p>RingActivity is intentionally a thin UI. This service keeps the alarm audible while the
 * activity is covered by a mission, persists the mission cursor/snooze count, and serializes
 * overlapping alarm deliveries.
 */
public class AlarmService extends Service {
  public static final String ACTION_TRIGGER = "com.desperta.action.TRIGGER";
  public static final String ACTION_DISMISS = "com.desperta.action.DISMISS";
  public static final String ACTION_SNOOZE = "com.desperta.action.SNOOZE";
  public static final String ACTION_STOP_PREVIEW = "com.desperta.action.STOP_PREVIEW";
  public static final String ACTION_MISSION_RESULT = "com.desperta.action.MISSION_RESULT";
  public static final String ACTION_STOP = "com.desperta.action.STOP";
  public static final String ACTION_MISSION_ADVANCED = "com.desperta.action.MISSION_ADVANCED";
  public static final String EXTRA_MISSION_SUCCESS = "mission_success";
  public static final String EXTRA_MISSION_INDEX = "mission_index";
  public static final String EXTRA_MISSION_DONE = "mission_done";

  private static final String CHANNEL_ID = "alarm_ringing";
  private static final int NOTIFICATION_ID = 0x44455350;
  private static final long WAKE_LOCK_MS = 15L * 60L * 1000L;

  private final ArrayDeque<RingRequest> waiting = new ArrayDeque<>();
  private Handler main;
  private MediaPlayer player;
  private Vibrator vibrator;
  private TextToSpeech speech;
  private PowerManager.WakeLock wakeLock;
  private Alarm activeAlarm;
  private int activeId = -1;
  private int missionIndex;
  private int snoozeCount;
  private boolean activePreview;
  private boolean activeSnooze;
  private boolean activeWakeCheck;
  private Runnable volumeRamp;
  private AudioManager routedAudioManager;
  private int previousAudioMode;
  private boolean previousSpeakerphone;
  private boolean audioRouteCaptured;
  private AudioManager alarmVolumeManager;
  private int previousAlarmVolume = -1;
  private boolean alarmVolumeRaised;

  private static final class RingRequest {
    final int id;
    final boolean preview;
    final boolean snooze;
    final boolean wakeCheck;

    RingRequest(int id, boolean preview, boolean snooze, boolean wakeCheck) {
      this.id = id;
      this.preview = preview;
      this.snooze = snooze;
      this.wakeCheck = wakeCheck;
    }
  }

  /** Public UI entry point used by MainActivity, RingActivity and tests. */
  public static void start(Context context, int alarmId, boolean preview) {
    start(context, alarmId, preview, false, false);
  }

  public static void start(
      Context context, int alarmId, boolean preview, boolean snooze, boolean wakeCheck) {
    if (context == null || alarmId <= 0) {
      return;
    }
    Intent intent =
        new Intent(context, AlarmService.class)
            .setAction(ACTION_TRIGGER)
            .putExtra(Scheduler.EXTRA_ALARM_ID, alarmId)
            .putExtra(Scheduler.EXTRA_PREVIEW, preview)
            .putExtra(Scheduler.EXTRA_SNOOZE, snooze)
            .putExtra(Scheduler.EXTRA_WAKE_CHECK, wakeCheck);
    startServiceCompat(context.getApplicationContext(), intent);
  }

  public static void dismiss(Context context, int alarmId) {
    command(context, ACTION_DISMISS, alarmId, false);
  }

  public static void snooze(Context context, int alarmId) {
    command(context, ACTION_SNOOZE, alarmId, false);
  }

  /** Synchronous guard used by RingActivity before it closes its UI. */
  public static boolean canSnooze(Context context, int alarmId) {
    if (context == null || alarmId <= 0) {
      return false;
    }
    Alarm alarm = Store.get(context, alarmId);
    if (alarm == null || alarm.snoozeLimit <= 0) {
      return false;
    }
    JSONObject session = Store.getSession(context);
    int count =
        session != null && session.optInt("alarmId", -1) == alarmId
            ? Math.max(0, session.optInt("snoozeCount", 0))
            : 0;
    return count < alarm.snoozeLimit;
  }

  public static void stopPreview(Context context, int alarmId) {
    command(context, ACTION_STOP_PREVIEW, alarmId, true);
  }

  public static void missionResult(Context context, int alarmId, boolean success) {
    missionResult(context, alarmId, -1, success);
  }

  /**
   * Delivers a mission result together with the cursor value observed by the mission screen. A
   * delayed callback from an older screen must never advance a newer mission.
   */
  public static void missionResult(
      Context context, int alarmId, int expectedMissionIndex, boolean success) {
    if (context == null || alarmId <= 0) {
      return;
    }
    Intent intent =
        new Intent(context, AlarmService.class)
            .setAction(ACTION_MISSION_RESULT)
            .putExtra(Scheduler.EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_MISSION_INDEX, expectedMissionIndex)
            .putExtra(EXTRA_MISSION_SUCCESS, success);
    startServiceCompat(context.getApplicationContext(), intent);
  }

  private static void command(Context context, String action, int alarmId, boolean preview) {
    if (context == null || alarmId <= 0) {
      return;
    }
    Intent intent =
        new Intent(context, AlarmService.class)
            .setAction(action)
            .putExtra(Scheduler.EXTRA_ALARM_ID, alarmId)
            .putExtra(Scheduler.EXTRA_PREVIEW, preview);
    startServiceCompat(context.getApplicationContext(), intent);
  }

  private static void startServiceCompat(Context context, Intent intent) {
    try {
      if (Build.VERSION.SDK_INT >= 26 && ACTION_TRIGGER.equals(intent.getAction())) {
        context.startForegroundService(intent);
      } else {
        context.startService(intent);
      }
    } catch (RuntimeException ignored) {
      // A stopped package or an OEM background restriction can reject a
      // command.  AlarmManager will retry the next configured occurrence.
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    main = new Handler(Looper.getMainLooper());
    vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    createNotificationChannel();
    restoreSession();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) {
      if (activeId > 0 && activeAlarm != null) {
        startRing(activeAlarm, activePreview, activeSnooze, activeWakeCheck);
      } else {
        stopSelfResult(startId);
      }
      return START_STICKY;
    }
    String action = intent.getAction();
    // Control messages never create a new ringing session. A delayed camera result
    // after completion must not leave an idle foreground-service start outstanding.
    if (!ACTION_TRIGGER.equals(action) && activeAlarm == null) {
      stopSelfResult(startId);
      return START_NOT_STICKY;
    }
    if (ACTION_DISMISS.equals(action)) {
      finishActive(intent.getIntExtra(Scheduler.EXTRA_ALARM_ID, activeId), "dismissed", false);
    } else if (ACTION_SNOOZE.equals(action)) {
      handleSnooze(intent.getIntExtra(Scheduler.EXTRA_ALARM_ID, activeId));
    } else if (ACTION_STOP_PREVIEW.equals(action)) {
      stopPreviewInternal(intent.getIntExtra(Scheduler.EXTRA_ALARM_ID, activeId));
    } else if (ACTION_MISSION_RESULT.equals(action)) {
      handleMissionResult(
          intent.getIntExtra(Scheduler.EXTRA_ALARM_ID, activeId),
          intent.getBooleanExtra(EXTRA_MISSION_SUCCESS, false),
          intent.getIntExtra(EXTRA_MISSION_INDEX, -1));
    } else if (ACTION_STOP.equals(action)) {
      finishActive(intent.getIntExtra(Scheduler.EXTRA_ALARM_ID, activeId), "stopped", true);
    } else {
      int id = intent.getIntExtra(Scheduler.EXTRA_ALARM_ID, -1);
      if (id > 0) {
        enqueueOrStart(
            new RingRequest(
                id,
                intent.getBooleanExtra(Scheduler.EXTRA_PREVIEW, false),
                intent.getBooleanExtra(Scheduler.EXTRA_SNOOZE, false),
                intent.getBooleanExtra(Scheduler.EXTRA_WAKE_CHECK, false)));
      }
    }
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    if (main != null && volumeRamp != null) {
      main.removeCallbacks(volumeRamp);
    }
    stopPlayback();
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void enqueueOrStart(RingRequest request) {
    Alarm alarm = Store.get(this, request.id);
    if (alarm == null) {
      return;
    }
    if (!request.preview && !alarm.enabled && !request.snooze && !request.wakeCheck) {
      return;
    }
    if (activeId > 0) {
      // A process restart restores the persisted session before the
      // snooze/wake-check PendingIntent arrives.  No player means that
      // restored session is not currently ringing yet, so resume it.
      if (activeId == request.id && activePreview == request.preview && player == null) {
        restoreDelayedState(request.id, request.snooze || request.wakeCheck);
        activeSnooze = request.snooze;
        activeWakeCheck = request.wakeCheck;
        startRing(alarm, request.preview, request.snooze, request.wakeCheck);
        return;
      }
      if (activeId != request.id || activePreview != request.preview) {
        waiting.addLast(request);
        saveSession();
      }
      return;
    }
    boolean realTrigger = !request.preview && !request.snooze && !request.wakeCheck;
    if (realTrigger) {
      // The one-time editor changes only the upcoming AlarmManager delivery. Consume that marker
      // at the real trigger, before re-arming the weekly schedule, so a delayed delivery cannot
      // schedule the temporary timestamp a second time. Preview, snooze, and wake-check sessions
      // intentionally leave the marker alone.
      if (Scheduler.consumeNextOverride(alarm, System.currentTimeMillis())) {
        Store.save(this, alarm);
      }
    }
    if (realTrigger) {
      // AlarmManager entries are one-shot.  Re-arm the repeating alarm
      // before starting media so a long mission cannot lose tomorrow's
      // delivery.  One-shot alarms are disabled after dismissal.
      if (alarm.days == 0) {
        Scheduler.cancel(this, alarm.id);
      } else {
        try {
          Scheduler.schedule(this, alarm);
        } catch (SecurityException ignored) {
          // Playback remains active; the Settings screen can grant
          // exact-alarm access and the next boot/time event retries.
        }
      }
    }
    if (request.snooze || request.wakeCheck) {
      restoreDelayedState(request.id, true);
    } else {
      missionIndex = 0;
      snoozeCount = 0;
    }
    activeAlarm = alarm;
    activeId = alarm.id;
    activePreview = request.preview;
    activeSnooze = request.snooze;
    activeWakeCheck = request.wakeCheck;
    saveSession();
    startRing(alarm, request.preview, request.snooze, request.wakeCheck);
  }

  private void startRing(Alarm alarm, boolean preview, boolean snooze, boolean wakeCheck) {
    activeAlarm = alarm;
    activeId = alarm.id;
    activePreview = preview;
    activeSnooze = snooze;
    activeWakeCheck = wakeCheck;
    PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
    android.app.KeyguardManager keyguard = (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
    boolean useFullScreenNotification = (power != null && !power.isInteractive())
        || (keyguard != null && keyguard.isKeyguardLocked());
    startForeground(NOTIFICATION_ID, buildNotification(alarm, preview));
    acquireWakeLock();
    configureAudioRoute();
    startPlayback(alarm);
    startVibration(alarm);
    speakReminders(alarm);
    // On the lock screen Android launches the notification's full-screen intent.
    // Launching it again here creates a second RingActivity over the active scanner.
    if (!useFullScreenNotification) launchRingActivity(alarm, preview);
  }

  private void launchRingActivity(Alarm alarm, boolean preview) {
    Intent ui =
        new Intent()
            .setClassName(this, getPackageName() + ".RingActivity")
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Scheduler.EXTRA_ALARM_ID, alarm.id)
            .putExtra(Scheduler.EXTRA_PREVIEW, preview);
    try {
      startActivity(ui);
    } catch (RuntimeException ignored) {
      // The notification still carries a full-screen intent.  This path
      // is used on devices that temporarily block activity launches.
    }
  }

  private void handleMissionResult(int alarmId, boolean success, int expectedMissionIndex) {
    if (alarmId != activeId || activeAlarm == null || activePreview) {
      return;
    }
    if (expectedMissionIndex >= 0 && expectedMissionIndex != missionIndex) {
      // The Activity may have been recreated or delivered the same scanner callback twice after
      // the cursor moved. Keep the active session at its current mission.
      return;
    }
    if (success) {
      if (missionIndex < activeAlarm.missions.size()) {
        missionIndex++;
        saveSession();
      }
      boolean done = missionIndex >= activeAlarm.missions.size();
      sendMissionAdvanced(alarmId, true, missionIndex, done);
      if (done) {
        finishActive(alarmId, "completed", false);
      }
    } else {
      // Wrong answers and cancellation leave the alarm ringing and the
      // mission cursor untouched.
      sendMissionAdvanced(alarmId, false, missionIndex, false);
    }
  }

  private void sendMissionAdvanced(int alarmId, boolean success, int next, boolean done) {
    Intent update =
        new Intent(ACTION_MISSION_ADVANCED)
            .setPackage(getPackageName())
            .putExtra(Scheduler.EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_MISSION_SUCCESS, success)
            .putExtra(EXTRA_MISSION_INDEX, next)
            .putExtra(EXTRA_MISSION_DONE, done);
    sendBroadcast(update);
  }

  private void handleSnooze(int alarmId) {
    if (alarmId != activeId || activeAlarm == null) {
      return;
    }
    if (activePreview) {
      stopPreviewInternal(alarmId);
      return;
    }
    int limit = Math.max(0, activeAlarm.snoozeLimit);
    if (snoozeCount >= limit) {
      return;
    }
    snoozeCount++;
    saveSession();
    long minutes = Math.max(1, activeAlarm.snoozeMinutes);
    try {
      Scheduler.scheduleAt(
          this, activeAlarm.id, System.currentTimeMillis() + minutes * 60_000L, false, true);
    } catch (SecurityException ignored) {
      // Keep the current ring and cursor intact.  Closing the service
      // here would leave the user with no alarm and no pending retry.
      snoozeCount--;
      saveSession();
      return;
    }
    JSONObject delayed = new JSONObject();
    try {
      delayed
          .put("alarmId", activeId)
          .put("preview", false)
          .put("snooze", true)
          .put("wakeCheck", false)
          .put("missionIndex", missionIndex)
          .put("snoozeCount", snoozeCount);
      Store.saveSnoozedSession(this, activeId, delayed);
    } catch (JSONException ignored) {
      // All values are primitive; retain the active ring if persistence
      // somehow fails rather than silently losing the delayed session.
      snoozeCount--;
      saveSession();
      Scheduler.cancel(this, activeId);
      return;
    }
    Store.clearSession(this);
    stopPlayback();
    clearActiveForQueue(false);
  }

  private void restoreDelayedState(int alarmId, boolean delayed) {
    if (!delayed) {
      return;
    }
    JSONObject saved = Store.getSnoozedSession(this, alarmId);
    if (saved == null) {
      saved = Store.getSession(this);
      if (saved != null && saved.optInt("alarmId", -1) != alarmId) {
        saved = null;
      }
    }
    if (saved != null) {
      missionIndex = Math.max(0, saved.optInt("missionIndex", 0));
      snoozeCount = Math.max(0, saved.optInt("snoozeCount", 0));
      Store.clearSnoozedSession(this, alarmId);
    }
  }

  private void stopPreviewInternal(int alarmId) {
    if (alarmId != activeId || !activePreview) {
      return;
    }
    finishActive(alarmId, "preview_stopped", true);
  }

  private void finishActive(int alarmId, String event, boolean noHistory) {
    if (alarmId > 0 && alarmId != activeId) {
      return;
    }
    Alarm finished = activeAlarm;
    boolean preview = activePreview;
    boolean wakeCheck = activeWakeCheck;
    stopPlayback();
    if (finished != null && !preview && !noHistory) {
      Store.logHistory(this, finished, event);
      if (("dismissed".equals(event) || "completed".equals(event)) && finished.days == 0) {
        finished.enabled = false;
        Store.save(this, finished);
        Scheduler.cancel(this, finished.id);
      }
      if (!wakeCheck
          && finished.wakeCheckMinutes > 0
          && ("dismissed".equals(event) || "completed".equals(event))) {
        try {
          Scheduler.scheduleAt(
              this,
              finished.id,
              System.currentTimeMillis() + finished.wakeCheckMinutes * 60_000L,
              false,
              true,
              true);
        } catch (SecurityException ignored) {
          // See the exact-alarm access explanation in Settings.
        }
      }
    }
    clearActiveForQueue(true);
  }

  private void clearActiveForQueue(boolean clearStoredSession) {
    activeAlarm = null;
    activeId = -1;
    activePreview = false;
    activeSnooze = false;
    activeWakeCheck = false;
    missionIndex = 0;
    snoozeCount = 0;
    if (clearStoredSession) {
      Store.clearSession(this);
    }
    RingRequest next = waiting.pollFirst();
    if (next != null) {
      saveSession();
      enqueueOrStart(next);
      return;
    }
    stopForeground(true);
    stopSelf();
  }

  private void restoreSession() {
    JSONObject saved = Store.getSession(this);
    if (saved == null) {
      return;
    }
    int id = saved.optInt("alarmId", -1);
    Alarm alarm = Store.get(this, id);
    if (alarm == null) {
      Store.clearSession(this);
      return;
    }
    activeId = id;
    activeAlarm = alarm;
    activePreview = saved.optBoolean("preview", false);
    activeSnooze = saved.optBoolean("snooze", false);
    activeWakeCheck = saved.optBoolean("wakeCheck", false);
    missionIndex = Math.max(0, saved.optInt("missionIndex", 0));
    snoozeCount = Math.max(0, saved.optInt("snoozeCount", 0));
    JSONArray queue = saved.optJSONArray("queue");
    if (queue != null) {
      for (int i = 0; i < queue.length(); i++) {
        JSONObject row = queue.optJSONObject(i);
        if (row != null && row.optInt("id", -1) > 0) {
          waiting.addLast(
              new RingRequest(
                  row.optInt("id"),
                  row.optBoolean("preview", false),
                  row.optBoolean("snooze", false),
                  row.optBoolean("wakeCheck", false)));
        }
      }
    }
  }

  private void saveSession() {
    if (activeId <= 0 || activeAlarm == null) {
      if (waiting.isEmpty()) {
        Store.clearSession(this);
      }
      return;
    }
    JSONObject saved = new JSONObject();
    JSONArray queue = new JSONArray();
    try {
      saved
          .put("alarmId", activeId)
          .put("preview", activePreview)
          .put("snooze", activeSnooze)
          .put("wakeCheck", activeWakeCheck)
          .put("missionIndex", missionIndex)
          .put("snoozeCount", snoozeCount);
      for (RingRequest request : waiting) {
        queue.put(
            new JSONObject()
                .put("id", request.id)
                .put("preview", request.preview)
                .put("snooze", request.snooze)
                .put("wakeCheck", request.wakeCheck));
      }
      saved.put("queue", queue);
      Store.saveSession(this, saved);
    } catch (JSONException ignored) {
      // JSONObject.put only fails for unsupported values; all values here
      // are primitives, so this is defensive for alternative JSON libs.
    }
  }

  private void startPlayback(Alarm alarm) {
    stopMediaOnly();
    Uri uri = soundUri(alarm);
    raiseAlarmVolumeIfNeeded(alarm);
    try {
      player = createAlarmPlayer(uri);
    } catch (Exception ignored) {
      stopMediaOnly();
      try {
        player = createAlarmPlayer(Settings.System.DEFAULT_ALARM_ALERT_URI);
      } catch (Exception fallbackFailure) {
        stopMediaOnly();
        return;
      }
    }
    if (player != null) {
      player.setLooping(true);
      player.setOnErrorListener(
          (mp, what, extra) -> {
            stopMediaOnly();
            return true;
          });
      float target = targetVolume(alarm);
      float initial = alarm.gentleSeconds > 0 ? target * 0.08f : target;
      player.setVolume(initial, initial);
      try {
        player.start();
        beginVolumeRamp(alarm, target);
      } catch (RuntimeException ignored) {
        stopMediaOnly();
      }
    }
  }

  private MediaPlayer createAlarmPlayer(Uri uri) throws IOException {
    if (uri == null) {
      throw new IOException("No alarm sound URI");
    }
    MediaPlayer candidate = new MediaPlayer();
    candidate.setAudioAttributes(
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build());
    candidate.setDataSource(this, uri);
    candidate.prepare();
    return candidate;
  }

  private void raiseAlarmVolumeIfNeeded(Alarm alarm) {
    if (!alarm.extraLoud || alarmVolumeRaised) {
      return;
    }
    AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
    if (manager == null) {
      return;
    }
    try {
      alarmVolumeManager = manager;
      previousAlarmVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM);
      int maximum = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
      if (maximum > 0 && previousAlarmVolume < maximum) {
        manager.setStreamVolume(AudioManager.STREAM_ALARM, maximum, 0);
        alarmVolumeRaised = true;
      }
    } catch (RuntimeException ignored) {
      alarmVolumeManager = null;
      previousAlarmVolume = -1;
    }
  }

  private Uri soundUri(Alarm alarm) {
    if (!TextUtils.isEmpty(alarm.sound)) {
      try {
        if (alarm.sound.contains("://") || alarm.sound.startsWith("content:")) {
          return Uri.parse(alarm.sound);
        }
        int resource = getResources().getIdentifier(alarm.sound, "raw", getPackageName());
        if (resource != 0) {
          return Uri.parse("android.resource://" + getPackageName() + "/" + resource);
        }
      } catch (RuntimeException ignored) {
        // use the platform alarm sound below
      }
    }
    return Settings.System.DEFAULT_ALARM_ALERT_URI;
  }

  private float targetVolume(Alarm alarm) {
    if (alarm.extraLoud) {
      return 1.0f;
    }
    return Math.max(0.05f, Math.min(1.0f, alarm.volume / 100.0f));
  }

  private void beginVolumeRamp(Alarm alarm, float target) {
    if (main == null || player == null || alarm.gentleSeconds <= 0) {
      return;
    }
    final long started = System.currentTimeMillis();
    final long duration = alarm.gentleSeconds * 1000L;
    volumeRamp =
        new Runnable() {
          @Override
          public void run() {
            if (player == null || !player.isPlaying()) {
              return;
            }
            float progress =
                Math.min(1.0f, (System.currentTimeMillis() - started) / (float) duration);
            float volume = target * (0.08f + 0.92f * progress);
            try {
              player.setVolume(volume, volume);
            } catch (RuntimeException ignored) {
              return;
            }
            if (progress < 1.0f) {
              main.postDelayed(this, 500L);
            }
          }
        };
    main.post(volumeRamp);
  }

  private void startVibration(Alarm alarm) {
    if (!alarm.vibrate || vibrator == null || !vibrator.hasVibrator()) {
      return;
    }
    try {
      long[] pattern = new long[] {0L, 450L, 350L, 450L};
      if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(
            VibrationEffect.createWaveform(pattern, 0),
            new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
      } else {
        vibrator.vibrate(pattern, 0);
      }
    } catch (RuntimeException ignored) {
      // Vibration permission is optional; audio remains active.
    }
  }

  private void speakReminders(Alarm alarm) {
    speakReminders(alarm, true);
  }

  private void speakReminders(Alarm alarm, boolean refreshAllowed) {
    SharedPreferences preferences = Store.prefs(this);
    long age = System.currentTimeMillis() - preferences.getLong("weather_time", 0);
    String city = preferences.getString("city", "");
    if (refreshAllowed && alarm.weatherReminder && !city.isEmpty() && age > 3 * 60 * 60 * 1000L) {
      Weather.refresh(
          this,
          city,
          () -> {
            if (activeId == alarm.id && activeAlarm != null) speakReminders(alarm, false);
          });
      return;
    }
    boolean useSpeech = alarm.timeReminder || alarm.labelReminder || alarm.weatherReminder;
    if (!useSpeech) {
      return;
    }
    StringBuilder message = new StringBuilder();
    if (alarm.timeReminder) {
      message.append(
          DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
              .format(Calendar.getInstance().getTime()));
    }
    if (alarm.labelReminder && !TextUtils.isEmpty(alarm.label)) {
      if (message.length() > 0) message.append(". ");
      message.append(alarm.label);
    }
    if (alarm.weatherReminder) {
      String weather =
          age <= 3 * 60 * 60 * 1000L && preferences.getLong("weather_time", 0) > 0
              ? preferences.getString("weather_text", "")
              : "Previsão do tempo indisponível no momento";
      if (!TextUtils.isEmpty(weather)) {
        if (message.length() > 0) message.append(". ");
        message.append(weather);
      }
    }
    if (message.length() == 0) {
      return;
    }
    if (speech == null) {
      speech =
          new TextToSpeech(
              getApplicationContext(),
              status -> {
                if (status == TextToSpeech.SUCCESS && speech != null) {
                  speech.setAudioAttributes(
                      new AudioAttributes.Builder()
                          .setUsage(AudioAttributes.USAGE_ALARM)
                          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                          .build());
                  speech.setLanguage(new Locale("pt", "BR"));
                  speech.speak(
                      message.toString(), TextToSpeech.QUEUE_FLUSH, null, "desperta-reminder");
                }
              });
    } else {
      speech.speak(message.toString(), TextToSpeech.QUEUE_FLUSH, null, "desperta-reminder");
    }
  }

  private void configureAudioRoute() {
    AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
    if (manager == null) {
      return;
    }
    String route =
        Store.prefs(this)
            .getString("output_route", Store.prefs(this).getString("output", "device"));
    try {
      if (!audioRouteCaptured) {
        routedAudioManager = manager;
        previousAudioMode = manager.getMode();
        previousSpeakerphone = manager.isSpeakerphoneOn();
        audioRouteCaptured = true;
      }
      manager.setMode(AudioManager.MODE_NORMAL);
      manager.setSpeakerphoneOn("speaker".equalsIgnoreCase(route));
    } catch (RuntimeException ignored) {
      // Routing is best effort and must not prevent the alarm.
    }
  }

  private void stopPlayback() {
    stopMediaOnly();
    if (main != null && volumeRamp != null) {
      main.removeCallbacks(volumeRamp);
      volumeRamp = null;
    }
    if (vibrator != null) {
      try {
        vibrator.cancel();
      } catch (RuntimeException ignored) {
      }
    }
    if (speech != null) {
      try {
        speech.stop();
        speech.shutdown();
      } catch (RuntimeException ignored) {
      }
      speech = null;
    }
    if (wakeLock != null && wakeLock.isHeld()) {
      try {
        wakeLock.release();
      } catch (RuntimeException ignored) {
      }
    }
    restoreAudioRoute();
    restoreAlarmVolume();
  }

  private void restoreAudioRoute() {
    if (!audioRouteCaptured || routedAudioManager == null) {
      return;
    }
    try {
      routedAudioManager.setSpeakerphoneOn(previousSpeakerphone);
      routedAudioManager.setMode(previousAudioMode);
    } catch (RuntimeException ignored) {
      // The device may have changed its route while the alarm played.
    } finally {
      audioRouteCaptured = false;
      routedAudioManager = null;
    }
  }

  private void restoreAlarmVolume() {
    if (!alarmVolumeRaised || alarmVolumeManager == null || previousAlarmVolume < 0) {
      return;
    }
    try {
      alarmVolumeManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0);
    } catch (RuntimeException ignored) {
      // The user or system may have changed volume while the alarm ran.
    } finally {
      alarmVolumeRaised = false;
      alarmVolumeManager = null;
      previousAlarmVolume = -1;
    }
  }

  private void stopMediaOnly() {
    if (player != null) {
      try {
        if (player.isPlaying()) player.stop();
      } catch (RuntimeException ignored) {
      }
      try {
        player.reset();
        player.release();
      } catch (RuntimeException ignored) {
      }
      player = null;
    }
  }

  private void acquireWakeLock() {
    try {
      PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
      if (power != null) {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":alarm");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(WAKE_LOCK_MS);
      }
    } catch (RuntimeException ignored) {
    }
  }

  private Notification buildNotification(Alarm alarm, boolean preview) {
    Intent fullScreen =
        new Intent()
            .setClassName(this, getPackageName() + ".RingActivity")
            .putExtra(Scheduler.EXTRA_ALARM_ID, alarm.id)
            .putExtra(Scheduler.EXTRA_PREVIEW, preview)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent fullScreenIntent =
        PendingIntent.getActivity(
            this,
            alarm.id,
            fullScreen,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder =
        Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);
    return builder
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle(preview ? "Prévia do alarme" : "Hora de despertar")
        .setContentText(TextUtils.isEmpty(alarm.label) ? "Desperta" : alarm.label)
        .setCategory(Notification.CATEGORY_ALARM)
        .setPriority(Notification.PRIORITY_MAX)
        .setOngoing(true)
        .setAutoCancel(false)
        .setContentIntent(fullScreenIntent)
        .setFullScreenIntent(fullScreenIntent, true)
        .build();
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT < 26) {
      return;
    }
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (manager == null) {
      return;
    }
    NotificationChannel channel =
        new NotificationChannel(CHANNEL_ID, "Alarm ringing", NotificationManager.IMPORTANCE_HIGH);
    channel.setDescription("Desperta active alarms");
    channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
    channel.setSound(null, null);
    manager.createNotificationChannel(channel);
  }
}
