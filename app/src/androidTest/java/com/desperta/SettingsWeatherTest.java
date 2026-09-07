package com.desperta;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Connected smoke tests for the settings contract. Weather assertions are intentionally bounded and
 * use the real Open-Meteo endpoints; an offline device should fail these tests rather than silently
 * claiming that networking works.
 */
@RunWith(AndroidJUnit4.class)
public final class SettingsWeatherTest {
  private static final long NETWORK_TIMEOUT_SECONDS = 25L;

  private Context context;
  private SharedPreferences preferences;
  private String oldIdentity;
  private String oldOutput;

  @Rule
  public final ActivityTestRule<SettingsActivity> activityRule =
      new ActivityTestRule<>(SettingsActivity.class);

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    preferences = context.getSharedPreferences(Weather.PREFS, Context.MODE_PRIVATE);
    oldIdentity = preferences.getString(Identity.KEY, null);
    oldOutput = preferences.getString("output", null);
    Weather.clear(context);
  }

  @After
  public void tearDown() {
    SharedPreferences.Editor editor = preferences.edit().remove(Identity.KEY).remove("output");
    if (oldIdentity != null) editor.putString(Identity.KEY, oldIdentity);
    if (oldOutput != null) editor.putString("output", oldOutput);
    editor.apply();
    Weather.clear(context);
  }

  @Test
  public void settingsPersistIdentityAndOutputChoices() {
    onView(withText("Preferências")).check(matches(isDisplayed()));

    onView(withText("Identidade")).perform(click());
    onView(withText(org.hamcrest.Matchers.startsWith("Anos 90"))).perform(click());
    assertEquals(Identity.NINETIES, preferences.getString(Identity.KEY, ""));
    if (Build.VERSION.SDK_INT < 35) {
      assertEquals(
          Identity.current(context).bg, activityRule.getActivity().getWindow().getStatusBarColor());
      assertEquals(
          Identity.current(context).bg,
          activityRule.getActivity().getWindow().getNavigationBarColor());
    }

    onView(withText("Saída de som")).perform(click());
    onView(withText("Alto-falante")).perform(click());
    assertEquals("speaker", preferences.getString("output", ""));
    assertEquals("speaker", preferences.getString("output_route", ""));
  }

  @Test
  public void weatherRefreshUsesRealGeocodingAndForecast() throws Exception {
    CountDownLatch success = new CountDownLatch(1);
    Weather.refresh(context, "São Paulo", success::countDown);
    assertTrue(
        "Open-Meteo success callback timed out",
        success.await(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertTrue(
        "successful weather response must have a timestamp: " + Weather.getCachedText(context),
        Weather.getCachedTime(context) > 0L);
    String text = Weather.getCachedText(context);
    assertFalse(
        "successful weather response must contain readable text",
        text == null || text.trim().isEmpty() || text.startsWith("Clima indisponível:"));
    assertTrue("forecast should include a temperature", text.contains("°C"));
  }

  @Test
  public void weatherRefreshReportsInvalidCityAndClearsTimestamp() throws Exception {
    CountDownLatch failure = new CountDownLatch(1);
    Weather.refresh(context, "cidade-inexistente-desperta-000000000", failure::countDown);
    assertTrue(
        "Open-Meteo error callback timed out",
        failure.await(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertEquals(0L, Weather.getCachedTime(context));
    assertTrue(Weather.getCachedText(context).startsWith("Clima indisponível:"));
  }
}
