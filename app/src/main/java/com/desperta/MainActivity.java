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
  int bg, card, fg, muted;
  final int pink = Color.rgb(255, 49, 89), cyan = Color.rgb(32, 199, 223);
  LinearLayout page, body;
  Alarm draft;
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
    if (b != null) registerType = b.getString("registerType", "");
    render();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (page != null) render();
  }

  @Override
  protected void onPause() {
    stopSample();
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    if (draft != null) b.putString("draft", draft.json().toString());
    b.putString("registerType", registerType);
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
    b.setBackground(shape(card, 16));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, d(54));
    p.setMargins(0, d(5), 0, d(5));
    b.setLayoutParams(p);
    b.setOnClickListener(v -> run.run());
    return b;
  }

  void heading(String s) {
    TextView t = text(s, 26, fg);
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
    l.setBackground(shape(card, 24));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, d(12), 0, d(12));
    body.addView(l, p);
    return l;
  }

  void render() {
    boolean light = getSharedPreferences("desperta", 0).getString("theme", "dark").equals("light");
    if (getSharedPreferences("desperta", 0).getString("theme", "dark").equals("system"))
      light = (getResources().getConfiguration().uiMode & 48) == 16;
    bg = Color.parseColor(light ? "#F5F5F8" : "#09090B");
    card = Color.parseColor(light ? "#FFFFFF" : "#19191D");
    fg = Color.parseColor(light ? "#16161B" : "#FAFAFC");
    muted = Color.parseColor(light ? "#656574" : "#A2A2AF");
    page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setBackgroundColor(bg);
    page.setPadding(d(20), d(12), d(20), d(8));
    page.setOnApplyWindowInsetsListener(
        (v, in) -> {
          page.setPadding(
              d(20),
              in.getSystemWindowInsetTop() + d(12),
              d(20),
              in.getSystemWindowInsetBottom() + d(8));
          return in;
        });
    setContentView(page);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    scroll.addView(body);
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    if (draft != null) {
      editor();
      return;
    }
    LinearLayout title = new LinearLayout(this);
    TextView brand = text("desperta", 32, fg);
    brand.setTypeface(null, Typeface.BOLD);
    title.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
    TextView badge = text("GRATUITO", 12, cyan);
    title.addView(badge);
    body.addView(title);
    alarms();
    LinearLayout nav = new LinearLayout(this);
    for (String label : new String[] {"Alarmes"}) {
      TextView t = text(label, 12, label.equals(tab) ? cyan : muted);
      t.setGravity(17);
      t.setPadding(0, d(20), 0, d(16));
      nav.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
      t.setOnClickListener(
          v -> {
            tab = label;
            render();
          });
    }
    TextView settings = text("Ajustes", 12, muted);
    settings.setGravity(17);
    settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    nav.addView(settings, new LinearLayout.LayoutParams(0, -2, 1));
    page.addView(nav);
  }

  String remaining(long ms) {
    long min = Math.max(1, (ms - System.currentTimeMillis() + 59999) / 60000);
    return "Toca em " + (min / 60) + " h " + (min % 60) + " min";
  }

  String days(int mask) {
    if (mask == 127) return "Todos os dias";
    if (mask == 0) return "Uma vez";
    String s = "";
    String[] n = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};
    for (int i = 0; i < 7; i++) if ((mask & (1 << i)) != 0) s += n[i] + "  ";
    return s;
  }

  void alarms() {
    List<Alarm> all = Store.all(this);
    long nearest = Long.MAX_VALUE;
    for (Alarm a : all)
      if (a.enabled) nearest = Math.min(nearest, Scheduler.next(a, System.currentTimeMillis()));
    note(nearest == Long.MAX_VALUE ? "Sua manhã começa aqui." : remaining(nearest));
    if (Build.VERSION.SDK_INT >= 31
        && !getSystemService(AlarmManager.class).canScheduleExactAlarms())
      body.addView(
          button(
              "Permitir alarmes exatos →",
              () -> startActivity(new Intent(this, SettingsActivity.class))));
    if (all.isEmpty()) {
      LinearLayout c = box();
      c.addView(text("☀", 48, cyan));
      c.addView(text("Acorde com um propósito", 24, fg));
      c.addView(
          text("Crie seu primeiro alarme e escolha uma missão para começar o dia.", 16, muted));
    }
    for (Alarm a : all) {
      LinearLayout c = box();
      LinearLayout row = new LinearLayout(this);
      TextView label = text(days(a.days), 13, muted);
      row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
      Switch sw = new Switch(this);
      sw.setContentDescription("Ativar " + a.label);
      sw.setChecked(a.enabled);
      row.addView(sw);
      sw.setOnCheckedChangeListener(
          (v, on) -> {
            a.enabled = on;
            Store.save(this, a);
            schedule(a);
            render();
          });
      c.addView(row);
      TextView time =
          text(String.format(Locale.getDefault(), "%02d:%02d", a.hour, a.minute), 52, fg);
      time.setOnClickListener(
          v -> {
            draft = Alarm.from(a.json());
            render();
          });
      c.addView(time);
      c.addView(
          text(
              a.label + (a.missions.isEmpty() ? "" : "  ·  " + a.missions.size() + " missão(ões)"),
              16,
              fg));
      Button menu =
          button(
              "Editar  ·  Mais opções",
              () ->
                  new AlertDialog.Builder(this)
                      .setTitle(a.label)
                      .setItems(
                          new String[] {
                            "Editar",
                            "Prévia do alarme",
                            "Pular uma vez",
                            "Duplicar alarme",
                            "Excluir"
                          },
                          (dialog, i) -> {
                            if (i == 0) {
                              draft = Alarm.from(a.json());
                              render();
                            }
                            if (i == 1) AlarmService.start(this, a.id, true);
                            if (i == 2) {
                              a.skipUntil = Scheduler.next(a, System.currentTimeMillis());
                              Store.save(this, a);
                              schedule(a);
                              render();
                            }
                            if (i == 3) {
                              Alarm copy = a.copy();
                              Store.save(this, copy);
                              schedule(copy);
                              render();
                            }
                            if (i == 4)
                              new AlertDialog.Builder(this)
                                  .setMessage("Excluir este alarme?")
                                  .setNegativeButton("Cancelar", null)
                                  .setPositiveButton(
                                      "Excluir",
                                      (x, y) -> {
                                        Scheduler.cancel(this, a.id);
                                        Store.delete(this, a.id);
                                        render();
                                      })
                                  .show();
                          })
                      .show());
      c.addView(menu);
      if (a.skipUntil > System.currentTimeMillis())
        c.addView(text("Próxima ocorrência pulada", 13, cyan));
    }
    Button add =
        button(
            "＋  Novo alarme",
            () -> {
              draft = new Alarm();
              render();
            });
    add.setBackground(shape(pink, 20));
    body.addView(add);
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
    body.addView(
        button(
            "‹  Voltar",
            () ->
                new AlertDialog.Builder(this)
                    .setMessage("Descartar alterações?")
                    .setNegativeButton("Continuar editando", null)
                    .setPositiveButton(
                        "Descartar",
                        (v, i) -> {
                          draft = null;
                          render();
                        })
                    .show()));
    heading("Alarme para despertar");
    body.addView(
        button(
            "☀  " + draft.label,
            () ->
                input(
                    "Nome do alarme",
                    draft.label,
                    false,
                    s -> {
                      draft.label = s;
                      render();
                    })));
    Button time =
        button(
            String.format(Locale.getDefault(), "%02d : %02d", draft.hour, draft.minute),
            () ->
                new TimePickerDialog(
                        this,
                        (v, h, m) -> {
                          draft.hour = h;
                          draft.minute = m;
                          render();
                        },
                        draft.hour,
                        draft.minute,
                        true)
                    .show());
    time.setTextSize(38);
    time.setHeight(d(90));
    body.addView(time);
    note(remaining(Scheduler.next(draft, System.currentTimeMillis())));
    LinearLayout daysBox = box();
    CheckBox daily = new CheckBox(this);
    daily.setText("Todos os dias");
    daily.setTextColor(fg);
    daily.setChecked(draft.days == 127);
    daysBox.addView(daily);
    daily.setOnCheckedChangeListener(
        (v, on) -> {
          draft.days = on ? 127 : 0;
          render();
        });
    LinearLayout dr = new LinearLayout(this);
    String[] dn = {"D", "S", "T", "Q", "Q", "S", "S"};
    for (int i = 0; i < 7; i++) {
      final int bit = 1 << i;
      TextView day = text(dn[i], 18, (draft.days & bit) != 0 ? cyan : muted);
      day.setGravity(17);
      day.setContentDescription("Dia " + i);
      day.setBackground(shape((draft.days & bit) != 0 ? Color.rgb(24, 60, 68) : card, 10));
      LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(0, d(48), 1);
      dp.setMargins(d(2), 0, d(2), 0);
      dr.addView(day, dp);
      day.setOnClickListener(
          v -> {
            draft.days ^= bit;
            render();
          });
    }
    daysBox.addView(dr);
    LinearLayout ms = box();
    ms.addView(text("Missões para despertar  " + draft.missions.size() + "/5", 20, fg));
    for (int i = 0; i < draft.missions.size(); i++) {
      final int index = i;
      Alarm.Mission m = draft.missions.get(i);
      String name = names[Arrays.asList(types).indexOf(m.type)];
      ms.addView(
          button(
              (i + 1) + ". " + name + "  ×",
              () -> {
                draft.missions.remove(index);
                render();
              }));
      ms.addView(
          text(
              m.type.equals("photo")
                  ? "Foto de referência cadastrada"
                  : m.target.isEmpty() ? m.count + " repetições" : m.target,
              13,
              muted));
    }
    if (draft.missions.size() < 5) ms.addView(button("＋  Adicionar missão", this::chooseMission));
    body.addView(
        button(
            "Confirmar despertar: "
                + (draft.wakeCheckMinutes == 0 ? "desligado" : draft.wakeCheckMinutes + " min"),
            () ->
                choice(
                    "Confirmar que está acordado",
                    new String[] {"Desligado", "1 minuto", "3 minutos", "5 minutos"},
                    i -> {
                      draft.wakeCheckMinutes = new int[] {0, 1, 3, 5}[i];
                      render();
                    })));
    body.addView(
        button(
            "Proteção contra desligamento  ⓘ",
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Limite do Android")
                    .setMessage(
                        "Um app comum não pode impedir desligamento físico ou forçar que o celular"
                            + " permaneça ligado. O Desperta mantém o alarme em serviço ativo e"
                            + " reagenda após reiniciar. Veja otimização em Ajustes.")
                    .setPositiveButton("Entendi", null)
                    .show()));
    heading("Som do alarme");
    body.addView(
        button(
            draft.sound.isEmpty()
                ? "♫  Escolher som (padrão do aparelho)"
                : "♫  Alterar som personalizado",
            () -> {
              Intent in =
                  new Intent(Intent.ACTION_OPEN_DOCUMENT)
                      .setType("audio/*")
                      .addCategory(Intent.CATEGORY_OPENABLE);
              startActivityForResult(in, 40);
            }));
    body.addView(button("▶  Ouvir som", this::previewSound));
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
    toggle("Falar a hora", draft.timeReminder, v -> draft.timeReminder = v);
    toggle("Falar a previsão do tempo", draft.weatherReminder, v -> draft.weatherReminder = v);
    toggle("Falar o nome do alarme", draft.labelReminder, v -> draft.labelReminder = v);
    toggle("Efeito de volume máximo", draft.extraLoud, v -> draft.extraLoud = v);
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
                                    + java.text.DateFormat.getTimeInstance(3).format(new Date())
                                : i == 1
                                    ? getSharedPreferences("desperta", 0)
                                        .getString(
                                            "weather_text",
                                            "Configure a cidade e atualize a previsão em Ajustes.")
                                    : draft.label))));
    note(
        "A previsão usa a cidade configurada em Ajustes. Voz depende do mecanismo de fala"
            + " instalado.");
    heading("Personalizar");
    body.addView(
        button(
            "Soneca: " + draft.snoozeMinutes + " min · " + draft.snoozeLimit + " vezes",
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
            "Papel de parede: "
                + (draft.wallpaper.startsWith("content:") ? "Imagem escolhida" : draft.wallpaper),
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
    Button save =
        button(
            "Salvar alarme",
            () -> {
              Store.save(this, draft);
              schedule(draft);
              draft = null;
              stopSample();
              render();
            });
    save.setBackground(shape(pink, 18));
    page.addView(save);
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

  void input(String title, String initial, boolean number, Value action) {
    EditText e = new EditText(this);
    e.setText(initial);
    if (number) e.setInputType(2);
    new AlertDialog.Builder(this)
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
    new AlertDialog.Builder(this).setTitle(title).setItems(labels, (v, i) -> action.set(i)).show();
  }

  void toggle(String title, boolean checked, Bool action) {
    Switch s = new Switch(this);
    s.setText(title);
    s.setTextSize(16);
    s.setTextColor(fg);
    s.setPadding(0, d(16), 0, d(16));
    s.setChecked(checked);
    s.setOnCheckedChangeListener((v, b) -> action.set(b));
    body.addView(s);
  }

  void chooseMission() {
    choice(
        "Escolha sua missão",
        names,
        i -> {
          String type = types[i];
          if (type.equals("steps")
              && getSystemService(android.hardware.SensorManager.class)
                      .getDefaultSensor(android.hardware.Sensor.TYPE_STEP_DETECTOR)
                  == null) {
            new AlertDialog.Builder(this)
                .setMessage("Este aparelho não tem sensor de passos. Escolha outra missão.")
                .setPositiveButton("Entendi", null)
                .show();
            return;
          }
          if (type.equals("rhythm")
              && !android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            new AlertDialog.Builder(this)
                .setMessage(
                    "Instale ou ative um serviço de reconhecimento de voz no Android para usar esta"
                        + " missão.")
                .setPositiveButton("Entendi", null)
                .show();
            return;
          }
          if (type.equals("barcode") || type.equals("photo")) {
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
                    draft.missions.add(new Alarm.Mission(type, s, 3));
                    render();
                  }
                });
          } else if (type.equals("object")) {
            choice(
                "Objeto a encontrar (reconhecimento local)",
                new String[] {"Copo / caneca", "Livro", "Garrafa", "Cadeira", "Planta"},
                k -> {
                  draft.missions.add(
                      new Alarm.Mission(
                          type, new String[] {"Cup", "Book", "Bottle", "Chair", "Plant"}[k], 1));
                  render();
                });
          } else {
            input(
                "Quantidade (1–50)",
                "3",
                true,
                s -> {
                  draft.missions.add(new Alarm.Mission(type, "", bounded(s, 1, 50)));
                  render();
                });
          }
        });
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
    if (req == 42) {
      String target = data.getStringExtra("target");
      if (target != null && !target.isEmpty() && draft.missions.size() < 5)
        draft.missions.add(new Alarm.Mission(registerType, target, 1));
    }
    render();
  }

  @Override
  public void onBackPressed() {
    if (draft != null)
      new AlertDialog.Builder(this)
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
