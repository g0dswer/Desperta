package com.desperta;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/** Runtime coverage for the public update path and the update settings contract. */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class UpdateFlowTest {
  private static final long NETWORK_TIMEOUT_SECONDS = 25L;
  private Context context;
  private SharedPreferences preferences;
  private boolean oldAuto;
  private boolean oldPreviews;
  private boolean oldAutoPresent;
  private boolean oldPreviewsPresent;
  private String[] oldStrings;
  private long oldLastCheck;
  private boolean oldLastCheckPresent;
  private boolean oldPreviewPresent;
  private boolean oldLastPrerelease;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    preferences = context.getSharedPreferences(UpdateChecker.PREFS, Context.MODE_PRIVATE);
    oldAutoPresent = preferences.contains(UpdateChecker.KEY_AUTO_CHECK);
    oldAuto = preferences.getBoolean(UpdateChecker.KEY_AUTO_CHECK, true);
    oldPreviewsPresent = preferences.contains(UpdateChecker.KEY_INCLUDE_PREVIEWS);
    oldPreviews = preferences.getBoolean(UpdateChecker.KEY_INCLUDE_PREVIEWS, true);
    oldLastCheckPresent = preferences.contains(UpdateChecker.KEY_LAST_CHECK);
    oldLastCheck = preferences.getLong(UpdateChecker.KEY_LAST_CHECK, 0L);
    oldPreviewPresent = preferences.contains(UpdateChecker.KEY_LAST_PRERELEASE);
    oldLastPrerelease = preferences.getBoolean(UpdateChecker.KEY_LAST_PRERELEASE, false);
    oldStrings =
        new String[] {
          preferences.getString(UpdateChecker.KEY_LAST_STATUS, null),
          preferences.getString(UpdateChecker.KEY_LAST_VERSION, null),
          preferences.getString(UpdateChecker.KEY_LAST_NAME, null),
          preferences.getString(UpdateChecker.KEY_LAST_RELEASE_URL, null),
          preferences.getString(UpdateChecker.KEY_LAST_DOWNLOAD_URL, null),
          preferences.getString(UpdateChecker.KEY_LAST_ASSET_NAME, null),
          preferences.getString(UpdateChecker.KEY_LAST_CHECKSUM_URL, null),
          preferences.getString(UpdateChecker.KEY_LAST_CHECKSUM, null),
          preferences.getString("updates_last_message", null)
        };
  }

  @After
  public void tearDown() {
    SharedPreferences.Editor editor = preferences.edit();
    editor.remove(UpdateChecker.KEY_AUTO_CHECK).remove(UpdateChecker.KEY_INCLUDE_PREVIEWS);
    editor.remove(UpdateChecker.KEY_LAST_CHECK).remove(UpdateChecker.KEY_LAST_PRERELEASE);
    editor.remove(UpdateChecker.KEY_LAST_STATUS).remove(UpdateChecker.KEY_LAST_VERSION);
    editor.remove(UpdateChecker.KEY_LAST_NAME).remove(UpdateChecker.KEY_LAST_RELEASE_URL);
    editor.remove(UpdateChecker.KEY_LAST_DOWNLOAD_URL).remove(UpdateChecker.KEY_LAST_ASSET_NAME);
    editor.remove(UpdateChecker.KEY_LAST_CHECKSUM_URL).remove(UpdateChecker.KEY_LAST_CHECKSUM);
    editor.remove("updates_last_message");
    if (oldAutoPresent) editor.putBoolean(UpdateChecker.KEY_AUTO_CHECK, oldAuto);
    if (oldPreviewsPresent) editor.putBoolean(UpdateChecker.KEY_INCLUDE_PREVIEWS, oldPreviews);
    if (oldLastCheckPresent) editor.putLong(UpdateChecker.KEY_LAST_CHECK, oldLastCheck);
    if (oldPreviewPresent) editor.putBoolean(UpdateChecker.KEY_LAST_PRERELEASE, oldLastPrerelease);
    String[] keys = {
      UpdateChecker.KEY_LAST_STATUS,
      UpdateChecker.KEY_LAST_VERSION,
      UpdateChecker.KEY_LAST_NAME,
      UpdateChecker.KEY_LAST_RELEASE_URL,
      UpdateChecker.KEY_LAST_DOWNLOAD_URL,
      UpdateChecker.KEY_LAST_ASSET_NAME,
      UpdateChecker.KEY_LAST_CHECKSUM_URL,
      UpdateChecker.KEY_LAST_CHECKSUM,
      "updates_last_message"
    };
    for (int i = 0; i < keys.length; i++) if (oldStrings[i] != null) editor.putString(keys[i], oldStrings[i]);
    editor.apply();
  }

  @Test
  public void manualQueryReachesPublicGithubAndUsesPackageManagerVersion() throws Exception {
    CountDownLatch finished = new CountDownLatch(1);
    AtomicReference<UpdateChecker.Result> result = new AtomicReference<>();
    assertTrue(
        "a manual check should start when no check is running",
        UpdateChecker.check(context, true, value -> {
          result.set(value);
          finished.countDown();
        }));
    assertTrue(
        "GitHub release query timed out",
        finished.await(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    UpdateChecker.Result checked = result.get();
    assertNotNull(checked);
    assertFalse("the public GitHub query must return a useful result", checked.status == UpdateChecker.Result.Status.ERROR);
    assertEquals(UpdateChecker.getInstalledVersionName(context), checked.installedVersion);
  }

  @Test
  public void preferencesPersistAndCachedOfferInvalidatesAfterInstallingVersion() {
    UpdateChecker.setAutoCheckEnabled(context, false);
    UpdateChecker.setIncludePreviews(context, false);
    assertFalse(UpdateChecker.autoCheckEnabled(context));
    assertFalse(UpdateChecker.includePreviews(context));

    preferences.edit()
        .putString(UpdateChecker.KEY_LAST_STATUS, "available")
        .putString(UpdateChecker.KEY_LAST_VERSION, "99.0.0")
        .putString(UpdateChecker.KEY_LAST_NAME, "Future")
        .putString(UpdateChecker.KEY_LAST_DOWNLOAD_URL, "https://github.com/g0dswer/Desperta/releases/download/v99.0.0/Desperta-99.0.0.apk")
        .putString(UpdateChecker.KEY_LAST_ASSET_NAME, "Desperta-99.0.0.apk")
        .apply();
    assertEquals(UpdateChecker.Result.Status.UPDATE_AVAILABLE, UpdateChecker.cachedResult(context).status);

    preferences.edit().putString(UpdateChecker.KEY_LAST_VERSION, UpdateChecker.getInstalledVersionName(context)).apply();
    assertEquals(UpdateChecker.Result.Status.UP_TO_DATE, UpdateChecker.cachedResult(context).status);
  }

  @Test
  public void updateScreenShowsInstalledVersionAndManualControls() {
    try (ActivityScenario<UpdateActivity> scenario = ActivityScenario.launch(UpdateActivity.class)) {
      onView(withText("Atualizações")).check(matches(isDisplayed()));
      onView(withText("Versão instalada")).check(matches(isDisplayed()));
      onView(withText("v" + UpdateChecker.getInstalledVersionName(context))).check(matches(isDisplayed()));
      onView(withContentDescription("Verificar diariamente")).check(matches(isDisplayed()));
    }
  }
}
