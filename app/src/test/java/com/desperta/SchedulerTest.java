package com.desperta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Pure scheduling tests; no Android device or alarm permission is required. */
public class SchedulerTest {
  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
  private TimeZone previousTimeZone;

  @Before
  public void useUtcForThePublicSchedulerApi() {
    previousTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(UTC);
  }

  @After
  public void restoreDefaultTimeZone() {
    TimeZone.setDefault(previousTimeZone);
  }

  private static long instant(TimeZone zone, int year, int month, int day, int hour, int minute) {
    Calendar c = Calendar.getInstance(zone);
    c.clear();
    c.set(year, month - 1, day, hour, minute, 0);
    return c.getTimeInMillis();
  }

  @Test
  public void dailyAlarmUsesTheNextFutureClockOccurrence() {
    Alarm alarm = new Alarm();
    alarm.hour = 7;
    alarm.minute = 30;
    alarm.days = 127;
    long now = instant(UTC, 2026, 9, 6, 6, 45);
    long next = Scheduler.next(alarm, now, UTC);
    assertEquals(instant(UTC, 2026, 9, 6, 7, 30), next);
  }

  @Test
  public void sundayBitZeroSelectsSundayAndSkipsOtherDays() {
    Alarm alarm = new Alarm();
    alarm.hour = 7;
    alarm.minute = 30;
    alarm.days = 1; // Sunday is bit zero.
    long monday = instant(UTC, 2026, 9, 7, 8, 0);
    assertEquals(instant(UTC, 2026, 9, 13, 7, 30), Scheduler.next(alarm, monday, UTC));
  }

  @Test
  public void skipUntilSuppressesExactlyOneMatchingOccurrence() {
    Alarm alarm = new Alarm();
    alarm.hour = 7;
    alarm.minute = 30;
    alarm.days = 127;
    long now = instant(UTC, 2026, 9, 6, 6, 45);
    alarm.skipUntil = instant(UTC, 2026, 9, 6, 7, 30);
    assertEquals(instant(UTC, 2026, 9, 7, 7, 30), Scheduler.next(alarm, now, UTC));
  }

  @Test
  public void zeroMaskIsAOneShotClockOccurrence() {
    Alarm alarm = new Alarm();
    alarm.hour = 7;
    alarm.minute = 30;
    alarm.days = 0;
    long after = instant(UTC, 2026, 9, 6, 8, 0);
    assertEquals(instant(UTC, 2026, 9, 7, 7, 30), Scheduler.next(alarm, after, UTC));
  }

  @Test
  public void springDstGapNormalizesToTheFirstValidLocalTime() {
    TimeZone eastern = TimeZone.getTimeZone("America/New_York");
    Alarm alarm = new Alarm();
    alarm.hour = 2;
    alarm.minute = 30;
    alarm.days = 127;
    long before = instant(eastern, 2024, 3, 10, 1, 0);
    long next = Scheduler.next(alarm, before, eastern);
    Calendar local = Calendar.getInstance(eastern);
    local.setTimeInMillis(next);
    assertEquals(2024, local.get(Calendar.YEAR));
    assertEquals(Calendar.MARCH, local.get(Calendar.MONTH));
    assertEquals(10, local.get(Calendar.DAY_OF_MONTH));
    assertEquals(3, local.get(Calendar.HOUR_OF_DAY));
    assertEquals(30, local.get(Calendar.MINUTE));
  }

  @Test
  public void fallDstOccurrenceIsStrictlyAfterTheInput() {
    TimeZone eastern = TimeZone.getTimeZone("America/New_York");
    Alarm alarm = new Alarm();
    alarm.hour = 1;
    alarm.minute = 30;
    alarm.days = 127;
    long afterFirstOneThirty = instant(eastern, 2024, 11, 3, 1, 45);
    long next = Scheduler.next(alarm, afterFirstOneThirty, eastern);
    assertTrue(next > afterFirstOneThirty);
    Calendar local = Calendar.getInstance(eastern);
    local.setTimeInMillis(next);
    assertEquals(4, local.get(Calendar.DAY_OF_MONTH));
    assertEquals(1, local.get(Calendar.HOUR_OF_DAY));
    assertEquals(30, local.get(Calendar.MINUTE));
  }

  @Test
  public void oneTimeOverrideWinsWhenItIsLaterThanTheRegularOccurrence() {
    Alarm alarm = new Alarm();
    alarm.hour = 7;
    alarm.minute = 30;
    alarm.days = 127;
    long now = instant(UTC, 2026, 9, 7, 6, 45);
    long regular = instant(UTC, 2026, 9, 7, 7, 30);
    long override = instant(UTC, 2026, 9, 7, 8, 0);

    assertTrue(Scheduler.setNextOverride(alarm, override, now));
    assertEquals(regular, alarm.nextOverrideOriginalAt);
    assertEquals(override, Scheduler.next(alarm, now, UTC));
    assertTrue(regular < Scheduler.next(alarm, now, UTC));
  }

