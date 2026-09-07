package com.desperta;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.util.*;
import org.json.*;

/** Reusable local code library; alarm selection is committed only on Complete. */
public class BarcodeLibraryActivity extends Activity {
  private final ArrayList<String> codes = new ArrayList<>();
  private final ArrayList<String> selected = new ArrayList<>();
  private static final String KEY = "barcode_library";
  private String feedback = "";
  private Identity theme;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    theme = Identity.current(this);
    theme.applyWindow(this);
    try {
      JSONArray saved = new JSONArray(Store.prefs(this).getString(KEY, "[]"));
      for (int i = 0; i < saved.length(); i++) addUnique(codes, saved.getString(i));
    } catch (JSONException ignored) {
    }
    if (!Store.prefs(this).getBoolean("barcode_library_migrated", false)) {
      for (Alarm alarm : Store.all(this))
        for (Alarm.Mission mission : alarm.missions)
          if ("barcode".equals(mission.type))
            for (String code : mission.acceptedCodes()) addUnique(codes, code);
      Store.prefs(this).edit().putBoolean("barcode_library_migrated", true).commit();
    }
    ArrayList<String> incoming =
        state == null
            ? getIntent().getStringArrayListExtra("targets")
            : state.getStringArrayList("selected");
    if (incoming != null)
      for (String code : incoming) {
        addUnique(selected, code);
        addUnique(codes, code);
      }
    if (state != null) feedback = state.getString("feedback", "");
    persist();
    render();
  }

  private static void addUnique(List<String> list, String value) {
    if (value != null && !value.isEmpty() && !list.contains(value)) list.add(value);
  }

  private void persist() {
    Store.prefs(this).edit().putString(KEY, new JSONArray(codes).toString()).commit();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putStringArrayList("selected", selected);
    state.putString("feedback", feedback);
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private TextView label(String text, int size) {
    TextView view = new TextView(this);
    view.setText(text);
    view.setTextColor(theme.fg);
    view.setTextSize(size);
    view.setPadding(0, dp(10), 0, dp(10));
    return view;
  }

  /** Keeps the identity-specific panel grammar while adding the selection outline. */
  private Drawable codeRowBackground(boolean checked) {
    Drawable panel = theme.panel(this);
    if (!checked) return panel;
    GradientDrawable outline = new GradientDrawable();
    outline.setColor(Color.TRANSPARENT);
    outline.setCornerRadius(dp(14));
    outline.setStroke(dp(2), theme.accent);
    return new LayerDrawable(new Drawable[] {panel, outline});
  }

  private Button button(String text, Runnable action) {
    Button button = new Button(this);
    button.setText(text);
    button.setAllCaps(false);
    button.setTextColor(theme.onAccent);
    button.setBackground(theme.primary(this));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(56));
    p.setMargins(0, dp(8), 0, dp(8));
    button.setLayoutParams(p);
    button.setOnClickListener(v -> action.run());
    return button;
  }

  private void render() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(22), dp(16), dp(22), dp(16));
    root.setBackground(theme.background(this));
    root.setOnApplyWindowInsetsListener(
        (v, in) -> {
          root.setPadding(
              dp(22),
              in.getSystemWindowInsetTop() + dp(16),
              dp(22),
              in.getSystemWindowInsetBottom() + dp(16));
          return in;
        });
    setContentView(root);
    Button heading = button("‹  QR / Código de barras", this::finish);
    theme.styleText(heading, true);
    heading.setBackgroundColor(Color.TRANSPARENT);
    heading.setTextColor(theme.fg);
    heading.setTextSize(20);
    root.addView(heading);
    root.addView(
        button(
            "＋  Adicionar código",
            () ->
                startActivityForResult(
                    new Intent(this, MissionActivity.class)
                        .putExtra("type", "barcode")
                        .putExtra("mode", "register"),
                    1)));
    TextView hint =
        label(
            "Selecione os códigos aceitos. Escanear qualquer um dos selecionados conclui esta"
                + " missão.",
            14);
    root.addView(hint);
    ScrollView scroll = new ScrollView(this);
    LinearLayout list = new LinearLayout(this);
    list.setOrientation(LinearLayout.VERTICAL);
    scroll.addView(list);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    if (codes.isEmpty())
      list.addView(label("Nenhum código cadastrado. Toque em Adicionar código para escanear.", 16));
    for (String code : new ArrayList<>(codes)) {
      LinearLayout row = new LinearLayout(this);
      row.setGravity(Gravity.CENTER_VERTICAL);
      row.setPadding(dp(10), dp(8), dp(10), dp(8));
      row.setBackground(codeRowBackground(selected.contains(code)));
      LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
      rp.setMargins(0, dp(6), 0, dp(6));
      list.addView(row, rp);
      CheckBox check = new CheckBox(this);
      check.setText(code);
      check.setTextColor(theme.fg);
      check.setTextSize(18);
      check.setChecked(selected.contains(code));
      row.addView(check, new LinearLayout.LayoutParams(0, -2, 1));
      check.setOnCheckedChangeListener(
          (v, on) -> {
            if (on) addUnique(selected, code);
            else selected.remove(code);
            render();
          });
      Button menu =
          button(
              "⋮",
              () ->
                  theme
                      .dialog(this)
                      .setTitle("Código cadastrado")
                      .setItems(
                          new String[] {"Revisar código", "Excluir código"},
                          (dialog, item) -> {
                            if (item == 0)
                              theme
                                  .dialog(this)
                                  .setTitle("QR / Código de barras")
                                  .setMessage(code)
                                  .setPositiveButton("Fechar", null)
                                  .show();
                            else
                              theme
                                  .dialog(this)
                                  .setTitle("Excluir código?")
                                  .setMessage(
                                      "Remover da biblioteca e desta seleção? Outros alarmes salvos"
                                          + " mantêm seus códigos.")
                                  .setNegativeButton("Cancelar", null)
                                  .setPositiveButton(
                                      "Excluir",
                                      (d, which) -> {
                                        codes.remove(code);
                                        selected.remove(code);
                                        persist();
                                        render();
                                      })
                                  .show();
                          })
                      .show());
      menu.setBackgroundColor(Color.TRANSPARENT);
      menu.setTextColor(theme.fg);
      menu.setContentDescription("Opções do código " + code);
      row.addView(menu, new LinearLayout.LayoutParams(dp(48), dp(48)));
    }
    if (!feedback.isEmpty()) root.addView(label(feedback, 15));
    Button preview =
        button(
            "Testar leitura",
            () ->
                startActivityForResult(
                    new Intent(this, MissionActivity.class)
                        .putExtra("type", "barcode")
                        .putExtra("mode", "solve")
                        .putExtra("preview", true)
                        .putExtra("target", selected.get(0))
                        .putStringArrayListExtra("targets", new ArrayList<>(selected)),
                    2));
    preview.setEnabled(!selected.isEmpty());
    LinearLayout footer = new LinearLayout(this);
    preview.setBackground(theme.secondary(this));
    preview.setTextColor(theme.fg);
    LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, dp(56), 1);
    pp.setMargins(0, dp(8), dp(8), dp(8));
    footer.addView(preview, pp);
    Button complete =
        button(
            "Concluir",
            () -> {
              setResult(
                  RESULT_OK,
                  new Intent().putStringArrayListExtra("targets", new ArrayList<>(selected)));
              finish();
            });
    complete.setEnabled(!selected.isEmpty());
    footer.addView(complete, new LinearLayout.LayoutParams(0, dp(56), 1.5f));
    footer.setGravity(Gravity.CENTER_VERTICAL);
    root.addView(footer);
    if (getIntent().getBooleanExtra("editing", false))
      root.addView(
          button(
              "Remover missão deste alarme",
              () -> {
                setResult(RESULT_OK, new Intent().putExtra("remove", true));
                finish();
              }));
    theme.applyTree(root);
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request == 1 && result == RESULT_OK && data != null) {
      String code = data.getStringExtra("target");
      if (code != null && !code.isEmpty()) {
        feedback =
            codes.contains(code)
                ? "Código já cadastrado e selecionado."
                : "Código cadastrado e selecionado.";
        addUnique(codes, code);
        addUnique(selected, code);
        persist();
      }
    } else if (request == 2)
      feedback =
          result == RESULT_OK
              ? "Leitura confirmada. Este código conclui a missão."
              : "Teste cancelado. A seleção foi mantida.";
    render();
  }
}
