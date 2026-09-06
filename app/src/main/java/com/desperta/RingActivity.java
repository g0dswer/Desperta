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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.util.Locale;

/** Full-screen alarm surface. Playback and session state stay in AlarmService. */
public class RingActivity extends Activity {
  private static final int REQUEST_MISSION = 710;
  private static final int BG = Color.rgb(9, 9, 11);
  private static final int CARD = Color.rgb(27, 27, 31);
  private static final int FG = Color.rgb(250, 250, 252);
  private static final int MUTED = Color.rgb(166, 166, 177);
  private static final int PINK = Color.rgb(255, 49, 89);
  private static final int CYAN = Color.rgb(35, 201, 225);

  private int alarmId = -1;
  private boolean preview;
  private Alarm alarm;
  private TextView status;
  private TextView missionStatus;
  private boolean missionOpen;
  private BroadcastReceiver missionReceiver;
  private final Handler handler = new Handler();

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    configureWindow();
    readIntent(getIntent());
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
    TextView sun = label("☀", 34, CYAN);
    header.addView(sun, new LinearLayout.LayoutParams(dp(52), -2));
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

    TextView time =
        label(String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute), 66, FG);
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

    if (!preview && !alarm.missions.isEmpty()) {
      LinearLayout missionCard = new LinearLayout(this);
      missionCard.setOrientation(LinearLayout.VERTICAL);
      missionCard.setPadding(dp(18), dp(15), dp(18), dp(15));
      missionCard.setBackground(round(CARD, 22));
      TextView heading = label("Missão para desligar", 19, FG);
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

    status = label("", 15, CYAN);
    status.setGravity(android.view.Gravity.CENTER);
    content.addView(status, margins(0, 5, 0, 10));

    LinearLayout actions = new LinearLayout(this);
    actions.setOrientation(LinearLayout.VERTICAL);
    String primaryText =
        preview
            ? "Parar prévia"
            : (alarm.missions.isEmpty() ? "Encerrar alarme" : "Continuar missão");
    Button primary =
        action(
            primaryText,
            PINK,
            () -> {
              if (preview) {
                AlarmService.stopPreview(this, alarmId);
                finish();
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
    actions.addView(primary);
    if (!preview) {
      JSONObjectSession session = JSONObjectSession.read(this);
      boolean snoozeAvailable = alarm.snoozeLimit > session.snoozeCount;
      if (snoozeAvailable) {
        Button snooze =
            action(
                "Soneca de " + Math.max(1, alarm.snoozeMinutes) + " min",
                CARD,
                () -> {
                  if (!AlarmService.canSnooze(this, alarmId)) {
                    setStatus("Limite de sonecas atingido.", PINK);
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
      if (!alarm.missions.isEmpty()) {
        Button retry =
            action("Tentar missão novamente", CARD, () -> launchMission(currentMissionIndex()));
        actions.addView(retry);
      }
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
              setStatus("Ainda não. Tente novamente.", PINK);
            } else if (done) {
              setStatus("Missão concluída", CYAN);
              finish();
            } else {
              setStatus("Muito bem. Próxima missão…", CYAN);
              updateMissionStatus(index);
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
    return Math.max(0, JSONObjectSession.read(this).index);
  }

  private void launchMission(int index) {
    if (alarm == null || index < 0 || index >= alarm.missions.size() || missionOpen) {
      return;
    }
    Alarm.Mission mission = alarm.missions.get(index);
    missionOpen = true;
    updateMissionStatus(index);
    Intent intent =
        new Intent()
            .setClassName(this, getPackageName() + ".MissionActivity")
            .putExtra("type", mission.type)
            .putExtra("target", mission.target)
            .putExtra("count", mission.count)
            .putExtra("alarm_id", alarmId)
            .putExtra("mission_index", index)
            .putExtra("preview", false)
            .putExtra("mode", "solve");
    try {
      startActivityForResult(intent, REQUEST_MISSION);
    } catch (RuntimeException missingMissionScreen) {
      missionOpen = false;
      setStatus("Tela da missão indisponível", PINK);
    }
  }

  private void updateMissionStatus(int index) {
    if (missionStatus != null && alarm != null) {
      missionStatus.setText(
          "Missão " + Math.min(index + 1, alarm.missions.size()) + " de " + alarm.missions.size());
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode != REQUEST_MISSION) return;
    missionOpen = false;
    if (resultCode == RESULT_OK) {
      AlarmService.missionResult(this, alarmId, true);
    } else {
      // Cancellation and a wrong answer leave the ringing service alive.
      setStatus("Missão não concluída. Tente novamente.", PINK);
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
    button.setTextColor(FG);
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
          return new android.graphics.drawable.LayerDrawable(new Drawable[] {image, new android.graphics.drawable.ColorDrawable(0xB809090B)});
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
      colors = new int[] {Color.rgb(56, 19, 44), Color.rgb(22, 25, 48), BG};
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
