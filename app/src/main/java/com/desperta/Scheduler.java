package com.desperta;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/** AlarmManager integration and deterministic next-occurrence calculation. */
public final class Scheduler {
  public static final String ACTION_ALARM = "com.desperta.action.ALARM";
  public static final String ACTION_SHOW_ALARMS = "com.desperta.action.SHOW_ALARMS";
  public static final String EXTRA_ALARM_ID = "alarm_id";
  public static final String EXTRA_PREVIEW = "preview";
  public static final String EXTRA_SNOOZE = "snooze";
  public static final String EXTRA_WAKE_CHECK = "wake_check";

  private static final int REQUEST_OFFSET = 0x24000000;
  private static final long MAX_SEARCH_DAYS = 370L;

  private Scheduler() {}

  /**
   * Find the next local wall-clock occurrence strictly after {@code fromMillis}. Sunday is bit 0 in
   * Alarm.days, Monday bit 1, and so on. A zero mask is a one-shot alarm and therefore uses the
   * next occurrence of its clock time. Alarm.skipUntil suppresses the matching occurrence when it
   * is still in the future; it is intentionally not cleared here because this method is pure.
   */
  public static long next(Alarm alarm, long fromMillis) {
    return next(alarm, fromMillis, TimeZone.getDefault());
  }

  /** Package-visible overload used by tests to make timezone behavior explicit. */
  static long next(Alarm alarm, long fromMillis, TimeZone timeZone) {
    if (alarm == null) {
      return -1L;
    }
    TimeZone zone = timeZone == null ? TimeZone.getDefault() : timeZone;
    long base = Math.max(0L, fromMillis);
    Calendar date = Calendar.getInstance(zone);
    date.setTimeInMillis(base);
    date.set(Calendar.HOUR_OF_DAY, 0);
    date.set(Calendar.MINUTE, 0);
    date.set(Calendar.SECOND, 0);
    date.set(Calendar.MILLISECOND, 0);

    int hour = Math.max(0, Math.min(23, alarm.hour));
    int minute = Math.max(0, Math.min(59, alarm.minute));
    int mask = alarm.days & 0x7f;
    long skipUntil = alarm.skipUntil;

    for (int offset = 0; offset <= MAX_SEARCH_DAYS; offset++) {
      int sundayBasedDay = date.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
      boolean matches = mask == 0 || (mask & (1 << sundayBasedDay)) != 0;
      if (matches) {
        // Calendar normalizes a nonexistent local time during a spring
        // DST gap to the first valid instant after the gap.
        Calendar candidate = (Calendar) date.clone();
        candidate.setLenient(true);
        candidate.set(Calendar.HOUR_OF_DAY, hour);
        candidate.set(Calendar.MINUTE, minute);
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);
        long at = candidate.getTimeInMillis();
        if (at > base && at > skipUntil) {
          return at;
        }
      }

      // A zero mask is a one-shot clock occurrence.  Once today's
      // occurrence was considered, its next valid date is tomorrow;
      // returning it gives callers a safe alarm rather than a null alarm.
      date.add(Calendar.DAY_OF_MONTH, 1);
    }
    return -1L;
  }

  /** Schedule the next enabled occurrence of an alarm. */
  public static void schedule(Context context, Alarm alarm) {
    if (context == null || alarm == null) {
      return;
    }
    if (!alarm.enabled) {
      cancel(context, alarm.id);
      return;
    }
    long triggerAt = next(alarm, System.currentTimeMillis());
    if (triggerAt > 0L) {
      scheduleAt(context, alarm.id, triggerAt, false, false);
    } else {
      cancel(context, alarm.id);
    }
  }

  /** Schedule every enabled persisted alarm after boot or a time-zone change. */
  public static void rescheduleAll(Context context) {
    if (context == null) {
      return;
    }
    List<Alarm> alarms = Store.all(context);
    for (Alarm alarm : alarms) {
      schedule(context, alarm);
    }
  }

  /** Cancel all PendingIntent variants used by this alarm. */
  public static void cancel(Context context, int alarmId) {
    if (context == null) {
      return;
    }
    AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (manager == null) {
      return;
    }
    manager.cancel(pendingIntent(context, alarmId, false, false));
    manager.cancel(pendingIntent(context, alarmId, false, true));
    manager.cancel(pendingIntent(context, alarmId, true, false));
    manager.cancel(pendingIntent(context, alarmId, false, false, true));
    manager.cancel(pendingIntent(context, alarmId, false, true, true));
    manager.cancel(pendingIntent(context, alarmId, true, false, true));
  }

  /**
   * Schedule an explicit trigger used for snooze and wake-check sessions. The alarm itself is not
   * changed, so a preview or snooze never mutates the user's repeating schedule.
   */
  public static void scheduleAt(
      Context context, int alarmId, long triggerAt, boolean preview, boolean snooze) {
    scheduleAt(context, alarmId, triggerAt, preview, snooze, false);
  }

  public static void scheduleAt(
      Context context,
      int alarmId,
      long triggerAt,
      boolean preview,
      boolean snooze,
      boolean wakeCheck) {
    if (context == null || triggerAt <= 0L) {
      return;
    }
    AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (manager == null) {
      return;
    }
    PendingIntent operation = pendingIntent(context, alarmId, preview, snooze, wakeCheck);
    Intent show =
        new Intent(context, MainActivity.class)
            .setAction(ACTION_SHOW_ALARMS)
            .setPackage(context.getPackageName());
    AlarmManager.AlarmClockInfo info =
        new AlarmManager.AlarmClockInfo(
            triggerAt,
            PendingIntent.getActivity(context, REQUEST_OFFSET - alarmId, show, pendingFlags()));
    try {
      if (Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) {
        // Do not silently turn a user's exact alarm into an inexact one:
        // callers can catch this exception and take the user to the
        // system's exact-alarm access screen.
        throw new SecurityException("Exact alarm permission is not granted");
      }
      manager.setAlarmClock(info, operation);
    } catch (SecurityException deniedExactAlarm) {
      throw deniedExactAlarm;
    }
  }

  public static PendingIntent pendingIntent(
      Context context, int alarmId, boolean preview, boolean snooze) {
    return pendingIntent(context, alarmId, preview, snooze, false);
  }

  public static PendingIntent pendingIntent(
      Context context, int alarmId, boolean preview, boolean snooze, boolean wakeCheck) {
    String kind = preview ? "preview" : (snooze ? (wakeCheck ? "wake" : "snooze") : "alarm");
    Intent intent =
        new Intent(context, AlarmReceiver.class)
            .setAction(ACTION_ALARM)
            .setData(Uri.parse("desperta://alarm/" + alarmId + "/" + kind))
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_PREVIEW, preview)
            .putExtra(EXTRA_SNOOZE, snooze)
            .putExtra(EXTRA_WAKE_CHECK, wakeCheck);
    int requestCode = REQUEST_OFFSET ^ (alarmId * 31) ^ kind.hashCode();
    return PendingIntent.getBroadcast(context, requestCode, intent, pendingFlags());
  }

  private static int pendingFlags() {
    return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
  }
}
