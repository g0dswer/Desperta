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
  public long skipUntil = 0;
  public String label = "Bom dia", sound = "", wallpaper = "Aurora";
  public List<Mission> missions = new ArrayList<>();

  public static class Mission {
    public String type, target;
    public int count;

    public Mission(String t, String v, int n) {
      type = t;
      target = v;
      count = n;
    }

    public JSONObject json() throws JSONException {
      return new JSONObject().put("type", type).put("target", target).put("count", count);
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
          a.missions.add(
              new Mission(m.getString("type"), m.optString("target"), m.optInt("count", 1)));
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
    return a;
  }
}
