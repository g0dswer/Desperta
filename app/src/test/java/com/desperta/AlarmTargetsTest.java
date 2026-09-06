package com.desperta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/** Regression tests for the reusable barcode targets added to the alarm model. */
public class AlarmTargetsTest {
  @Test
  public void legacyBarcodeMissionUsesItsOriginalTargetAndRoundTripsWithoutTargetsField()
      throws Exception {
    JSONObject legacyAlarm =
        new JSONObject()
            .put("id", 73001)
            .put("hour", 7)
            .put("minute", 30)
            .put("days", 127)
            .put(
                "missions",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("type", "barcode")
                            .put("target", "7891035002427")
                            .put("count", 1)));

    Alarm restored = Alarm.from(legacyAlarm);
    assertEquals(Arrays.asList("7891035002427"), restored.missions.get(0).acceptedCodes());

    JSONObject encoded = restored.json();
    JSONObject encodedMission = encoded.getJSONArray("missions").getJSONObject(0);
    assertTrue(encodedMission.has("targets"));
    assertEquals(0, encodedMission.getJSONArray("targets").length());
    Alarm roundTripped = Alarm.from(encoded);
    assertEquals(
        restored.missions.get(0).acceptedCodes(), roundTripped.missions.get(0).acceptedCodes());
  }

  @Test
  public void multipleTargetsAreSerializedInOrderAndDuplicateEntriesAreIgnoredOnRead()
      throws Exception {
    Alarm alarm = new Alarm();
    alarm.id = 73002;
    Alarm.Mission mission = new Alarm.Mission("barcode", "7891035002427", 1);
    mission.targets.add("7891035002427");
    mission.targets.add("DESPERTA-ACORDAR-2026");
    alarm.missions.add(mission);

    JSONObject encoded = alarm.json();
    encoded
        .getJSONArray("missions")
        .getJSONObject(0)
        .put(
            "targets",
            new JSONArray().put("7891035002427").put("DESPERTA-ACORDAR-2026").put("7891035002427"));
    Alarm restored = Alarm.from(encoded);

    assertEquals(
        Arrays.asList("7891035002427", "DESPERTA-ACORDAR-2026"),
        restored.missions.get(0).acceptedCodes());
  }

  @Test
  public void copyingAnAlarmDeepCopiesTargetsAndResetsSkipState() {
    Alarm alarm = new Alarm();
    alarm.id = 73003;
    alarm.skipUntil = 123456789L;
    alarm.label = "Código de casa";
    Alarm.Mission mission = new Alarm.Mission("barcode", "7891035002427", 1);
    mission.targets.add("7891035002427");
    mission.targets.add("DESPERTA-ACORDAR-2026");
    alarm.missions.add(mission);

    Alarm copy = alarm.copy();

    assertNotEquals(alarm.id, copy.id);
    assertEquals(0, copy.skipUntil);
    assertEquals(alarm.label, copy.label);
    assertEquals(alarm.missions.get(0).acceptedCodes(), copy.missions.get(0).acceptedCodes());
    copy.missions.get(0).targets.add("another-code");
    assertFalse(alarm.missions.get(0).targets.contains("another-code"));
  }
}
