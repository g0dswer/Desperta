package com.desperta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Receives AlarmManager deliveries and hands all playback/UI work to the service. */
public final class AlarmReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (context == null) {
      return;
    }
    Intent source = intent == null ? new Intent() : intent;
    int alarmId = source.getIntExtra(Scheduler.EXTRA_ALARM_ID, -1);
    if (alarmId <= 0) {
      return;
    }
    boolean preview = source.getBooleanExtra(Scheduler.EXTRA_PREVIEW, false);
    boolean snooze = source.getBooleanExtra(Scheduler.EXTRA_SNOOZE, false);
    boolean wakeCheck = source.getBooleanExtra(Scheduler.EXTRA_WAKE_CHECK, false);
    AlarmService.start(context, alarmId, preview, snooze, wakeCheck);
  }
}
