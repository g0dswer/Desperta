package com.desperta;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Optional, user-controlled device-admin integration.
 *
 * <p>Android requires the user to approve activation in a system screen. This receiver deliberately
 * requests no destructive policy: a regular application cannot reliably prevent a user from
 * powering a phone off or uninstalling it, and Desperta does not claim to do either. The
 * active-admin state can be removed from the settings screen exposed by {@link #remove(Context)}.
 */
public final class ProtectionAdmin extends DeviceAdminReceiver {
  public static final int REQUEST_ENABLE = 7_401;

  /** Shared with Store and the settings screen. */
  public static final String PREFS = "desperta";

  public static final String KEY_ADMIN_ENABLED = "protection_admin_enabled";

  public static ComponentName component(Context context) {
    return new ComponentName(context.getApplicationContext(), ProtectionAdmin.class);
  }

  public static boolean isActive(Context context) {
    if (context == null) return false;
    DevicePolicyManager manager =
        (DevicePolicyManager)
            context.getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
    return manager != null && manager.isAdminActive(component(context));
  }

  /** Starts Android's consent screen. No activation happens silently. */
  public static boolean requestActivation(Activity activity) {
    if (activity == null) return false;
    Intent intent =
        new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(activity))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Permite manter os alarmes confiáveis. Você pode remover esta permissão a qualquer"
                    + " momento. O Android não permite que um app comum impeça o desligamento do"
                    + " aparelho.");
    try {
      activity.startActivityForResult(intent, REQUEST_ENABLE);
      return true;
    } catch (RuntimeException error) {
      return false;
    }
  }

  /**
   * Requests removal of this app's admin registration. Android may still ask the user to confirm in
   * system settings on some OEM builds.
   */
  public static boolean remove(Context context) {
    if (context == null) return false;
    Context app = context.getApplicationContext();
    DevicePolicyManager manager =
        (DevicePolicyManager) app.getSystemService(Context.DEVICE_POLICY_SERVICE);
    if (manager == null || !manager.isAdminActive(component(app))) return true;
    try {
      manager.removeActiveAdmin(component(app));
    } catch (SecurityException ignored) {
      // OEMs can require the user-facing settings page; the caller can
      // use openSettings(Context) as a safe fallback.
      return false;
    }
    return !manager.isAdminActive(component(app));
  }

  /** Opens the Android security settings where active administrators can be managed. */
  public static boolean openSettings(Context context) {
    if (context == null) return false;
    Intent intent = new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try {
      context.startActivity(intent);
      return true;
    } catch (RuntimeException error) {
      return false;
    }
  }

  @Override
  public void onEnabled(Context context, Intent intent) {
    super.onEnabled(context, intent);
    context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_ADMIN_ENABLED, true)
        .apply();
  }

  @Override
  public void onDisabled(Context context, Intent intent) {
    super.onDisabled(context, intent);
    context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_ADMIN_ENABLED, false)
        .apply();
  }

  @Override
  public CharSequence onDisableRequested(Context context, Intent intent) {
    return "A proteção opcional será removida. Seus alarmes continuarão disponíveis.";
  }
}
