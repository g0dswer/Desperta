package com.desperta;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Small, synchronous persistence layer for alarms and the alarm history.
 *
 * <p>The app deliberately keeps this data in one private SharedPreferences file. Alarm changes are
 * committed synchronously: an alarm is commonly edited immediately before the process is
 * backgrounded and an asynchronous apply() would make a crash lose that edit. The payload is
 * version tolerant because Alarm.from() ignores fields it does not know.
 */
public final class Store {
  public static final String PREFS = "desperta";
  public static final String ALARMS_KEY = "alarms";
  public static final String HISTORY_KEY = "history";
  public static final String ACTIVE_SESSION_KEY = "active_session";

  /** Sessions waiting for a snooze/wake-check delivery, keyed by alarm id. */
  public static final String SNOOZED_SESSIONS_KEY = "snoozed_sessions";

  private static final Object LOCK = new Object();
  private static final int MAX_HISTORY_ENTRIES = 500;

  private Store() {}

  public static SharedPreferences prefs(Context context) {
    return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  /** Return a defensive list of all persisted alarms, ordered by id. */
  public static List<Alarm> all(Context context) {
    if (context == null) {
      return new ArrayList<>();
    }
    synchronized (LOCK) {
      JSONArray encoded = readArray(prefs(context), ALARMS_KEY);
      List<Alarm> alarms = new ArrayList<>();
      for (int i = 0; i < encoded.length(); i++) {
        JSONObject item = encoded.optJSONObject(i);
        if (item == null) {
          continue;
        }
        try {
          Alarm alarm = Alarm.from(item);
          if (alarm.id > 0) {
            alarms.add(alarm);
          }
        } catch (RuntimeException ignored) {
          // A corrupt single record must not hide all the healthy alarms.
        }
      }
      Collections.sort(
          alarms,
          new Comparator<Alarm>() {
            @Override
            public int compare(Alarm left, Alarm right) {
              return Integer.compare(left.id, right.id);
            }
          });
      return alarms;
    }
  }

  /** Persist an alarm, replacing the record with the same id. */
  public static void save(Context context, Alarm alarm) {
    if (context == null || alarm == null) {
      return;
    }
    synchronized (LOCK) {
      if (alarm.id <= 0) {
        alarm.id = nextIdLocked(context);
      }
      JSONArray alarms = readArray(prefs(context), ALARMS_KEY);
      JSONArray replacement = new JSONArray();
      boolean replaced = false;
      for (int i = 0; i < alarms.length(); i++) {
        JSONObject item = alarms.optJSONObject(i);
        if (item == null) {
          continue;
        }
        if (item.optInt("id", Integer.MIN_VALUE) == alarm.id) {
          replacement.put(alarm.json());
          replaced = true;
        } else {
          replacement.put(item);
        }
      }
      if (!replaced) {
        replacement.put(alarm.json());
      }
      putArray(prefs(context), ALARMS_KEY, replacement);
    }
  }

  /** Remove an alarm. Calling this with an unknown id is harmless. */
  public static void delete(Context context, int id) {
    if (context == null) {
      return;
    }
    synchronized (LOCK) {
      JSONArray alarms = readArray(prefs(context), ALARMS_KEY);
      JSONArray remaining = new JSONArray();
      for (int i = 0; i < alarms.length(); i++) {
        JSONObject item = alarms.optJSONObject(i);
        if (item != null && item.optInt("id", Integer.MIN_VALUE) != id) {
          remaining.put(item);
        }
      }
      putArray(prefs(context), ALARMS_KEY, remaining);
      clearSnoozedSession(context, id);
    }
  }

  /** Read one alarm, or null when the id is not present. */
  public static Alarm get(Context context, int id) {
    if (context == null) {
      return null;
    }
    synchronized (LOCK) {
      JSONArray alarms = readArray(prefs(context), ALARMS_KEY);
      for (int i = 0; i < alarms.length(); i++) {
        JSONObject item = alarms.optJSONObject(i);
        if (item != null && item.optInt("id", Integer.MIN_VALUE) == id) {
          try {
            return Alarm.from(item);
          } catch (RuntimeException ignored) {
            return null;
          }
        }
      }
      return null;
    }
  }

  /**
   * Append a real alarm outcome to the local history. Preview sessions must never call this method.
   * The JSON shape is intentionally simple so it is also useful to the report screen and remains
   * forward compatible.
   */
  public static void logHistory(Context context, Alarm alarm, String event) {
    if (context == null || alarm == null) {
      return;
    }
    synchronized (LOCK) {
      JSONArray history = readArray(prefs(context), HISTORY_KEY);
      JSONObject row = new JSONObject();
      try {
        row.put("alarmId", alarm.id);
        row.put("label", alarm.label == null ? "" : alarm.label);
        row.put("event", event == null ? "dismissed" : event);
        row.put("timestamp", System.currentTimeMillis());
        history.put(row);
      } catch (JSONException ignored) {
        return;
      }
      // Keep history bounded so repeated alarms cannot grow preferences forever.
      int first = Math.max(0, history.length() - MAX_HISTORY_ENTRIES);
      JSONArray bounded = new JSONArray();
      for (int i = first; i < history.length(); i++) {
        bounded.put(history.opt(i));
      }
      putArray(prefs(context), HISTORY_KEY, bounded);
    }
  }

  public static JSONArray history(Context context) {
    if (context == null) {
      return new JSONArray();
    }
    synchronized (LOCK) {
      return readArray(prefs(context), HISTORY_KEY);
    }
  }

  /** Persist the service session atomically. Passing null clears it. */
  public static void saveSession(Context context, JSONObject session) {
    if (context == null) {
      return;
    }
    synchronized (LOCK) {
      SharedPreferences.Editor editor = prefs(context).edit();
      if (session == null) {
        editor.remove(ACTIVE_SESSION_KEY);
      } else {
        editor.putString(ACTIVE_SESSION_KEY, session.toString());
      }
      editor.commit();
    }
  }

  public static JSONObject getSession(Context context) {
    if (context == null) {
      return null;
    }
    synchronized (LOCK) {
      String value = prefs(context).getString(ACTIVE_SESSION_KEY, null);
      if (value == null || value.trim().isEmpty()) {
        return null;
      }
      try {
        return new JSONObject(value);
      } catch (JSONException ignored) {
        prefs(context).edit().remove(ACTIVE_SESSION_KEY).commit();
        return null;
      }
    }
  }

  public static void clearSession(Context context) {
    saveSession(context, null);
  }

  /**
   * Save a session which is waiting for a snooze or wake-check PendingIntent. This is separate from
   * ACTIVE_SESSION_KEY so another alarm can ring while the delayed session is pending without
   * overwriting its mission cursor.
   */
  public static void saveSnoozedSession(Context context, int alarmId, JSONObject session) {
    if (context == null || alarmId <= 0 || session == null) {
      return;
    }
    synchronized (LOCK) {
      SharedPreferences preferences = prefs(context);
      JSONObject all = readObject(preferences, SNOOZED_SESSIONS_KEY);
      try {
        all.put(String.valueOf(alarmId), session);
      } catch (JSONException ignored) {
        return;
      }
      preferences.edit().putString(SNOOZED_SESSIONS_KEY, all.toString()).commit();
    }
  }

  public static JSONObject getSnoozedSession(Context context, int alarmId) {
    if (context == null || alarmId <= 0) {
      return null;
    }
    synchronized (LOCK) {
      JSONObject all = readObject(prefs(context), SNOOZED_SESSIONS_KEY);
      JSONObject session = all.optJSONObject(String.valueOf(alarmId));
      if (session == null) {
        return null;
      }
      try {
        return new JSONObject(session.toString());
      } catch (JSONException ignored) {
        return null;
      }
    }
  }

  public static void clearSnoozedSession(Context context, int alarmId) {
    if (context == null || alarmId <= 0) {
      return;
    }
    synchronized (LOCK) {
      SharedPreferences preferences = prefs(context);
      JSONObject all = readObject(preferences, SNOOZED_SESSIONS_KEY);
      all.remove(String.valueOf(alarmId));
      if (all.length() == 0) {
        preferences.edit().remove(SNOOZED_SESSIONS_KEY).commit();
      } else {
        preferences.edit().putString(SNOOZED_SESSIONS_KEY, all.toString()).commit();
      }
    }
  }

  private static int nextIdLocked(Context context) {
    int candidate = (int) (System.nanoTime() & 0x3fffffff);
    if (candidate <= 0) {
      candidate = 1;
    }
    while (containsId(readArray(prefs(context), ALARMS_KEY), candidate)) {
      candidate++;
      if (candidate <= 0) {
        candidate = 1;
      }
    }
    return candidate;
  }

  private static boolean containsId(JSONArray array, int id) {
    for (int i = 0; i < array.length(); i++) {
      JSONObject item = array.optJSONObject(i);
      if (item != null && item.optInt("id", Integer.MIN_VALUE) == id) {
        return true;
      }
    }
    return false;
  }

  private static JSONArray readArray(SharedPreferences preferences, String key) {
    String value = preferences.getString(key, "[]");
    if (value == null || value.trim().isEmpty()) {
      return new JSONArray();
    }
    try {
      return new JSONArray(value);
    } catch (JSONException ignored) {
      // Fail closed: a malformed preference is replaced only for this key.
      preferences.edit().remove(key).commit();
      return new JSONArray();
    }
  }

  private static JSONObject readObject(SharedPreferences preferences, String key) {
    String value = preferences.getString(key, "{}");
    if (value == null || value.trim().isEmpty()) {
      return new JSONObject();
    }
    try {
      return new JSONObject(value);
    } catch (JSONException ignored) {
      preferences.edit().remove(key).commit();
      return new JSONObject();
    }
  }

  private static void putArray(SharedPreferences preferences, String key, JSONArray value) {
    preferences.edit().putString(key, value.toString()).commit();
  }
}
