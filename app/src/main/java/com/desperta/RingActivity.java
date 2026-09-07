package com.desperta;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.util.Locale;

/** Full-screen alarm surface. Playback and session state stay in AlarmService. */
public class RingActivity extends Activity {
  private static final int REQUEST_MISSION = 710;
  private static final int BG = Color.rgb(16, 26, 42);
  private static final int CARD = Color.rgb(27, 42, 61);
  private static final int FG = Color.rgb(255, 246, 231);
  private static final int MUTED = Color.rgb(173, 185, 201);
  private static final int PRIMARY = Color.rgb(246, 185, 93);
  private static final int PRIMARY_TEXT = Color.rgb(23, 32, 51);
  private static final int SUCCESS = Color.rgb(132, 213, 176);
  private static final int ERROR = Color.rgb(255, 173, 176);

  private int alarmId = -1;
  private boolean preview;
  private Alarm alarm;
  private int openMissionIndex = -1;
  private int previewMissionIndex;
  private TextView status;
  private TextView missionStatus;
  private Button primaryAction;
  private boolean missionOpen;
  private BroadcastReceiver missionReceiver;
  private final Handler handler = new Handler();

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    configureWindow();
    readIntent(getIntent());
    if (state != null) {
      missionOpen = state.getBoolean("missionOpen", false);
      openMissionIndex = state.getInt("openMissionIndex", -1);
      previewMissionIndex = Math.max(0, state.getInt("previewMissionIndex", 0));
    }
    alarm = Store.get(this, alarmId);
    if (alarm == null) {
      finish();
      return;
    }
    buildUi();
    registerMissionReceiver();
    if (!preview) {
      handler.post(this::startMissionIfNeeded);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putBoolean("missionOpen", missionOpen);
    outState.putInt("openMissionIndex", openMissionIndex);
    outState.putInt("previewMissionIndex", previewMissionIndex);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    readIntent(intent);
    Alarm incoming = Store.get(this, alarmId);
    if (incoming != null) {
      alarm = incoming;
      buildUi();
    }
  }

  private void readIntent(Intent intent) {
    if (intent == null) return;
    alarmId = intent.getIntExtra(Scheduler.EXTRA_ALARM_ID, alarmId);
    preview = intent.getBooleanExtra(Scheduler.EXTRA_PREVIEW, preview);
  }