  @Test
  public void earlierOverrideSuppressesTheOriginalOccurrenceAfterDelivery() {
    Alarm alarm = new Alarm();
    alarm.hour = 7;
    alarm.minute = 30;
    alarm.days = 127;
    long now = instant(UTC, 2026, 9, 7, 6, 0);
    long override = instant(UTC, 2026, 9, 7, 6, 30);
    long original = instant(UTC, 2026, 9, 7, 7, 30);
    long following = instant(UTC, 2026, 9, 8, 7, 30);
    assertTrue(Scheduler.setNextOverride(alarm, override, now));
    assertEquals(original, alarm.nextOverrideOriginalAt);
    assertEquals(override, Scheduler.next(alarm, now, UTC));

    assertTrue(Scheduler.consumeNextOverride(alarm, instant(UTC, 2026, 9, 7, 6, 31)));
    assertEquals(original, alarm.skipUntil);
    assertEquals(following, Scheduler.next(alarm, instant(UTC, 2026, 9, 7, 6, 31), UTC));
  }

  @Test
  public void restartAfterAnEarlierOverridePassedStillSkipsTheOriginalOccurrence() {
    Alarm alarm = new Alarm();
    alarm.hour = 7;
    alarm.minute = 30;
    alarm.days = 127;
    long now = instant(UTC, 2026, 9, 7, 6, 0);
    long override = instant(UTC, 2026, 9, 7, 6, 30);
    long following = instant(UTC, 2026, 9, 8, 7, 30);
    assertTrue(Scheduler.setNextOverride(alarm, override, now));

    // The process can restart after the temporary trigger but before the service consumes it.
    assertEquals(following, Scheduler.next(alarm, instant(UTC, 2026, 9, 7, 6, 31), UTC));
  }

  @Test
  public void nextOverrideRejectsPastTimestampWithoutChangingExistingValue() {
    Alarm alarm = new Alarm();
    long now = instant(UTC, 2026, 9, 7, 10, 0);
    long original = instant(UTC, 2026, 9, 8, 7, 30);
    assertTrue(Scheduler.setNextOverride(alarm, original, now));
    long displaced = alarm.nextOverrideOriginalAt;

    assertFalse(Scheduler.setNextOverride(alarm, now, now));
    assertEquals(original, alarm.nextOverrideAt);
    assertEquals(displaced, alarm.nextOverrideOriginalAt);
    assertEquals(original, Scheduler.next(alarm, now, UTC));
  }

  @Test
  public void replacingOverrideRetainsTheOriginalWeeklyOccurrence() {
    Alarm alarm = new Alarm();
    alarm.hour = 7;
    alarm.minute = 30;
    alarm.days = 127;
    long now = instant(UTC, 2026, 9, 7, 6, 0);
    long firstOverride = instant(UTC, 2026, 9, 7, 6, 30);
    long replacement = instant(UTC, 2026, 9, 7, 8, 0);
    long original = instant(UTC, 2026, 9, 7, 7, 30);
    long following = instant(UTC, 2026, 9, 8, 7, 30);
    assertTrue(Scheduler.setNextOverride(alarm, firstOverride, now));
    assertTrue(Scheduler.setNextOverride(alarm, replacement, now));
    assertEquals(original, alarm.nextOverrideOriginalAt);
    assertEquals(replacement, Scheduler.next(alarm, now, UTC));
    assertTrue(Scheduler.consumeNextOverride(alarm, instant(UTC, 2026, 9, 7, 8, 1)));
    assertEquals(following, Scheduler.next(alarm, instant(UTC, 2026, 9, 7, 8, 1), UTC));
  }

  @Test
  public void skippingTheOverrideUsesTheFollowingRegularOccurrence() {
    Alarm alarm = new Alarm();
    alarm.hour = 7;
    alarm.minute = 30;
    alarm.days = 127;
    long now = instant(UTC, 2026, 9, 7, 6, 45);
    long override = instant(UTC, 2026, 9, 7, 8, 0);
    long following = instant(UTC, 2026, 9, 8, 7, 30);
    assertTrue(Scheduler.setNextOverride(alarm, override, now));

    // This mirrors the existing menu action in MainActivity.
    alarm.skipUntil = Scheduler.next(alarm, now, UTC);
    assertEquals(following, Scheduler.next(alarm, now, UTC));
    assertFalse(Scheduler.hasNextOverride(alarm, now));
    assertEquals(override, alarm.skipUntil);
  }

  @Test
  public void delayedOverrideIsConsumedOnceAndLeavesWeeklyScheduleIntact() {
    Alarm alarm = new Alarm();
    alarm.hour = 7;
    alarm.minute = 30;
    alarm.days = 127;
    long now = instant(UTC, 2026, 9, 7, 6, 45);
    long override = instant(UTC, 2026, 9, 7, 8, 0);
    long afterDelivery = instant(UTC, 2026, 9, 7, 8, 4);
    long nextRegular = instant(UTC, 2026, 9, 8, 7, 30);
    assertTrue(Scheduler.setNextOverride(alarm, override, now));

    assertTrue(Scheduler.consumeNextOverride(alarm, afterDelivery));
    assertEquals(0L, alarm.nextOverrideAt);
    assertFalse(Scheduler.consumeNextOverride(alarm, afterDelivery));
    assertEquals(nextRegular, Scheduler.next(alarm, afterDelivery, UTC));
  }
}
