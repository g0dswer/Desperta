package com.desperta;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Settings screen for the free, local-first Desperta alarm app. */
public class SettingsActivity extends Activity {
  private static final String PREFS = Weather.PREFS;
  private static final String KEY_OUTPUT = "output";
  private static final int REQUEST_POST_NOTIFICATIONS = 7_402;

  private TextView exactStatus;
  private TextView notificationsStatus;
  private TextView fullscreenStatus;
  private TextView batteryStatus;
  private TextView protectionStatus;
  private TextView weatherResult;
  private EditText cityInput;

  private int background;
  private int surface;
  private int surfaceRaised;
  private int primaryText;
  private int secondaryText;
  private int accent;
  private int pink;
  private int positive;
  private boolean dark;
  private Identity identity;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    render();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (identity != null && !identity.id.equals(Identity.current(this).id)) {
      render();
      return;
    }
    updateOptimizationStatuses();
    updateProtectionStatus();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    updateOptimizationStatuses();
    updateProtectionStatus();
  }

  private void render() {
    identity = Identity.current(this);
    dark = !identity.light;
    background = identity.bg;
    surface = identity.surface;
    surfaceRaised = identity.raised;
    primaryText = identity.fg;
    secondaryText = identity.muted;
    accent = identity.accent;
    pink = identity.error;
    positive = identity.positive;
    identity.applyWindow(this);

    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setOnApplyWindowInsetsListener(
        (view, insets) -> {
          scroll.setPadding(
              0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
          return insets;
        });
    scroll.setBackground(identity.background(this));

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20), dp(14), dp(20), dp(36));
    scroll.addView(
        root,
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    root.addView(header());
    TextView intro = text("Desperta", 30, Typeface.BOLD, primaryText);
    root.addView(intro, lp(0, 0, 0, 4));
    root.addView(
        text(
            "Alarme gratuito, sem anúncios e sem login. Seus alarmes ficam no aparelho.",
            15,
            Typeface.NORMAL,
            secondaryText),
        lp(0, 0, 0, 20));

    root.addView(sectionTitle("Desperta"));
    root.addView(
        row(
            "Atualizações",
            UpdateChecker.summary(this),
            "Verifique versões públicas do GitHub e instale somente após a confirmação do Android.",
            v -> openUpdateActivity()),
        lp(0, 0, 0, 18));

    root.addView(sectionTitle("Preferências"));
    LinearLayout preferences = card();
    preferences.addView(
        row("Identidade", identity.name, identity.description(), v -> chooseIdentity()));
    preferences.addView(divider());
    preferences.addView(
        row(
            "Saída de som",
            outputName(),
            "Alto-falante do aparelho ou dispositivo conectado",
            v -> chooseOutput()));
    root.addView(preferences, lp(0, 0, 0, 18));

    root.addView(sectionTitle("Clima para o lembrete"));
    root.addView(weatherCard(), lp(0, 0, 0, 18));

    root.addView(sectionTitle("Otimização do alarme"));
    TextView optimizationIntro =
        text(
            "Estas verificações ajudam o Android a entregar alarmes no horário. Toque em uma linha"
                + " para abrir a configuração correspondente.",
            14,
            Typeface.NORMAL,
            secondaryText);
    root.addView(optimizationIntro, lp(0, 0, 0, 10));
    LinearLayout optimization = card();
    exactStatus = statusText();
    notificationsStatus = statusText();
    fullscreenStatus = statusText();
    batteryStatus = statusText();
    optimization.addView(
        optimizationRow(
            "Alarmes exatos",
            "Permite tocar no horário escolhido",
            exactStatus,
            v -> openExactAlarmSettings()));
    optimization.addView(divider());
    optimization.addView(
        optimizationRow(
            "Notificações",
            "Necessárias para mostrar o alarme",
            notificationsStatus,
            v -> openNotificationSettings()));
    optimization.addView(divider());
    optimization.addView(
        optimizationRow(
            "Tela cheia no alarme",
            "Permite exibir o despertador sobre a tela bloqueada",
            fullscreenStatus,
            v -> openFullscreenSettings()));
    optimization.addView(divider());
    optimization.addView(
        optimizationRow(
            "Economia de bateria",
            "Remove a restrição que pode atrasar alarmes",
            batteryStatus,
            v -> openBatterySettings()));
    root.addView(optimization, lp(0, 0, 0, 18));

    root.addView(sectionTitle("Proteção avançada"));
    root.addView(protectionCard(), lp(0, 0, 0, 18));

    root.addView(sectionTitle("Privacidade"));
    LinearLayout privacy = card();
    privacy.addView(
        text(
            "Os alarmes, missões e preferências são guardados localmente. A consulta de clima envia"
                + " somente a cidade para o Open-Meteo quando você pede uma atualização ou quando"
                + " um alarme com lembrete de clima precisa atualizar a previsão. Se você usar"
                + " leitura em voz alta, o Android poderá usar o serviço de síntese de voz"
                + " escolhido no aparelho.",
            14,
            Typeface.NORMAL,
            secondaryText));
    Button clearWeather = button("Limpar cidade e clima salvos", false);
    clearWeather.setOnClickListener(
        v -> {
          Weather.clear(this);
          if (cityInput != null) cityInput.setText("");
          updateWeatherText();
        });
    privacy.addView(clearWeather, lp(0, 14, 0, 0));
    root.addView(privacy, lp(0, 0, 0, 22));

    root.addView(
        text(
            "Desligar o telefone à força não pode ser impedido por um aplicativo Android comum."
                + " Desperta não simula esse controle.",
            13,
            Typeface.NORMAL,
            secondaryText),
        lp(0, 0, 0, 0));

    setContentView(scroll);
    identity.applyTree(scroll);
    updateOptimizationStatuses();
    updateProtectionStatus();
    updateWeatherText();
  }

  private View header() {
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setPadding(0, 0, 0, dp(16));

    TextView back = text("‹", 38, Typeface.NORMAL, primaryText);
    back.setGravity(Gravity.CENTER);
    back.setContentDescription("Voltar");
    back.setOnClickListener(v -> finish());
    header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

    TextView title = text("Configurações", 22, Typeface.BOLD, primaryText);
    title.setGravity(Gravity.CENTER);
    header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));

    TextView empty = text("", 22, Typeface.NORMAL, primaryText);
    header.addView(empty, new LinearLayout.LayoutParams(dp(48), dp(48)));
    return header;
  }

  private LinearLayout weatherCard() {
    LinearLayout card = card();
    card.addView(
        text(
            "Informe uma cidade para usar no lembrete de clima do alarme.",
            14,
            Typeface.NORMAL,
            secondaryText),
        lp(0, 0, 0, 10));

    LinearLayout inputRow = new LinearLayout(this);
    inputRow.setGravity(Gravity.CENTER_VERTICAL);
    cityInput = new EditText(this);
    cityInput.setSingleLine(true);
    cityInput.setHint("Ex.: São Paulo");
    cityInput.setHintTextColor(secondaryText);
    cityInput.setTextColor(primaryText);
    cityInput.setTextSize(16);
    cityInput.setPadding(dp(14), 0, dp(14), 0);
    cityInput.setBackground(identity.secondary(this));
    cityInput.setText(Weather.getCity(this));
    inputRow.addView(cityInput, new LinearLayout.LayoutParams(0, dp(50), 1f));

    Button refresh = button("Atualizar", true);
    refresh.setOnClickListener(v -> refreshWeather());
    LinearLayout.LayoutParams refreshParams =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(50));
    refreshParams.setMargins(dp(8), 0, 0, 0);
    inputRow.addView(refresh, refreshParams);
    card.addView(inputRow);

    weatherResult = text("", 14, Typeface.NORMAL, secondaryText);
    weatherResult.setPadding(0, dp(12), 0, 0);
    card.addView(weatherResult);
    return card;
  }

  private LinearLayout protectionCard() {
    LinearLayout card = card();
    card.addView(
        text(
            "A proteção avançada é opcional. Se ativada, ela registra Desperta como administrador"
                + " do aparelho após uma confirmação do Android. Não bloqueia o desligamento"
                + " forçado e pode ser removida a qualquer momento.",
            14,
            Typeface.NORMAL,
            secondaryText),
        lp(0, 0, 0, 12));
    card.addView(
        text(
            "Desinstalação: o Android não permite que um app comum a bloqueie. O administrador"
                + " opcional também não cria um bloqueio irreversível.",
            14,
            Typeface.NORMAL,
            secondaryText),
        lp(0, 0, 0, 12));

    protectionStatus = text("", 15, Typeface.BOLD, primaryText);
    protectionStatus.setPadding(0, 0, 0, dp(10));
    card.addView(protectionStatus);

    LinearLayout actions = new LinearLayout(this);
    actions.setGravity(Gravity.CENTER_VERTICAL);
    Button enable = button("Ativar proteção", true);
    enable.setBackground(roundBackground(pink, 14));
    enable.setTextColor(readableTextColor(pink));
    enable.setOnClickListener(v -> ProtectionAdmin.requestActivation(this));
    actions.addView(enable, new LinearLayout.LayoutParams(0, dp(48), 1f));
    Button remove = button("Remover", false);
    remove.setOnClickListener(
        v -> {
          if (!ProtectionAdmin.remove(this)) {
            ProtectionAdmin.openSettings(this);
          }
          updateProtectionStatus();
        });
    LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
    removeParams.setMargins(dp(8), 0, 0, 0);
    actions.addView(remove, removeParams);
    card.addView(actions);
    return card;
  }

  private LinearLayout optimizationRow(
      String title, String description, TextView status, View.OnClickListener listener) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.VERTICAL);
    row.setPadding(0, dp(12), 0, dp(12));
    row.setClickable(true);
    row.setFocusable(true);
    row.setOnClickListener(listener);

    LinearLayout heading = new LinearLayout(this);
    heading.setGravity(Gravity.CENTER_VERTICAL);
    TextView titleView = text(title, 16, Typeface.BOLD, primaryText);
    heading.addView(titleView, new LinearLayout.LayoutParams(0, dp(28), 1f));
    heading.addView(
        status, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(28)));
    row.addView(heading);
    row.addView(text(description, 13, Typeface.NORMAL, secondaryText));
    return row;
  }

  private LinearLayout row(
      String title, String value, String description, View.OnClickListener listener) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.VERTICAL);
    row.setPadding(0, dp(14), 0, dp(14));
    row.setClickable(true);
    row.setFocusable(true);
    row.setOnClickListener(listener);

    LinearLayout heading = new LinearLayout(this);
    heading.setGravity(Gravity.CENTER_VERTICAL);
    heading.addView(
        text(title, 16, Typeface.BOLD, primaryText), new LinearLayout.LayoutParams(0, dp(28), 1f));
    TextView valueView = text(value, 15, Typeface.NORMAL, accent);
    valueView.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
    heading.addView(
        valueView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(28)));
    row.addView(heading);
    row.addView(text(description, 13, Typeface.NORMAL, secondaryText));
    return row;
  }

  private TextView statusText() {
    TextView view = text("…", 14, Typeface.BOLD, secondaryText);
    view.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
    return view;
  }

  private TextView sectionTitle(String title) {
    return text(title, 14, Typeface.BOLD, accent, dp(4), dp(8), dp(4), dp(7));
  }

  private TextView text(String value, float size, int style, int color) {
    return text(value, size, style, color, 0, 0, 0, 0);
  }

  private TextView text(
      String value, float size, int style, int color, int left, int top, int right, int bottom) {
    TextView view = new TextView(this);
    view.setText(value);
    view.setTextSize(size);
    view.setTextColor(color);
    view.setTypeface(Typeface.DEFAULT, style);
    identity.styleText(view, size >= 20);
    view.setGravity(Gravity.CENTER_VERTICAL);
    view.setPadding(left, top, right, bottom);
    return view;
  }

  private Button button(String label, boolean filled) {
    Button button = new Button(this);
    button.setText(label);
    button.setTextSize(14);
    button.setAllCaps(false);
    button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    button.setTextColor(filled ? identity.onAccent : primaryText);
    button.setMinHeight(0);
    button.setMinWidth(0);
    button.setPadding(dp(12), 0, dp(12), 0);
    button.setBackground(filled ? identity.primary(this) : identity.secondary(this));
    return button;
  }

  private LinearLayout card() {
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(16), dp(12), dp(16), dp(14));
    card.setBackground(identity.panel(this));
    return card;
  }

  private View divider() {
    View divider = new View(this);
    divider.setBackgroundColor(identity.muted);
    divider.setLayoutParams(
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
    return divider;
  }

  private GradientDrawable roundBackground(int color, int radiusDp) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(dp(radiusDp));
    return drawable;
  }

  private int readableTextColor(int backgroundColor) {
    double luminance = relativeLuminance(backgroundColor);
    double blackContrast = (luminance + 0.05d) / 0.05d;
    double whiteContrast = 1.05d / (luminance + 0.05d);
    return blackContrast >= whiteContrast ? Color.BLACK : Color.WHITE;
  }

  private double relativeLuminance(int color) {
    double red = linearChannel(Color.red(color) / 255d);
    double green = linearChannel(Color.green(color) / 255d);
    double blue = linearChannel(Color.blue(color) / 255d);
    return 0.2126d * red + 0.7152d * green + 0.0722d * blue;
  }

  private double linearChannel(double channel) {
    return channel <= 0.03928d ? channel / 12.92d : Math.pow((channel + 0.055d) / 1.055d, 2.4d);
  }

  private LinearLayout.LayoutParams lp(int left, int top, int right, int bottom) {
    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
    return params;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private String outputName() {
    String value = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_OUTPUT, "device");
    return "speaker".equals(value) ? "Alto-falante" : "Dispositivo atual";
  }

  private void chooseIdentity() {
    Identity selectedIdentity = Identity.current(this);
    int selected = indexOf(Identity.IDS, selectedIdentity.id);
    String[] choices = new String[Identity.NAMES.length];
    for (int i = 0; i < choices.length; i++) {
      choices[i] = Identity.NAMES[i] + "\n" + Identity.DESCRIPTIONS[i];
    }
    selectedIdentity
        .dialog(this)
        .setTitle("Identidade visual")
        .setSingleChoiceItems(
            choices,
            selected,
            (dialog, which) -> {
              Identity.set(this, Identity.IDS[which]);
              dialog.dismiss();
              render();
            })
        .setNegativeButton("Cancelar", null)
        .show();
  }

  private void chooseOutput() {
    String current = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_OUTPUT, "device");
    String[] keys = {"device", "speaker"};
    String[] labels = {"Dispositivo atual", "Alto-falante"};
    int selected = indexOf(keys, current);
    identity
        .dialog(this)
        .setTitle("Saída de som")
        .setSingleChoiceItems(
            labels,
            selected,
            (dialog, which) -> {
              getSharedPreferences(PREFS, MODE_PRIVATE)
                  .edit()
                  .putString(KEY_OUTPUT, keys[which])
                  // AlarmService accepts this explicit alias as well.
                  .putString("output_route", keys[which])
                  .apply();
              dialog.dismiss();
              render();
            })
        .setNegativeButton("Cancelar", null)
        .show();
  }

  private int indexOf(String[] values, String value) {
    for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i;
    return 0;
  }

  private void refreshWeather() {
    String city = cityInput == null ? "" : cityInput.getText().toString().trim();
    if (cityInput != null) cityInput.clearFocus();
    Weather.refresh(
        this,
        city,
        () -> {
          if (!isFinishing()) updateWeatherText();
        });
    if (weatherResult != null) weatherResult.setText("Atualizando clima…");
  }

  private void updateWeatherText() {
    if (weatherResult == null) return;
    String value = Weather.getCachedText(this);
    long time = Weather.getCachedTime(this);
    if (value == null || value.trim().isEmpty()) {
      weatherResult.setText("Nenhuma consulta salva.");
      return;
    }
    if (time > 0) {
      String when =
          DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
              .format(new Date(time));
      weatherResult.setText(value + "\nAtualizado em " + when);
    } else {
      weatherResult.setText(value);
    }
  }

  private void updateOptimizationStatuses() {
    if (exactStatus == null) return;
    setStatus(exactStatus, exactAlarmsAllowed());
    setStatus(notificationsStatus, notificationsAllowed());
    setStatus(fullscreenStatus, fullscreenAllowed());
    setStatus(batteryStatus, batteryAllowed());
  }

  private void setStatus(TextView view, boolean okay) {
    view.setText(okay ? "Pronto" : "Revisar");
    view.setTextColor(okay ? positive : identity.error);
  }

  private boolean exactAlarmsAllowed() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
    AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
    return manager != null && manager.canScheduleExactAlarms();
  }

  private boolean notificationsAllowed() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true;
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    return manager != null && manager.areNotificationsEnabled();
  }

  private boolean fullscreenAllowed() {
    if (Build.VERSION.SDK_INT < 34) return true;
    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    return manager != null && manager.canUseFullScreenIntent();
  }

  private boolean batteryAllowed() {
    PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
        || (manager != null && manager.isIgnoringBatteryOptimizations(getPackageName()));
  }

  private void openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
    open(
        new Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:" + getPackageName())));
  }

  private void openNotificationSettings() {
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(
          new String[] {Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
      return;
    }
    Intent intent =
        new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
    open(intent);
  }

  private void openFullscreenSettings() {
    if (Build.VERSION.SDK_INT >= 34) {
      Intent intent =
          new Intent(
              Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
              Uri.parse("package:" + getPackageName()));
      if (open(intent)) return;
    }
    openNotificationSettings();
  }

  private void openBatterySettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
    Intent request =
        new Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:" + getPackageName()));
    if (!open(request)) open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
  }

  private boolean open(Intent intent) {
    try {
      startActivity(intent);
      return true;
    } catch (RuntimeException error) {
      return false;
    }
  }

  private void updateProtectionStatus() {
    if (protectionStatus == null) return;
    boolean active = ProtectionAdmin.isActive(this);
    protectionStatus.setText(
        active
            ? "Ativa · pode ser removida pelo botão abaixo"
            : "Desativada · nenhuma permissão especial está ativa");
    protectionStatus.setTextColor(active ? positive : secondaryText);
  }

  private void openUpdateActivity() {
    startActivity(new Intent(this, UpdateActivity.class));
  }

  private void configureWindow() {
    identity.applyWindow(this);
  }
}
