package com.desperta;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import java.io.File;

/** Settings page for checking and installing signed Desperta releases from GitHub. */
public final class UpdateActivity extends Activity {
  private int background;
  private int surface;
  private int raised;
  private int primary;
  private int secondary;
  private int accent;
  private int positive;
  private boolean dark;
  private TextView status;
  private TextView details;
  private Button action;
  private Switch autoCheck;
  private Switch previews;
  private UpdateChecker.Result current;
  private File downloadedApk;
  private final android.os.Handler handler =
      new android.os.Handler(android.os.Looper.getMainLooper());
  private int checkGeneration;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    render();
    checkNow();
  }

  private void render() {
    dark = isDarkTheme();
    background = Color.parseColor(dark ? "#101A2A" : "#FAF5EC");
    surface = Color.parseColor(dark ? "#1B2A3D" : "#FFFFFF");
    raised = Color.parseColor(dark ? "#263A52" : "#F1E8D8");
    primary = Color.parseColor(dark ? "#FFF6E7" : "#172033");
    secondary = Color.parseColor(dark ? "#ADB9C9" : "#59606F");
    accent = Color.parseColor(dark ? "#F6B95D" : "#9C5D08");
    positive = Color.parseColor(dark ? "#83D6A3" : "#237747");
    configureWindow();

    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(background);
    scroll.setOnApplyWindowInsetsListener(
        (view, insets) -> {
          scroll.setPadding(
              0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
          return insets;
        });
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20), dp(14), dp(20), dp(36));
    scroll.addView(root, matchWrap());

    root.addView(header());
    root.addView(text("Atualizações", 30, Typeface.BOLD, primary), margins(0, 0, 0, 4));
    root.addView(
        text(
            "O Desperta verifica apenas releases públicos do GitHub. O download só é instalado"
                + " depois da confirmação do Android.",
            15,
            Typeface.NORMAL,
            secondary),
        margins(0, 0, 0, 20));

    LinearLayout installed = card();
    installed.addView(text("Versão instalada", 14, Typeface.BOLD, secondary));
    installed.addView(
        text("v" + UpdateChecker.getInstalledVersionName(this), 28, Typeface.BOLD, primary),
        margins(0, 8, 0, 2));
    installed.addView(
        text("Fonte: github.com/" + UpdateChecker.REPOSITORY, 13, Typeface.NORMAL, secondary));
    root.addView(installed, margins(0, 0, 0, 16));

    root.addView(sectionTitle("Preferências"));
    LinearLayout preferences = card();
    preferences.addView(
        switchRow(
            "Verificar diariamente",
            "Quando o app abrir, busca no máximo uma vez por dia.",
            UpdateChecker.autoCheckEnabled(this),
            checked -> UpdateChecker.setAutoCheckEnabled(this, checked)));
    preferences.addView(divider());
    preferences.addView(
        switchRow(
            "Incluir versões de teste",
            "Também mostra releases marcados como pré-lançamento.",
            UpdateChecker.includePreviews(this),
            checked -> {
              UpdateChecker.setIncludePreviews(this, checked);
              // A cached preview may no longer be suitable; refresh the release list immediately.
              checkNow();
            }));
    root.addView(preferences, margins(0, 0, 0, 18));

    root.addView(sectionTitle("Status"));
    LinearLayout resultCard = card();
    status = text("Verificando…", 18, Typeface.BOLD, primary);
    resultCard.addView(status);
    details = text("", 14, Typeface.NORMAL, secondary);
    resultCard.addView(details, margins(0, 8, 0, 0));
    action = button("Verificar agora", true);
    action.setOnClickListener(v -> checkNow());
    resultCard.addView(action, margins(0, 16, 0, 0));
    root.addView(resultCard, margins(0, 0, 0, 18));

    root.addView(
        text(
            "Cada APK é conferido pelo nome do pacote, pela versão e pela assinatura do Desperta"
                + " antes de abrir o instalador.",
            13,
            Typeface.NORMAL,
            secondary),
        margins(0, 0, 0, 0));
    setContentView(scroll);
    showResult(UpdateChecker.cachedResult(this));
  }

  private View header() {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, 0, 0, dp(16));
    TextView back = text("‹", 38, Typeface.NORMAL, primary);
    back.setGravity(Gravity.CENTER);
    back.setContentDescription("Voltar");
    back.setOnClickListener(v -> finish());
    row.addView(back, fixed(48, 48));
    TextView title = text("Desperta", 22, Typeface.BOLD, primary);
    title.setGravity(Gravity.CENTER);
    row.addView(title, weight(44, 1f));
    row.addView(text("", 22, Typeface.NORMAL, primary), fixed(48, 48));
    return row;
  }

  private void checkNow() {
    if (status == null) return;
    action.setEnabled(false);
    action.setText("Verificando…");
    status.setText("Verificando atualizações…");
    status.setTextColor(primary);
    details.setText("Consultando releases públicos do GitHub.");
    final int generation = ++checkGeneration;
    boolean started =
        UpdateChecker.check(
            this,
            previews == null ? UpdateChecker.includePreviews(this) : previews.isChecked(),
            result -> {
              if (isAlive() && generation == checkGeneration) showResult(result);
            });
    if (!started)
      handler.postDelayed(
          () -> {
            if (isAlive() && generation == checkGeneration) checkNow();
          },
          300L);
  }

  private void showResult(UpdateChecker.Result result) {
    if (result == null || status == null) return;
    current = result;
    action.setEnabled(true);
    if (result.status == UpdateChecker.Result.Status.UPDATE_AVAILABLE && result.release != null) {
      status.setText("Nova versão " + result.release.version + " disponível");
      status.setTextColor(accent);
      String label =
          result.release.name == null || result.release.name.isEmpty()
              ? "Release do GitHub"
              : result.release.name;
      String preview = result.release.prerelease ? "Pré-lançamento · " : "";
      details.setText(preview + label + "\nVocê está usando v" + result.installedVersion + ".");
      action.setText("Baixar atualização");
      action.setOnClickListener(v -> download());
      return;
    }
    if (result.status == UpdateChecker.Result.Status.ERROR) {
      status.setText("Não foi possível verificar agora");
      status.setTextColor(accent);
      details.setText(result.message + "\nConfira a conexão e tente novamente.");
      action.setText("Tentar novamente");
      action.setOnClickListener(v -> checkNow());
      return;
    }
    status.setText("Você já está na versão mais recente");
    status.setTextColor(positive);
    details.setText("Versão instalada: v" + result.installedVersion + ".");
    action.setText("Verificar novamente");
    action.setOnClickListener(v -> checkNow());
  }

  private void download() {
    if (current == null || current.release == null) return;
    action.setEnabled(false);
    action.setText("Baixando…");
    status.setText("Baixando atualização…");
    details.setText("O download será conferido antes de oferecer a instalação.");
    final UpdateChecker.ReleaseInfo release = current.release;
    UpdateChecker.downloadAsync(
        this,
        release,
        (downloaded, total) -> {
          if (isAlive() && total > 0 && action != null) {
            int percent = (int) Math.min(100L, downloaded * 100L / total);
            action.setText("Baixando " + percent + "%");
          }
        },
        file -> {
          if (!isAlive()) return;
          downloadedApk = file;
          status.setText("Download concluído");
          status.setTextColor(positive);
          details.setText("O APK foi conferido. O Android pedirá confirmação antes da instalação.");
          action.setEnabled(true);
          action.setText("Instalar atualização");
          action.setOnClickListener(v -> installDownloaded());
        },
        error -> {
          if (!isAlive()) return;
          status.setText("Download não concluído");
          status.setTextColor(accent);
          details.setText(error == null ? "Tente novamente." : error.getMessage());
          action.setEnabled(true);
          action.setText("Tentar download");
          action.setOnClickListener(v -> download());
        });
  }

  private void installDownloaded() {
    if (downloadedApk == null || !downloadedApk.exists()) {
      showResult(current);
      return;
    }
    try {
      if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
        Intent settings =
            new Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + getPackageName()));
        startActivity(settings);
        details.setText(
            "Permita instalações desta fonte e volte para tocar em Instalar atualização.");
        return;
      }
      Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", downloadedApk);
      Intent install =
          new Intent(Intent.ACTION_VIEW)
              .setDataAndType(uri, "application/vnd.android.package-archive");
      install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(install);
    } catch (RuntimeException error) {
      details.setText("Não foi possível abrir o instalador. Tente baixar novamente.");
    }
  }

  private LinearLayout switchRow(
      String title, String description, boolean checked, final CheckedAction listener) {
    LinearLayout row = new LinearLayout(this);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(10), 0, dp(10));
    LinearLayout copy = new LinearLayout(this);
    copy.setOrientation(LinearLayout.VERTICAL);
    copy.addView(text(title, 16, Typeface.BOLD, primary));
    copy.addView(text(description, 13, Typeface.NORMAL, secondary));
    row.addView(copy, weightWrap());
    Switch value = new Switch(this);
    value.setChecked(checked);
    value.setContentDescription(title);
    value.setOnCheckedChangeListener((button, isChecked) -> listener.accept(isChecked));
    row.addView(value, fixed(56, 48));
    if ("Verificar diariamente".equals(title)) autoCheck = value;
    if ("Incluir versões de teste".equals(title)) previews = value;
    return row;
  }

  private interface CheckedAction {
    void accept(boolean checked);
  }

  private LinearLayout card() {
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(16), dp(12), dp(16), dp(14));
    card.setBackground(round(surface, 20));
    return card;
  }

  private TextView sectionTitle(String value) {
    return text(value, 14, Typeface.BOLD, accent, 4, 8, 4, 7);
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
    view.setGravity(Gravity.CENTER_VERTICAL);
    view.setPadding(dp(left), dp(top), dp(right), dp(bottom));
    return view;
  }

  private Button button(String label, boolean filled) {
    Button value = new Button(this);
    value.setText(label);
    value.setTextSize(15);
    value.setAllCaps(false);
    value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    value.setTextColor(filled ? (dark ? Color.parseColor("#172033") : Color.WHITE) : primary);
    value.setMinHeight(dp(52));
    value.setMinWidth(0);
    value.setPadding(dp(14), dp(10), dp(14), dp(10));
    value.setBackground(round(filled ? accent : raised, 14));
    return value;
  }

  private View divider() {
    View value = new View(this);
    value.setBackgroundColor(Color.parseColor(dark ? "#30425A" : "#E8DDCB"));
    value.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
    return value;
  }

  private GradientDrawable round(int color, int radius) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(dp(radius));
    return drawable;
  }

  private ViewGroup.LayoutParams matchWrap() {
    return new ViewGroup.LayoutParams(-1, -2);
  }

  private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
    return params;
  }

  private LinearLayout.LayoutParams fixed(int width, int height) {
    return new LinearLayout.LayoutParams(dp(width), dp(height));
  }

  private LinearLayout.LayoutParams weight(int height, float value) {
    return new LinearLayout.LayoutParams(0, dp(height), value);
  }

  private LinearLayout.LayoutParams weightWrap() {
    return new LinearLayout.LayoutParams(0, -2, 1f);
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private boolean isDarkTheme() {
    String value = getSharedPreferences(Weather.PREFS, MODE_PRIVATE).getString("theme", "dark");
    if ("light".equals(value)) return false;
    if ("system".equals(value)) {
      int mode =
          getResources().getConfiguration().uiMode
              & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
      return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
    return true;
  }

  private void configureWindow() {
    getWindow().setStatusBarColor(dark ? background : background);
    getWindow().setNavigationBarColor(dark ? background : background);
    int flags = 0;
    if (!dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    if (!dark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    getWindow().getDecorView().setSystemUiVisibility(flags);
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    super.onDestroy();
  }

  private boolean isAlive() {
    return !isFinishing() && (Build.VERSION.SDK_INT < 17 || !isDestroyed());
  }
}
