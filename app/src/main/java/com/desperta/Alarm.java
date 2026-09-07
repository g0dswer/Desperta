package com.desperta;

import java.util.*;
import org.json.*;

public class Alarm {
  public int id = (int) (System.currentTimeMillis() & 0x3fffffff),
      hour = 7,
      minute = 30,
      days = 127,
      volume = 80,
      gentleSeconds = 60,
      snoozeMinutes = 10,
      snoozeLimit = 1,
      wakeCheckMinutes = 0;
  public boolean enabled = true,
      vibrate = true,
      timeReminder = false,
      weatherReminder = false,
      labelReminder = false,
      extraLoud = false,
      preventPower = false;
  /**
   * Absolute local-time instant for a one-time replacement of the next regular occurrence.
   *
   * <p>A value of zero means that the weekly schedule is active. The field is deliberately an
   * instant rather than another hour/minute pair so that the UI can choose a concrete upcoming
   * date, and so a process restart cannot accidentally reinterpret the edit as today's alarm.
   * nextOverrideOriginalAt remembers the regular occurrence displaced by the override, including
   * when the temporary time is earlier than the regular time on the same day.
   */
  public long skipUntil = 0, nextOverrideAt = 0, nextOverrideOriginalAt = 0;
  public String label = "Bom dia", sound = "", wallpaper = "Aurora";
  public List<Mission> missions = new ArrayList<>();

  public static class Mission {
    public String type, target;
    public int count;
    public List<String> targets = new ArrayList<>();

    public ArrayList<String> acceptedCodes() {
      ArrayList<String> codes = new ArrayList<>(targets);
      if (codes.isEmpty() && target != null && !target.isEmpty()) codes.add(target);
      return codes;
    }

    public Mission(String t, String v, int n) {
      type = t;
      target = v;
      count = n;
    }

    public JSONObject json() throws JSONException {
      return new JSONObject()
          .put("type", type)
          .put("target", target)
          .put("count", count)
          .put("targets", new JSONArray(targets));
    }
  }

  public JSONObject json() {
    try {
      JSONObject o = new JSONObject();
      for (var f : Alarm.class.getFields())
        if (!f.getName().equals("missions")) o.put(f.getName(), f.get(this));
      JSONArray a = new JSONArray();
      for (Mission m : missions) a.put(m.json());
      o.put("missions", a);
      return o;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static Alarm from(JSONObject o) {
    Alarm a = new Alarm();
    try {
      for (var f : Alarm.class.getFields()) {
        String n = f.getName();
        if (n.equals("missions") || !o.has(n)) continue;
        if (f.getType() == int.class) f.setInt(a, o.getInt(n));
        else if (f.getType() == long.class) f.setLong(a, o.getLong(n));
        else if (f.getType() == boolean.class) f.setBoolean(a, o.getBoolean(n));
        else f.set(a, o.getString(n));
      }
      JSONArray ms = o.optJSONArray("missions");
      if (ms != null)
        for (int i = 0; i < ms.length(); i++) {
          JSONObject m = ms.getJSONObject(i);
          Mission mission =
              new Mission(m.getString("type"), m.optString("target"), m.optInt("count", 1));
          JSONArray codes = m.optJSONArray("targets");
          if (codes != null)
            for (int j = 0; j < codes.length(); j++) {
              String code = codes.optString(j);
              if (!code.isEmpty() && !mission.targets.contains(code)) mission.targets.add(code);
            }
          a.missions.add(mission);
        }
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
    return a;
  }

  public Alarm copy() {
    Alarm a = from(json());
    a.id = (int) (System.nanoTime() & 0x3fffffff);
    a.skipUntil = 0;
    a.nextOverrideAt = 0;
    a.nextOverrideOriginalAt = 0;
    return a;
  }
}