  private void configureWindow() {
    Window window = getWindow();
    window.setStatusBarColor(Color.BLACK);
    window.setNavigationBarColor(Color.BLACK);
    window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    if (Build.VERSION.SDK_INT >= 30) {
      window.setDecorFitsSystemWindows(false);
      window
          .getDecorView()
          .setOnApplyWindowInsetsListener(
              (view, insets) -> {
                WindowInsets bars = insets;
                view.setPadding(
                    0, bars.getSystemWindowInsetTop(), 0, bars.getSystemWindowInsetBottom());
                return insets;
              });
    } else {
      window
          .getDecorView()
          .setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                  | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                  | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
  }

  private void buildUi() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20), dp(20), dp(20), dp(18));
    root.setBackground(wallpaper(alarm));

    LinearLayout header = new LinearLayout(this);
    header.setGravity(android.view.Gravity.CENTER_VERTICAL);
    ImageView sun = new ImageView(this);
    sun.setImageResource(R.drawable.ic_alarm);
    sun.setContentDescription("Desperta");
    sun.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    header.addView(sun, new LinearLayout.LayoutParams(dp(52), dp(52)));
    LinearLayout titleBox = new LinearLayout(this);
    titleBox.setOrientation(LinearLayout.VERTICAL);
    TextView title = label(preview ? "Prévia do alarme" : "Despertar", 23, FG);
    title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    titleBox.addView(title);
    TextView subtitle =
        label(
            alarm.label == null || alarm.label.isEmpty() ? "Hora de começar o dia" : alarm.label,
            15,
            MUTED);
    titleBox.addView(subtitle);
    header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
    root.addView(header);

    java.util.Calendar now = java.util.Calendar.getInstance();
    String displayedTime =
        preview
            ? String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)
            : String.format(
                Locale.getDefault(),
                "%02d:%02d",
                now.get(java.util.Calendar.HOUR_OF_DAY),
                now.get(java.util.Calendar.MINUTE));
    TextView time = label(displayedTime, 66, FG);
    time.setGravity(android.view.Gravity.CENTER);
    LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(-1, -2);
    timeParams.setMargins(0, dp(26), 0, dp(6));
    root.addView(time, timeParams);
    TextView ringing =
        label(preview ? "O som está sendo reproduzido" : "O alarme está tocando", 17, MUTED);
    ringing.setGravity(android.view.Gravity.CENTER);
    root.addView(ringing);

    ScrollView scroll = new ScrollView(this);
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-1, 0, 1);
    scroll.addView(content);
    root.addView(scroll, contentParams);

    if (!alarm.missions.isEmpty()) {
      LinearLayout missionCard = new LinearLayout(this);
      missionCard.setOrientation(LinearLayout.VERTICAL);
      missionCard.setPadding(dp(18), dp(15), dp(18), dp(15));
      missionCard.setBackground(round(CARD, 22));
      TextView heading = label(preview ? "Teste de missão" : "Missão para desligar", 19, FG);
      heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
      missionCard.addView(heading);
      missionStatus = label("Prepare-se…", 16, MUTED);
      missionCard.addView(missionStatus);
      content.addView(missionCard, margins(0, 18, 0, 10));
    } else {
      TextView hint =
          label(
              preview
                  ? "A prévia não altera histórico nem o próximo agendamento."
                  : "Você pode sonecar ou encerrar o alarme.",
              16,
              MUTED);
      content.addView(hint, margins(0, 28, 0, 10));
    }

    status = label("", 15, MUTED);
    status.setGravity(android.view.Gravity.CENTER);
    content.addView(status, margins(0, 5, 0, 10));

    LinearLayout actions = new LinearLayout(this);
    actions.setOrientation(LinearLayout.VERTICAL);
    String primaryText =
        preview
            ? (alarm.missions.isEmpty() ? "Parar prévia" : contextualMissionAction())
            : (alarm.missions.isEmpty() ? "Desligar alarme" : contextualMissionAction());
    Button primary =
        action(
            primaryText,
            PRIMARY,
            () -> {
              if (preview) {
                if (alarm.missions.isEmpty()) {
                  AlarmService.stopPreview(this, alarmId);
                  finish();
                } else {
                  launchMission(currentMissionIndex());
                }
              } else if (alarm.missions.isEmpty()) {
                AlarmService.dismiss(this, alarmId);
                finish();
              } else {
                // Keep this Activity as the receiver for the service's
                // mission-advanced broadcast while MissionActivity is
                // on top of it.
                launchMission(currentMissionIndex());
              }
            });
    primaryAction = primary;
    actions.addView(primary);
    if (!preview) {
      JSONObjectSession session = JSONObjectSession.read(this);
      boolean snoozeAvailable = alarm.snoozeLimit > session.snoozeCount;
      if (snoozeAvailable) {
        int remaining = Math.max(0, alarm.snoozeLimit - session.snoozeCount);
        String remainingLabel = remaining == 1 ? "resta 1" : "restam " + remaining;
        Button snooze =
            action(
                "Soneca de " + Math.max(1, alarm.snoozeMinutes) + " min · " + remainingLabel,
                CARD,
                () -> {
                  if (!AlarmService.canSnooze(this, alarmId)) {
                    setStatus("Limite de sonecas atingido.", ERROR);
                    return;
                  }
                  AlarmService.snooze(this, alarmId);
                  finish();
                });
        actions.addView(snooze);
      } else {
        TextView limit = label("Limite de sonecas atingido", 14, MUTED);
        limit.setGravity(android.view.Gravity.CENTER);
        actions.addView(limit);
      }
    } else if (!alarm.missions.isEmpty()) {
      // Preview needs its own exit affordance because the primary action advances the mission.
      actions.addView(
          action(
              "Parar prévia",
              CARD,
              () -> {
                AlarmService.stopPreview(this, alarmId);
                finish();
              }));
    }
    root.addView(actions, new LinearLayout.LayoutParams(-1, -2));
    setContentView(root);
  }

  private void registerMissionReceiver() {
    missionReceiver =
        new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getIntExtra(Scheduler.EXTRA_ALARM_ID, -1) != alarmId) {
              return;
            }
            boolean success = intent.getBooleanExtra(AlarmService.EXTRA_MISSION_SUCCESS, false);
            boolean done = intent.getBooleanExtra(AlarmService.EXTRA_MISSION_DONE, false);
            int index = intent.getIntExtra(AlarmService.EXTRA_MISSION_INDEX, 0);
            missionOpen = false;
            if (!success) {
              setStatus("Código lido, mas a missão continua pendente.", ERROR);
            } else if (done) {
              setStatus("Alarme desligado. Bom dia!", SUCCESS);
              handler.postDelayed(RingActivity.this::finish, 650L);
            } else {
              setStatus("Etapa concluída. Próxima missão…", SUCCESS);
              updateMissionStatus(index);
              updatePrimaryAction();
              handler.postDelayed(() -> launchMission(index), 180L);
            }
          }
        };
    IntentFilter filter = new IntentFilter(AlarmService.ACTION_MISSION_ADVANCED);
    ContextCompat.registerReceiver(
        this, missionReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
  }

  private void startMissionIfNeeded() {
    if (alarm == null || preview || alarm.missions.isEmpty() || missionOpen) {
      return;
    }
    launchMission(currentMissionIndex());
  }

  private int currentMissionIndex() {
    return preview ? Math.max(0, previewMissionIndex) : Math.max(0, JSONObjectSession.read(this).index);
  }

  private String contextualMissionAction() {
    if (alarm == null || alarm.missions.isEmpty()) return "Desligar alarme";
    int index = Math.min(Math.max(0, currentMissionIndex()), alarm.missions.size() - 1);
    String type = alarm.missions.get(index).type;
    if ("barcode".equals(type)) return "Escanear código";
    if ("photo".equals(type) || "object".equals(type) || "squat".equals(type)) {
      return "Abrir câmera";
    }
    if ("steps".equals(type)) return "Iniciar passos";
    if ("shake".equals(type)) return "Iniciar agitação";
    if ("rhythm".equals(type)) return "Iniciar microfone";
    if ("typing".equals(type)) return "Digitar resposta";
    if ("math".equals(type)) return "Resolver conta";
    if ("colors".equals(type)) return "Encontrar cor";
    return "Continuar missão";
  }

  private void launchMission(int index) {
    if (alarm == null || index < 0 || index >= alarm.missions.size() || missionOpen) {
      return;
    }
    Alarm.Mission mission = alarm.missions.get(index);
    missionOpen = true;
    openMissionIndex = index;
    updateMissionStatus(index);
    updatePrimaryAction();
    Intent intent =
        new Intent()
            .setClassName(this, getPackageName() + ".MissionActivity")
            .putExtra("type", mission.type)
            .putExtra("target", mission.target)
            .putStringArrayListExtra(MissionActivity.EXTRA_TARGETS, mission.acceptedCodes())
            .putExtra("count", mission.count)
            .putExtra("alarm_id", preview ? -1 : alarmId)
            .putExtra("mission_index", index)
            .putExtra("preview", false)
            .putExtra("mode", "solve");
    try {
      startActivityForResult(intent, REQUEST_MISSION);
    } catch (RuntimeException missingMissionScreen) {
      missionOpen = false;
      openMissionIndex = -1;
      setStatus("Tela da missão indisponível", ERROR);
    }
  }

  private void updateMissionStatus(int index) {
    if (missionStatus != null && alarm != null) {
      if (alarm.missions.size() <= 1) {
        missionStatus.setText("Conclua a missão para desligar o alarme");
      } else {
        missionStatus.setText(
            "Etapa " + Math.min(index + 1, alarm.missions.size()) + " de " + alarm.missions.size());
      }
    }
  }

  private void updatePrimaryAction() {
    if (primaryAction == null || alarm == null || alarm.missions.isEmpty()) return;
    primaryAction.setText(contextualMissionAction());
    primaryAction.setContentDescription(contextualMissionAction());
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode != REQUEST_MISSION) return;
    missionOpen = false;
    int completedMissionIndex = openMissionIndex;
    openMissionIndex = -1;
    if (resultCode == RESULT_OK) {
      if (preview) {
        int next = completedMissionIndex + 1;
        if (next >= alarm.missions.size()) {
          setStatus("Alarme desligado. Bom dia!", SUCCESS);
          AlarmService.stopPreview(this, alarmId);
          handler.postDelayed(
              RingActivity.this::finish,
              650L);
        } else {
          previewMissionIndex = next;
          setStatus("Etapa concluída. Próxima missão…", SUCCESS);
          updateMissionStatus(next);
          updatePrimaryAction();
          handler.postDelayed(() -> launchMission(next), 180L);
        }
        return;
      }
      // MissionActivity reports successful alarm-mode missions directly to the foreground
      // service as soon as the scanner/mission accepts them. Older or standalone mission
      // screens still return the result here, so retain this as the compatibility fallback.
      boolean serviceReported =
          data != null && data.getBooleanExtra(MissionActivity.RESULT_SERVICE_REPORTED, false);
      if (!serviceReported) AlarmService.missionResult(this, alarmId, completedMissionIndex, true);
    } else {
      // Cancellation and a wrong answer leave the ringing service alive.
      setStatus("Missão não concluída. Tente novamente.", ERROR);
    }
  }

  @Override
  public void onBackPressed() {
    if (preview) {
      AlarmService.stopPreview(this, alarmId);
      finish();
    } else {
      setStatus("Conclua a missão ou use Soneca", MUTED);
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (!preview
        && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
            || keyCode == KeyEvent.KEYCODE_VOLUME_UP
            || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)) {
      setStatus("O volume é controlado pelo alarme", MUTED);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    if (missionReceiver != null) {
      try {
        unregisterReceiver(missionReceiver);
      } catch (IllegalArgumentException ignored) {
      }
    }
    super.onDestroy();
  }

  private void setStatus(String text, int color) {
    if (status != null) {
      status.setText(text);
      status.setTextColor(color);
    }
  }

  private TextView label(String text, int size, int color) {
    TextView view = new TextView(this);
    view.setText(text);
    view.setTextSize(size);
    view.setTextColor(color);
    view.setPadding(0, dp(4), 0, dp(4));
    return view;
  }

  private Button action(String text, int color, Runnable callback) {
    Button button = new Button(this);
    button.setText(text);
    button.setTextSize(17);
    button.setTextColor(color == PRIMARY ? PRIMARY_TEXT : FG);
    button.setAllCaps(false);
    button.setMinHeight(dp(55));
    button.setBackground(round(color, 18));
    button.setOnClickListener(view -> callback.run());
    button.setContentDescription(text);
    return button;
  }

  private GradientDrawable round(int color, int radius) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(dp(radius));
    return drawable;
  }

  private Drawable wallpaper(Alarm current) {
    String value = current == null ? "Aurora" : current.wallpaper;
    if (value != null && value.startsWith("content:")) {
      try {
        Bitmap bitmap =
            BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.parse(value)));
        if (bitmap != null) {
          BitmapDrawable image = new BitmapDrawable(getResources(), bitmap);
          image.setGravity(android.view.Gravity.FILL);
          return new android.graphics.drawable.LayerDrawable(
              new Drawable[] {image, new android.graphics.drawable.ColorDrawable(0xB809090B)});
        }
      } catch (RuntimeException | java.io.IOException ignored) {
        // Fall back to the dark built-in wallpaper if access expired.
      }
    }
    int[] colors;
    if ("Oceano".equalsIgnoreCase(value)) {
      colors = new int[] {Color.rgb(8, 24, 38), Color.rgb(10, 78, 96), BG};
    } else if ("Noite".equalsIgnoreCase(value)) {
      colors = new int[] {Color.rgb(24, 16, 45), Color.rgb(11, 11, 17), BG};
    } else {
      colors = new int[] {Color.rgb(38, 39, 68), Color.rgb(23, 36, 57), BG};
    }
    return new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
  }

  private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
    return params;
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
  }

  /** Tiny adapter to read the service's persisted mission cursor. */
  private static final class JSONObjectSession {
    final int index;
    final int snoozeCount;

    JSONObjectSession(int index, int snoozeCount) {
      this.index = index;
      this.snoozeCount = snoozeCount;
    }

    static JSONObjectSession read(Context context) {
      android.content.SharedPreferences p = Store.prefs(context);
      String value = p.getString(Store.ACTIVE_SESSION_KEY, null);
      if (value == null) return new JSONObjectSession(0, 0);
      try {
        org.json.JSONObject session = new org.json.JSONObject(value);
        return new JSONObjectSession(
            Math.max(0, session.optInt("missionIndex", 0)),
            Math.max(0, session.optInt("snoozeCount", 0)));
      } catch (org.json.JSONException ignored) {
        return new JSONObjectSession(0, 0);
      }
    }
  }
}
