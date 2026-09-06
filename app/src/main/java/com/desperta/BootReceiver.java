package com.desperta;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Rebuilds AlarmManager entries after boot, clock edits, or timezone changes. */
public final class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (context == null) {
      return;
    }
    String action = intent == null ? null : intent.getAction();
    if (Intent.ACTION_BOOT_COMPLETED.equals(action)
        || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
        || Intent.ACTION_TIME_CHANGED.equals(action)
        || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
        || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
        || AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action)
        || action == null) {
      try {
        Scheduler.rescheduleAll(context.getApplicationContext());
      } catch (SecurityException ignored) {
        // The Settings screen exposes the exact-alarm access action;
        // retain the alarm records until the user grants it.
      }
    }
  }
}
