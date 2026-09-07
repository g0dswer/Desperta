package com.desperta;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
  int bg, card, fg, muted, accent, tint;
  boolean light;
  ScrollView scroll;
  final Set<String> expanded = new HashSet<>();
  Alarm undoAlarm;
  String undoMessage = "";
  int previewMissionIndex = -1;
  Alarm.Mission pendingMission;
  boolean guidedPending;
  int renderedDraftId = -1;
  boolean foreground;
  final android.content.SharedPreferences.OnSharedPreferenceChangeListener updateListener =
      (prefs, key) -> {
        if (foreground && this.draft == null && UpdateChecker.KEY_LAST_CHECK.equals(key)) render();
      };
  LinearLayout page, body;
  Alarm draft;
  int barcodeEditIndex = -1;
  String tab = "Alarmes", registerType = "";
  android.media.MediaPlayer sample;
  android.speech.tts.TextToSpeech speech;
  final String[] types = {
    "barcode", "math", "typing", "colors", "steps", "shake", "photo", "squat", "object", "rhythm"
  };
  final String[] names = {
    "QR / Código de barras",
    "Matemática",
    "Digitação",
    "Encontrar a cor",
    "Passos",
    "Sacudir",
    "Foto",
    "Agachamento",
    "Encontrar objeto",
    "Falar no ritmo"
  };

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    if (b != null && b.containsKey("draft"))
      try {
        draft = Alarm.from(new JSONObject(b.getString("draft")));
      } catch (Exception ignored) {
      }
    if (b != null) {
      registerType = b.getString("registerType", "");
      barcodeEditIndex = b.getInt("barcodeEditIndex", -1);
      expanded.addAll(
          b.getStringArrayList("expanded") == null
              ? new ArrayList<>()
              : b.getStringArrayList("expanded"));
      previewMissionIndex = b.getInt("previewMissionIndex", -1);
      guidedPending = b.getBoolean("guidedPending", false);
      undoMessage = b.getString("undoMessage", "");
      if (b.containsKey("undoAlarm")) {
        try {
          undoAlarm = Alarm.from(new JSONObject(b.getString("undoAlarm")));
        } catch (Exception ignored) {
        }
      }
      if (b.containsKey("pendingMission")) {
        try {
          Alarm holder = Alarm.from(new JSONObject(b.getString("pendingMission")));
          pendingMission = holder.missions.get(0);
        } catch (Exception ignored) {
        }
      }
    }
    render();
  }

  @Override
  protected void onResume() {
    super.onResume();
    foreground = true;
    Store.prefs(this).registerOnSharedPreferenceChangeListener(updateListener);
    if (page != null) render();
    if (draft == null && Store.getSession(this) == null) UpdateChecker.maybeCheckDaily(this);
  }

  @Override
  protected void onPause() {
    foreground = false;
    Store.prefs(this).unregisterOnSharedPreferenceChangeListener(updateListener);
    stopSample();
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    if (draft != null) b.putString("draft", draft.json().toString());
    b.putString("registerType", registerType);
    b.putInt("barcodeEditIndex", barcodeEditIndex);
    b.putStringArrayList("expanded", new ArrayList<>(expanded));
    b.putInt("previewMissionIndex", previewMissionIndex);
    b.putBoolean("guidedPending", guidedPending);
    b.putString("undoMessage", undoMessage);
    if (undoAlarm != null) b.putString("undoAlarm", undoAlarm.json().toString());
    if (pendingMission != null) {
      Alarm holder = new Alarm();
      holder.missions.add(pendingMission);
      b.putString("pendingMission", holder.json().toString());
    }
  }

  int d(int n) {
    return (int) (n * getResources().getDisplayMetrics().density);
  }

  GradientDrawable shape(int color, int radius) {
    GradientDrawable s = new GradientDrawable();
    s.setColor(color);
    s.setCornerRadius(d(radius));
    return s;
  }

  TextView text(String s, int size, int color) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setPadding(0, d(6), 0, d(6));
    return t;
  }

  Button button(String s, Runnable run) {
    Button b = new Button(this);
    b.setText(s);
    b.setTextColor(fg);
    b.setAllCaps(false);
    b.setTextSize(16);
    b.setBackground(shape(card, 14));
    b.setMinHeight(d(52));
    b.setPadding(d(14), d(10), d(14), d(10));
    b.setStateListAnimator(null);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, d(5), 0, d(5));
    b.setLayoutParams(p);
    b.setOnClickListener(v -> run.run());
    return b;
  }

  void heading(String s) {
    TextView t = text(s, 22, fg);
    t.setTypeface(null, Typeface.BOLD);
    body.addView(t);
  }

  void note(String s) {
    body.addView(text(s, 14, muted));
  }

  LinearLayout box() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(d(18), d(12), d(18), d(12));
    l.setBackground(shape(card, 20));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, d(12), 0, d(12));
    body.addView(l, p);
    return l;
  }

  void render() {
    int previousY =
        draft != null && draft.id == renderedDraftId && scroll != null ? scroll.getScrollY() : 0;
    renderedDraftId = draft == null ? -1 : draft.id;
    light = Store.prefs(this).getString("theme", "dark").equals("light");
    if (Store.prefs(this).getString("theme", "dark").equals("system"))
      light = (getResources().getConfiguration().uiMode & 48) == 16;
    bg = Color.parseColor(light ? "#FAF5EC" : "#101A2A");
    card = Color.parseColor(light ? "#FFFFFF" : "#1B2A3D");
    fg = Color.parseColor(light ? "#172033" : "#FFF6E7");
    muted = Color.parseColor(light ? "#596579" : "#ADB9C9");
    accent = Color.parseColor(light ? "#925507" : "#F6B95D");
    tint = Color.parseColor(light ? "#F3E7D2" : "#33404D");
    getWindow().setStatusBarColor(bg);
    getWindow().setNavigationBarColor(bg);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            light
                ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                : 0);
    page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setBackgroundColor(bg);
    page.setPadding(d(20), d(8), d(20), d(8));
    page.setOnApplyWindowInsetsListener(
        (v, in) -> {
          v.setPadding(
              d(20),
              in.getSystemWindowInsetTop() + d(8),
              d(20),
              in.getSystemWindowInsetBottom() + d(8));
          return in;
        });
    setContentView(page);
    scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(0, 0, 0, d(12));
    scroll.addView(body);
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    if (draft != null) {
      editor();
      scroll.post(() -> scroll.scrollTo(0, previousY));
      return;
    }
    LinearLayout title = new LinearLayout(this);
    title.setGravity(Gravity.CENTER_VERTICAL);
    ImageView mark = new ImageView(this);
    mark.setImageResource(com.desperta.R.drawable.ic_alarm);
    mark.setColorFilter(accent);
    mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    title.addView(mark, new LinearLayout.LayoutParams(d(30), d(30)));
    TextView brand = text("Desperta", 27, fg);
    brand.setTypeface(null, Typeface.BOLD);
    brand.setPadding(d(10), d(8), 0, d(8));
    title.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
    Button settings = button("⚙", () -> startActivity(new Intent(this, SettingsActivity.class)));
    settings.setContentDescription("Ajustes");
    settings.setTextSize(24);
    settings.setBackgroundColor(Color.TRANSPARENT);
    title.addView(settings, new LinearLayout.LayoutParams(d(52), d(52)));
    body.addView(title);
    UpdateChecker.Result update = UpdateChecker.cachedResult(this);
    if (update.status == UpdateChecker.Result.Status.UPDATE_AVAILABLE
        && Store.getSession(this) == null) {
      Button available =
          button(
              "Nova versão " + update.release.version + " disponível  ›",
              () -> startActivity(new Intent(this, UpdateActivity.class)));
      available.setTextColor(accent);
      body.addView(available);
    }
    alarms();
    if (undoAlarm != null) {
      LinearLayout undo = new LinearLayout(this);
      undo.setGravity(Gravity.CENTER_VERTICAL);
      TextView message = text(undoMessage, 13, fg);
      undo.addView(message, new LinearLayout.LayoutParams(0, -2, 1));
      Button restore =
          button(
              "Desfazer",
              () -> {
                Store.save(this, undoAlarm);
                schedule(undoAlarm);
                undoAlarm = null;
                render();
              });
      restore.setTextColor(accent);
      undo.addView(restore, new LinearLayout.LayoutParams(-2, -2));
      page.addView(undo);
    }
    Button add = primary("+ Alarme", () -> beginEdit(new Alarm()));
    page.addView(add);
  }

  Button primary(String title, Runnable action) {
    Button b = button(title, action);
    b.setTypeface(null, Typeface.BOLD);
    b.setBackground(shape(accent, 16));
    b.setTextColor(light ? Color.WHITE : Color.parseColor("#172033"));
    return b;
  }

  void beginEdit(Alarm alarm) {
    draft = alarm;
    expanded.clear();
    undoAlarm = null;
    render();
  }

  String remaining(long ms) {
    if (ms <= 0 || ms == Long.MAX_VALUE) return "Sem próximo horário";
    long min = Math.max(1, (ms - System.currentTimeMillis() + 59999) / 60000);
    long days = min / 1440;
    return "Em " + (days > 0 ? days + " d " : "") + ((min / 60) % 24) + " h " + (min % 60) + " min";
  }

  String occurrence(long at) {
    Calendar now = Calendar.getInstance(), date = Calendar.getInstance();
    date.setTimeInMillis(at);
    String day;
    if (now.get(Calendar.YEAR) == date.get(Calendar.YEAR)
        && now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)) day = "Hoje";
    else {
      now.add(Calendar.DAY_OF_MONTH, 1);
      day =
          now.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                  && now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
              ? "Amanhã"
              : new java.text.SimpleDateFormat("EEE, d 'de' MMM", new Locale("pt", "BR"))
                  .format(date.getTime());
    }
    return day
        + ", "
        + new java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(date.getTime());
  }

  String days(int mask) {
    if (mask == 127) return "Todos os dias";
    if (mask == 62) return "Seg–Sex";
    if (mask == 65) return "Sáb e Dom";
    if (mask == 0) return "Uma vez";
    List<String> selected = new ArrayList<>();
    String[] n = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};
    for (int i = 0; i < 7; i++) if ((mask & (1 << i)) != 0) selected.add(n[i]);
    return String.join(", ", selected);
  }

  String missionName(Alarm.Mission m) {
    int index = Arrays.asList(types).indexOf(m.type);
    return index >= 0 ? names[index] : "Missão";
  }

  String missionSummary(Alarm alarm) {
    if (alarm.missions.isEmpty()) return "Sem missão";
    if (alarm.missions.size() == 1) return missionName(alarm.missions.get(0));
    return alarm.missions.size() + " missões em sequência";
  }

  void alarms() {
    List<Alarm> all = Store.all(this);
    long now = System.currentTimeMillis();
    all.sort(Comparator.comparingLong(a -> a.enabled ? Scheduler.next(a, now) : Long.MAX_VALUE));
    Alarm next = all.stream().filter(a -> a.enabled).findFirst().orElse(null);
    LinearLayout hero = box();
    hero.setPadding(d(20), d(18), d(20), d(18));
    hero.addView(text(next == null ? "SUA PRÓXIMA MANHÃ" : "PRÓXIMO ALARME", 11, accent));
    TextView nextLabel =
        text(next == null ? "Amanhã começa aqui." : occurrence(Scheduler.next(next, now)), 28, fg);
    nextLabel.setTypeface(null, Typeface.BOLD);
    hero.addView(nextLabel);
    hero.addView(
        text(
            next == null
                ? "Escolha um horário e um jeito de despertar."
                : remaining(Scheduler.next(next, now)) + " · " + next.label,
            14,
            muted));
    if (next != null) {
      hero.setContentDescription(
          "Editar próximo alarme, " + occurrence(Scheduler.next(next, now)) + ", " + next.label);
      hero.setFocusable(true);
      hero.setOnClickListener(v -> beginEdit(Alarm.from(next.json())));
    }
    if (needsAttention()) {
      Button permission =
          button(
              "Revisar permissões do alarme  ›",
              () -> startActivity(new Intent(this, SettingsActivity.class)));
      permission.setTextColor(accent);
      body.addView(permission);
    }
    if (!all.isEmpty()) heading("Seus alarmes");
    for (Alarm a : all) {
      LinearLayout c = box();
      c.setPadding(d(16), d(8), d(10), d(12));
      LinearLayout row = new LinearLayout(this);
      row.setGravity(Gravity.CENTER_VERTICAL);
      TextView time =
          text(
              String.format(Locale.getDefault(), "%02d:%02d", a.hour, a.minute),
              34,
              a.enabled ? fg : muted);
      time.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
      row.addView(time, new LinearLayout.LayoutParams(0, -2, 1));
      Switch sw = new Switch(this);
      sw.setMinWidth(d(52));
      sw.setMinHeight(d(48));
      sw.setContentDescription("Ativar " + a.label);
      sw.setChecked(a.enabled);
      tintSwitch(sw);
      row.addView(sw);
      sw.setOnCheckedChangeListener(
          (v, on) -> {
            a.enabled = on;
            Store.save(this, a);
            schedule(a);
            render();
          });
      Button menu = button("⋮", () -> alarmMenu(a));
      menu.setTextSize(24);
      menu.setBackgroundColor(Color.TRANSPARENT);
      menu.setContentDescription("Mais opções de " + a.label);
      row.addView(menu, new LinearLayout.LayoutParams(d(48), d(48)));
      c.addView(row);
      c.addView(text(a.label, 16, fg));
      c.addView(text(days(a.days) + " · " + missionSummary(a), 13, muted));
      c.setFocusable(true);
      c.setContentDescription(
          "Editar alarme "
              + a.label
              + ", "
              + String.format(Locale.getDefault(), "%02d:%02d", a.hour, a.minute));
      c.setOnClickListener(v -> beginEdit(Alarm.from(a.json())));
      if (!a.enabled) c.addView(text("Desativado", 12, muted));
      if (a.skipUntil > now) c.addView(text("Pulando " + occurrence(a.skipUntil), 12, accent));
      if (Scheduler.hasNextOverride(a, now))
        c.addView(text("Só na próxima vez: " + occurrence(a.nextOverrideAt), 12, accent));
    }
    Button templates = button("Usar um modelo", this::templates);
    templates.setTextColor(accent);
    templates.setBackgroundColor(Color.TRANSPARENT);
    body.addView(templates);
  }

  boolean needsAttention() {
    if (Build.VERSION.SDK_INT >= 31
        && !getSystemService(AlarmManager.class).canScheduleExactAlarms()) return true;
    if (!getSystemService(NotificationManager.class).areNotificationsEnabled()) return true;
    return Build.VERSION.SDK_INT >= 34
        && !getSystemService(NotificationManager.class).canUseFullScreenIntent();
  }

  void alarmMenu(Alarm a) {
    long at = Scheduler.next(a, System.currentTimeMillis());
    String skip = "Pular " + occurrence(at);
    List<String> actions =
        new ArrayList<>(
            Arrays.asList(
                "Testar alarme",
                skip,
                "Só na próxima vez",
                "Duplicar alarme",
                "Salvar como modelo",
                "Excluir"));
    if (!a.enabled) {
      actions.remove(skip);
      actions.remove("Só na próxima vez");
    }
    dialog()
        .setTitle(a.label)
        .setItems(
            actions.toArray(new String[0]),
            (dialog, which) -> {
              String action = actions.get(which);
              if (action.equals("Testar alarme")) guidedTest(a);
              else if (action.equals(skip)) {
                undoAlarm = Alarm.from(a.json());
                undoMessage = "Ocorrência pulada";
                a.skipUntil = at;
                Store.save(this, a);
                schedule(a);
                render();
              } else if (action.equals("Só na próxima vez")) changeNextOccurrence(a);
              else if (action.equals("Duplicar alarme")) {
                Alarm copy = a.copy();
                copy.label = a.label + " (cópia)";
                Store.save(this, copy);
                schedule(copy);
                render();
              } else if (action.equals("Salvar como modelo")) saveTemplate(a);
              else if (action.equals("Excluir")) {
                undoAlarm = Alarm.from(a.json());
                undoMessage = "Alarme excluído";
                Scheduler.cancel(this, a.id);
                Store.delete(this, a.id);
                render();
              }
            })
        .show();
  }

  void changeNextOccurrence(Alarm a) {
    long next = Scheduler.next(a, System.currentTimeMillis());
    Calendar date = Calendar.getInstance();
    date.setTimeInMillis(next);
    LinearLayout content = new LinearLayout(this);
    content.setGravity(Gravity.CENTER);
    content.setPadding(d(16), d(8), d(16), d(8));
    NumberPicker hours =
        timeWheel(23, date.get(Calendar.HOUR_OF_DAY), "Horas da próxima ocorrência");
    NumberPicker minutes =
        timeWheel(59, date.get(Calendar.MINUTE), "Minutos da próxima ocorrência");
    content.addView(hours, new LinearLayout.LayoutParams(d(100), d(160)));
    content.addView(text(":", 26, fg));
    content.addView(minutes, new LinearLayout.LayoutParams(d(100), d(160)));
    AlertDialog dialog =
        dialog()
            .setTitle("Só na próxima vez")
            .setMessage(
                "Alterar " + occurrence(next) + ". Os outros dias mantêm o horário habitual.")
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", null)
            .create();
    dialog.setOnShowListener(
        v ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    b -> {
                      hours.clearFocus();
                      minutes.clearFocus();
                      date.set(Calendar.HOUR_OF_DAY, hours.getValue());
                      date.set(Calendar.MINUTE, minutes.getValue());
                      if (date.getTimeInMillis() <= System.currentTimeMillis()) {
                        Toast.makeText(
                                this, "Escolha um horário futuro para esse dia", Toast.LENGTH_LONG)
                            .show();
                        return;
                      }
                      undoAlarm = Alarm.from(a.json());
                      undoMessage = "Próximo horário alterado";
                      if (!Scheduler.setNextOverride(
                          a, date.getTimeInMillis(), System.currentTimeMillis())) {
                        Toast.makeText(this, "Escolha um horário futuro", Toast.LENGTH_LONG).show();
                        return;
                      }
                      Store.save(this, a);
                      schedule(a);
                      dialog.dismiss();
                      render();
                    }));
    dialog.show();
  }

  void guidedTest(Alarm a) {
    String message =
        "Confira o som e depois conclua "
            + (a.missions.size() == 1 ? "a missão" : "as missões")
            + ". O teste tem um botão próprio para sair e mantém seus horários.";
    if (a.missions.isEmpty())
      message =
          "Confira o som, a vibração e a aparência. Use Parar prévia para terminar. Seus horários"
              + " serão mantidos.";
    dialog()
        .setTitle("Testar alarme")
        .setMessage(message)
        .setNegativeButton("Agora não", null)
        .setPositiveButton(
            "Começar teste",
            (v, i) -> {
              guidedPending = true;
              AlarmService.start(this, a.id, true);
            })
        .show();
  }

  void templates() {
    List<Alarm> saved = readTemplates();
    List<String> labels =
        new ArrayList<>(
            Arrays.asList(
                "Trabalho · Seg–Sex, 07:00",
                "Academia · Seg, Qua, Sex, 06:30",
                "Compromisso · Uma vez, 08:00"));
    for (Alarm a : saved) labels.add(a.label);
    choice(
        "Modelos de alarme",
        labels.toArray(new String[0]),
        i -> {
          Alarm a = i >= 3 ? saved.get(i - 3).copy() : new Alarm();
          if (i == 0) {
            a.label = "Trabalho";
            a.days = 62;
            a.hour = 7;
            a.minute = 0;
          }
          if (i == 1) {
            a.label = "Academia";
            a.days = 42;
            a.hour = 6;
            a.minute = 30;
          }
          if (i == 2) {
            a.label = "Compromisso";
            a.days = 0;
            a.hour = 8;
            a.minute = 0;
          }
          beginEdit(a);
        });
  }

  List<Alarm> readTemplates() {
    List<Alarm> out = new ArrayList<>();
    try {
      JSONArray items = new JSONArray(Store.prefs(this).getString("alarm_templates", "[]"));
      for (int i = 0; i < items.length(); i++) out.add(Alarm.from(items.getJSONObject(i)));
    } catch (Exception ignored) {
    }
    return out;
  }

  void saveTemplate(Alarm a) {
    input(
        "Nome do modelo",
        a.label,
        false,
        name -> {
          if (name.isEmpty()) return;
          List<Alarm> list = readTemplates();
          list.removeIf(t -> t.label.equals(name));
          Alarm template = a.copy();
          template.label = name;
          template.enabled = true;
          list.add(template);
          JSONArray items = new JSONArray();
          for (Alarm t : list) items.put(t.json());
          Store.prefs(this).edit().putString("alarm_templates", items.toString()).commit();
          Toast.makeText(this, "Modelo salvo", Toast.LENGTH_SHORT).show();
        });
  }

  void schedule(Alarm a) {
    try {
      if (a.enabled) Scheduler.schedule(this, a);
      else Scheduler.cancel(this, a.id);
    } catch (SecurityException e) {
      Toast.makeText(
              this,
              "Salvo. Autorize alarmes exatos em Ajustes para ativar o agendamento.",
              Toast.LENGTH_LONG)
          .show();
    }
  }

  void editor() {
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    Button back = button("‹", this::onBackPressed);
    back.setContentDescription("Voltar");
    back.setTextSize(28);
    back.setBackgroundColor(Color.TRANSPARENT);
    header.addView(back, new LinearLayout.LayoutParams(d(48), d(48)));
    TextView title = text("Seu alarme", 21, fg);
    title.setTypeface(null, Typeface.BOLD);
    header.addView(title);
    body.addView(header);
    Button label =
        button(
            draft.label + "  ›",
            () ->
                input(
                    "Nome do alarme",
                    draft.label,
                    false,
                    name -> {
                      if (!name.isEmpty()) {
                        draft.label = name;
                        render();
                      }
                    }));
    label.setContentDescription("Nome do alarme: " + draft.label);
    label.setBackgroundColor(Color.TRANSPARENT);
    label.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    body.addView(label);
    LinearLayout wheels = new LinearLayout(this);
    wheels.setGravity(Gravity.CENTER);
    wheels.setBackground(shape(card, 20));
    NumberPicker hours = timeWheel(23, draft.hour, "Horas"),
        minutes = timeWheel(59, draft.minute, "Minutos");
    wheels.addView(hours, new LinearLayout.LayoutParams(d(110), d(144)));
    wheels.addView(text(":", 30, fg));
    wheels.addView(minutes, new LinearLayout.LayoutParams(d(110), d(144)));
    body.addView(wheels);
    TextView countdown =
        text(
            occurrence(Scheduler.next(draft, System.currentTimeMillis()))
                + " · "
                + remaining(Scheduler.next(draft, System.currentTimeMillis())),
            13,
            muted);
    body.addView(countdown);
    NumberPicker.OnValueChangeListener timeChange =
        (v, old, value) -> {
          draft.hour = hours.getValue();
          draft.minute = minutes.getValue();
          Scheduler.clearNextOverride(draft);
          v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
          countdown.setText(
              occurrence(Scheduler.next(draft, System.currentTimeMillis()))
                  + " · "
                  + remaining(Scheduler.next(draft, System.currentTimeMillis())));
        };
    hours.setOnValueChangedListener(timeChange);
    minutes.setOnValueChangedListener(timeChange);
    LinearLayout daysBox = box();
    Button preset =
        button(
            "Repetir · " + days(draft.days) + "  ›",
            () ->
                choice(
                    "Repetir",
                    new String[] {"Dias úteis", "Fim de semana", "Todos os dias", "Uma vez"},
                    i -> {
                      draft.days = new int[] {62, 65, 127, 0}[i];
                      Scheduler.clearNextOverride(draft);
                      render();
                    }));
    preset.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    daysBox.addView(preset);
    HorizontalScrollView week = new HorizontalScrollView(this);
    week.setHorizontalScrollBarEnabled(false);
    week.setFillViewport(true);
    LinearLayout daysRow = new LinearLayout(this);
    String[] shortDays = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};
    String[] fullDays = {
      "Domingo",
      "Segunda-feira",
      "Terça-feira",
      "Quarta-feira",
      "Quinta-feira",
      "Sexta-feira",
      "Sábado"
    };
    for (int i = 0; i < 7; i++) {
      final int bit = 1 << i;
      boolean checked = (draft.days & bit) != 0;
      CheckBox day = new CheckBox(this);
      day.setButtonDrawable(null);
      day.setText(shortDays[i]);
      day.setTextSize(12);
      day.setTypeface(null, Typeface.BOLD);
      day.setTextColor(checked ? accent : muted);
      day.setGravity(Gravity.CENTER);
      day.setMinWidth(d(48));
      day.setMinHeight(d(48));
      day.setPadding(0, 0, 0, 0);
      day.setChecked(checked);
      day.setContentDescription(fullDays[i]);
      day.setBackground(shape(checked ? tint : card, 12));
      daysRow.addView(day, new LinearLayout.LayoutParams(d(48), d(48), 1));
      day.setOnCheckedChangeListener(
          (v, on) -> {
            draft.days ^= bit;
            Scheduler.clearNextOverride(draft);
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            render();
          });
    }
    week.addView(daysRow);
    daysBox.addView(week);
    section(
        "missions",
        "Como desligar",
        missionSummary(draft),
        () -> {
          if (draft.missions.isEmpty()) note("Escolha uma missão ou desligue com um toque.");
          for (int i = 0; i < draft.missions.size(); i++) {
            final int index = i;
            Alarm.Mission m = draft.missions.get(i);
            Button mission =
                button((i + 1) + ". " + missionName(m) + "  ›", () -> missionMenu(index));
            body.addView(mission);
            if (m.type.equals("barcode")) note(String.join(" · ", m.acceptedCodes()));
          }
          if (draft.missions.size() < 5)
            body.addView(button("+ Adicionar missão", this::chooseMission));
          else note("Você já escolheu cinco missões.");
        });
    section(
        "sound",
        "Som",
        (draft.sound.isEmpty() ? "Padrão do aparelho" : "Áudio personalizado")
            + " · "
            + draft.volume
            + "%",
        () -> {
          body.addView(
              button(
                  draft.sound.isEmpty()
                      ? "Escolher som (padrão do aparelho)"
                      : "Alterar som personalizado",
                  () -> {
                    Intent in =
                        new Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .setType("audio/*")
                            .addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(in, 40);
                  }));
          body.addView(button("Ouvir som", this::previewSound));
          body.addView(
              button(
                  "Restaurar som padrão",
                  () -> {
                    draft.sound = "";
                    render();
                  }));
          TextView vol = text("Volume: " + draft.volume + "%", 16, fg);
          body.addView(vol);
          SeekBar slider = new SeekBar(this);
          slider.setMax(100);
          slider.setProgress(draft.volume);
          body.addView(slider);
          slider.setOnSeekBarChangeListener(
              new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int v, boolean u) {
                  draft.volume = v;
                  vol.setText("Volume: " + v + "%");
                }

                public void onStartTrackingTouch(SeekBar s) {}

                public void onStopTrackingTouch(SeekBar s) {}
              });
          toggle("Vibração", draft.vibrate, v -> draft.vibrate = v);
          body.addView(
              button(
                  "Despertar gradual: " + draft.gentleSeconds + " s",
                  () ->
                      choice(
                          "Aumento gradual do volume",
                          new String[] {"Desligado", "30 segundos", "60 segundos", "120 segundos"},
                          i -> {
                            draft.gentleSeconds = new int[] {0, 30, 60, 120}[i];
                            render();
                          })));
          toggle("Efeito de volume máximo", draft.extraLoud, v -> draft.extraLoud = v);
        });
    section(
        "snooze",
        "Soneca",
        draft.snoozeLimit == 0
            ? "Desativada"
            : draft.snoozeMinutes
                + " min · "
                + draft.snoozeLimit
                + (draft.snoozeLimit == 1 ? " vez" : " vezes"),
        () -> {
          body.addView(
              button(
                  "Soneca: "
                      + draft.snoozeMinutes
                      + " min · "
                      + draft.snoozeLimit
                      + (draft.snoozeLimit == 1 ? " vez" : " vezes"),
                  () ->
                      input(
                          "Minutos de soneca (1–60)",
                          "" + draft.snoozeMinutes,
                          true,
                          s -> {
                            draft.snoozeMinutes = bounded(s, 1, 60);
                            input(
                                "Limite de sonecas (0–10)",
                                "" + draft.snoozeLimit,
                                true,
                                t -> {
                                  draft.snoozeLimit = bounded(t, 0, 10);
                                  render();
                                });
                          })));
          body.addView(
              button(
                  "Confirmar despertar: "
                      + (draft.wakeCheckMinutes == 0
                          ? "desligado"
                          : draft.wakeCheckMinutes + " min"),
                  () ->
                      choice(
                          "Confirmar que está acordado",
                          new String[] {"Desligado", "1 minuto", "3 minutos", "5 minutos"},
                          i -> {
                            draft.wakeCheckMinutes = new int[] {0, 1, 3, 5}[i];
                            render();
                          })));
        });
    section(
        "voice",
        "Voz",
        voiceSummary(),
        () -> {
          toggle("Falar a hora", draft.timeReminder, v -> draft.timeReminder = v);
          toggle(
              "Falar a previsão do tempo", draft.weatherReminder, v -> draft.weatherReminder = v);
          toggle("Falar o nome do alarme", draft.labelReminder, v -> draft.labelReminder = v);
          body.addView(
              button(
                  "Ouvir exemplo de voz",
                  () ->
                      choice(
                          "Ouvir exemplo",
                          new String[] {"Hora", "Clima", "Nome do alarme"},
                          i ->
                              speak(
                                  i == 0
                                      ? "Agora são "
                                          + java.text.DateFormat.getTimeInstance(3)
                                              .format(new Date())
                                      : i == 1
                                          ? getSharedPreferences("desperta", 0)
                                              .getString(
                                                  "weather_text",
                                                  "Configure a cidade e atualize a previsão em"
                                                      + " Ajustes.")
                                          : draft.label))));
          note(
              "A previsão usa a cidade configurada em Ajustes. Voz depende do mecanismo de fala"
                  + " instalado.");
        });
    section(
        "appearance",
        "Aparência",
        draft.wallpaper.startsWith("content:") ? "Imagem escolhida" : draft.wallpaper,
        () -> {
          body.addView(
              button(
                  "Papel de parede: "
                      + (draft.wallpaper.startsWith("content:")
                          ? "Imagem escolhida"
                          : draft.wallpaper),
                  () ->
                      choice(
                          "Papel de parede",
                          new String[] {"Aurora", "Oceano", "Noite", "Escolher imagem"},
                          i -> {
                            if (i == 3)
                              startActivityForResult(
                                  new Intent(Intent.ACTION_OPEN_DOCUMENT)
                                      .setType("image/*")
                                      .addCategory(Intent.CATEGORY_OPENABLE),
                                  41);
                            else {
                              draft.wallpaper = new String[] {"Aurora", "Oceano", "Noite"}[i];
                              render();
                            }
                          })));
        });
    Button save =
        primary(
            "Salvar alarme",
            () -> {
              hours.clearFocus();
              minutes.clearFocus();
              draft.hour = hours.getValue();
              draft.minute = minutes.getValue();
              long at = Scheduler.next(draft, System.currentTimeMillis());
              Store.save(this, draft);
              boolean scheduled = scheduleChecked(draft);
              draft = null;
              expanded.clear();
              stopSample();
              render();
              if (scheduled)
                Toast.makeText(this, "Alarme definido para " + occurrence(at), Toast.LENGTH_LONG)
                    .show();
            });
    page.addView(save);
  }

  String voiceSummary() {
    List<String> enabled = new ArrayList<>();
    if (draft.timeReminder) enabled.add("Hora");
    if (draft.weatherReminder) enabled.add("Clima");
    if (draft.labelReminder) enabled.add("Nome");
    return enabled.isEmpty() ? "Desativada" : String.join(" · ", enabled);
  }

  void section(String key, String title, String summary, Runnable content) {
    boolean open = expanded.contains(key);
    LinearLayout outer = box();
    Button row =
        button(
            title + "  " + (open ? "⌃" : "⌄") + "\n" + summary,
            () -> {
              int y = scroll.getScrollY();
              if (!expanded.remove(key)) expanded.add(key);
              render();
              scroll.post(() -> scroll.scrollTo(0, y));
            });
    row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    row.setContentDescription(title + ", " + summary + ", " + (open ? "recolher" : "expandir"));
    outer.addView(row);
    if (open) {
      LinearLayout original = body;
      body = outer;
      content.run();
      body = original;
    }
  }

  boolean scheduleChecked(Alarm a) {
    try {
      if (a.enabled) Scheduler.schedule(this, a);
      else Scheduler.cancel(this, a.id);
      return true;
    } catch (SecurityException e) {
      Toast.makeText(
              this,
              "Alarme salvo. Autorize alarmes exatos em Ajustes para agendar.",
              Toast.LENGTH_LONG)
          .show();
      return false;
    }
  }

  void tintSwitch(Switch sw) {
    android.content.res.ColorStateList colors =
        new android.content.res.ColorStateList(
            new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
            new int[] {accent, muted});
    sw.setThumbTintList(colors);
    sw.setTrackTintList(colors);
  }

  void missionMenu(int index) {
    Alarm.Mission m = draft.missions.get(index);
    List<String> options = new ArrayList<>(Arrays.asList("Experimentar", "Configurar"));
    if (index > 0) options.add("Mover para cima");
    if (index < draft.missions.size() - 1) options.add("Mover para baixo");
    options.add("Remover missão");
    choice(
        missionName(m),
        options.toArray(new String[0]),
        i -> {
          String action = options.get(i);
          if (action.equals("Experimentar")) {
            previewMissionIndex = index;
            launchMissionPreview(m);
          } else if (action.equals("Configurar")) {
            if (m.type.equals("barcode")) openBarcodeLibrary(index);
            else editMission(index);
          } else if (action.equals("Mover para cima")) {
            Collections.swap(draft.missions, index, index - 1);
            render();
          } else if (action.equals("Mover para baixo")) {
            Collections.swap(draft.missions, index, index + 1);
            render();
          } else {
            draft.missions.remove(index);
            render();
          }
        });
  }

  void editMission(int index) {
    Alarm.Mission m = draft.missions.get(index);
    if (m.type.equals("photo")) {
      dialog()
          .setMessage("Para trocar a foto, remova esta missão e cadastre uma nova referência.")
          .setPositiveButton("Entendi", null)
          .show();
      return;
    }
    if (m.type.equals("object")) {
      choice(
          "Objeto a encontrar",
          new String[] {"Copo / caneca", "Livro", "Garrafa", "Cadeira", "Planta"},
          i -> {
            m.target = new String[] {"Cup", "Book", "Bottle", "Chair", "Plant"}[i];
            render();
          });
      return;
    }
    boolean text = m.type.equals("typing") || m.type.equals("rhythm");
    input(
        text ? "Frase ou palavra" : "Quantidade (1–50)",
        text ? m.target : "" + m.count,
        !text,
        value -> {
          if (text) {
            if (!value.isEmpty()) m.target = value;
          } else m.count = bounded(value, 1, 50);
          render();
        });
  }

  void launchMissionPreview(Alarm.Mission m) {
    startActivityForResult(
        new Intent(this, MissionActivity.class)
            .putExtra("type", m.type)
            .putExtra("target", m.target)
            .putExtra("count", m.count)
            .putExtra("mode", "solve")
            .putStringArrayListExtra("targets", m.acceptedCodes()),
        44);
  }

  interface Value {
    void set(String s);
  }

  interface Index {
    void set(int i);
  }

  interface Bool {
    void set(boolean b);
  }

  AlertDialog.Builder dialog() {
    return new AlertDialog.Builder(
        this, light ? android.R.style.Theme_Material_Light_Dialog_Alert : R.style.DespertaDialog);
  }

  void input(String title, String initial, boolean number, Value action) {
    EditText e = new EditText(this);
    e.setTextColor(fg);
    e.setHintTextColor(muted);
    e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
    e.setText(initial);
    if (number) e.setInputType(2);
    dialog()
        .setTitle(title)
        .setView(e)
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Confirmar", (v, i) -> action.set(e.getText().toString().trim()))
        .show();
  }

  int bounded(String s, int min, int max) {
    try {
      return Math.max(min, Math.min(max, Integer.parseInt(s)));
    } catch (Exception e) {
      return min;
    }
  }

  void choice(String title, String[] labels, Index action) {
    dialog().setTitle(title).setItems(labels, (v, i) -> action.set(i)).show();
  }

  void toggle(String title, boolean checked, Bool action) {
    Switch s = new Switch(this);
    s.setText(title);
    s.setTextSize(16);
    s.setTextColor(fg);
    s.setPadding(0, d(16), 0, d(16));
    s.setChecked(checked);
    s.setMinHeight(d(48));
    tintSwitch(s);
    s.setOnCheckedChangeListener((v, b) -> action.set(b));
    body.addView(s);
  }

  NumberPicker timeWheel(int max, int value, String description) {
    NumberPicker picker = new NumberPicker(this);
    picker.setMinValue(0);
    picker.setMaxValue(max);
    picker.setFormatter(n -> String.format(Locale.getDefault(), "%02d", n));
    picker.setValue(value);
    picker.setWrapSelectorWheel(true);
    picker.setDescendantFocusability(NumberPicker.FOCUS_AFTER_DESCENDANTS);
    picker.setOnLongPressUpdateInterval(65);
    picker.setContentDescription(description);
    if (Build.VERSION.SDK_INT >= 29) {
      picker.setTextColor(fg);
      picker.setTextSize(30 * getResources().getDisplayMetrics().scaledDensity);
      picker.setSelectionDividerHeight(d(1));
    }
    picker.setOnTouchListener(
        (v, event) -> {
          v.getParent()
              .requestDisallowInterceptTouchEvent(
                  event.getActionMasked() != MotionEvent.ACTION_UP
                      && event.getActionMasked() != MotionEvent.ACTION_CANCEL);
          return false;
        });
    return picker;
  }

  void openBarcodeLibrary(int index) {
    barcodeEditIndex = index;
    Intent intent = new Intent(this, BarcodeLibraryActivity.class);
    if (index >= 0)
      intent.putStringArrayListExtra("targets", draft.missions.get(index).acceptedCodes());
    intent.putExtra("editing", index >= 0);
    startActivityForResult(intent, 43);
  }

  void chooseMission() {
    ScrollView sc = new ScrollView(this);
    LinearLayout list = new LinearLayout(this);
    list.setOrientation(LinearLayout.VERTICAL);
    list.setPadding(d(20), d(8), d(20), d(8));
    sc.addView(list);
    AlertDialog dialog =
        dialog()
            .setTitle("Como você quer despertar?")
            .setView(sc)
            .setNegativeButton("Voltar", null)
            .create();
    String[] groups = {"Levantar da cama", "Ativar a cabeça", "Movimentar o corpo"};
    int[][] indices = {{0, 6, 8}, {1, 2, 3, 9}, {4, 5, 7}};
    for (int g = 0; g < groups.length; g++) {
      TextView h = text(groups[g], 14, accent);
      h.setTypeface(null, Typeface.BOLD);
      list.addView(h);
      for (int index : indices[g]) {
        Button option =
            button(
                names[index],
                () -> {
                  dialog.dismiss();
                  missionDescription(types[index]);
                });
        option.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        list.addView(option);
      }
    }
    dialog.show();
  }

  String description(String type) {
    switch (type) {
      case "barcode":
        return "Escaneie um dos códigos selecionados, por exemplo um produto em outro cômodo."
            + " Precisa de câmera.";
      case "math":
        return "Resolva contas para concluir. Você escolhe a quantidade de perguntas.";
      case "typing":
        return "Digite uma frase exatamente como foi cadastrada.";
      case "colors":
        return "Encontre a cor indicada entre as opções. Você escolhe a quantidade de rodadas.";
      case "steps":
        return "Caminhe até atingir a quantidade escolhida. Precisa de sensor de passos e permissão"
            + " de atividade física.";
      case "shake":
        return "Sacuda o aparelho até completar a quantidade escolhida. Usa o sensor de movimento.";
      case "photo":
        return "Cadastre uma foto de referência e fotografe a mesma cena ao acordar. Precisa de"
            + " câmera.";
      case "squat":
        return "Faça os agachamentos com o corpo visível na câmera. A posição e a iluminação"
            + " influenciam a leitura.";
      case "object":
        return "Encontre e fotografe o objeto escolhido. O reconhecimento usa a câmera.";
      default:
        return "Diga a palavra escolhida acompanhando o ritmo. Precisa de microfone e"
            + " reconhecimento de voz disponível.";
    }
  }

  void missionDescription(String type) {
    dialog()
        .setTitle(names[Arrays.asList(types).indexOf(type)])
        .setMessage(description(type))
        .setNegativeButton("Voltar", (v, i) -> chooseMission())
        .setPositiveButton("Configurar", (v, i) -> configureMission(type))
        .show();
  }

  void configureMission(String type) {
    if (type.equals("steps")
        && getSystemService(android.hardware.SensorManager.class)
                .getDefaultSensor(android.hardware.Sensor.TYPE_STEP_DETECTOR)
            == null) {
      dialog()
          .setMessage("Este aparelho não tem sensor de passos. Escolha outra missão.")
          .setPositiveButton("Entendi", null)
          .show();
      return;
    }
    if (type.equals("rhythm") && !android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
      dialog()
          .setMessage(
              "Instale ou ative um serviço de reconhecimento de voz no Android para usar esta"
                  + " missão.")
          .setPositiveButton("Entendi", null)
          .show();
      return;
    }
    if (type.equals("barcode")) {
      openBarcodeLibrary(-1);
    } else if (type.equals("photo")) {
      registerType = type;
      startActivityForResult(
          new Intent(this, MissionActivity.class)
              .putExtra("type", type)
              .putExtra("mode", "register"),
          42);
    } else if (type.equals("typing") || type.equals("rhythm")) {
      input(
          type.equals("typing") ? "Frase para digitar" : "Palavra para falar",
          type.equals("typing") ? "Hoje eu começo bem" : "acordar",
          false,
          s -> {
            if (!s.isEmpty()) {
              offerMission(new Alarm.Mission(type, s, 3));
            }
          });
    } else if (type.equals("object")) {
      choice(
          "Objeto a encontrar (reconhecimento local)",
          new String[] {"Copo / caneca", "Livro", "Garrafa", "Cadeira", "Planta"},
          k -> {
            offerMission(
                new Alarm.Mission(
                    type, new String[] {"Cup", "Book", "Bottle", "Chair", "Plant"}[k], 1));
          });
    } else {
      input(
          "Quantidade (1–50)",
          "3",
          true,
          s -> {
            offerMission(new Alarm.Mission(type, "", bounded(s, 1, 50)));
          });
    }
  }

  void offerMission(Alarm.Mission m) {
    pendingMission = m;
    dialog()
        .setTitle(missionName(m))
        .setMessage("Experimente com esta configuração antes de adicionar ao alarme.")
        .setNegativeButton("Cancelar", (v, i) -> pendingMission = null)
        .setNeutralButton(
            "Experimentar",
            (v, i) -> {
              previewMissionIndex = -1;
              launchMissionPreview(m);
            })
        .setPositiveButton(
            "Adicionar missão",
            (v, i) -> {
              if (draft != null && draft.missions.size() < 5) draft.missions.add(m);
              pendingMission = null;
              expanded.add("missions");
              render();
            })
        .show();
  }

  void previewSound() {
    stopSample();
    try {
      sample = new android.media.MediaPlayer();
      sample.setAudioAttributes(
          new android.media.AudioAttributes.Builder()
              .setUsage(android.media.AudioAttributes.USAGE_ALARM)
              .build());
      Uri u =
          draft.sound.isEmpty()
              ? android.media.RingtoneManager.getDefaultUri(
                  android.media.RingtoneManager.TYPE_ALARM)
              : Uri.parse(draft.sound);
      sample.setDataSource(this, u);
      sample.prepare();
      sample.setVolume(draft.volume / 100f, draft.volume / 100f);
      sample.start();
      new Handler().postDelayed(this::stopSample, 10000);
    } catch (Exception e) {
      stopSample();
      Toast.makeText(this, "Não foi possível abrir esse áudio", Toast.LENGTH_LONG).show();
    }
  }

  void speak(String value) {
    if (speech != null) speech.shutdown();
    speech =
        new android.speech.tts.TextToSpeech(
            this,
            status -> {
              if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                speech.setLanguage(new Locale("pt", "BR"));
                speech.speak(value, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "sample");
              } else
                Toast.makeText(this, "Mecanismo de voz indisponível", Toast.LENGTH_LONG).show();
            });
  }

  @Override
  protected void onDestroy() {
    if (speech != null) speech.shutdown();
    stopSample();
    super.onDestroy();
  }

  void stopSample() {
    if (sample != null) {
      sample.release();
      sample = null;
    }
  }

  @Override
  protected void onActivityResult(int req, int res, Intent data) {
    super.onActivityResult(req, res, data);
    if (req == 44 && draft != null) {
      boolean complete = res == RESULT_OK;
      Toast.makeText(
              this,
              complete ? "Teste concluído" : "Teste encerrado sem concluir",
              Toast.LENGTH_SHORT)
          .show();
      if (pendingMission != null) offerMission(pendingMission);
      previewMissionIndex = -1;
      return;
    }
    if (res != RESULT_OK || data == null || draft == null) return;
    if (req == 40 || req == 41) {
      Uri u = data.getData();
      if (u == null) return;
      try {
        getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      } catch (Exception ignored) {
      }
      if (req == 40) draft.sound = u.toString();
      else draft.wallpaper = u.toString();
    }
    if (req == 43) {
      ArrayList<String> codes = data.getStringArrayListExtra("targets");
      if (codes != null && !codes.isEmpty()) {
        Alarm.Mission mission = new Alarm.Mission("barcode", codes.get(0), 1);
        mission.targets.addAll(codes);
        if (barcodeEditIndex >= 0 && barcodeEditIndex < draft.missions.size())
          draft.missions.set(barcodeEditIndex, mission);
        else if (draft.missions.size() < 5) draft.missions.add(mission);
      } else if (data.getBooleanExtra("remove", false)
          && barcodeEditIndex >= 0
          && barcodeEditIndex < draft.missions.size()) {
        draft.missions.remove(barcodeEditIndex);
      }
      barcodeEditIndex = -1;
    }
    if (req == 42) {
      String target = data.getStringExtra("target");
      if (target != null && !target.isEmpty() && draft.missions.size() < 5) {
        render();
        offerMission(new Alarm.Mission(registerType, target, 1));
        return;
      }
    }
    render();
  }

  @Override
  public void onBackPressed() {
    if (draft != null)
      dialog()
          .setMessage("Descartar alterações?")
          .setNegativeButton("Continuar", null)
          .setPositiveButton(
              "Descartar",
              (v, i) -> {
                draft = null;
                render();
              })
          .show();
    else super.onBackPressed();
  }
}
