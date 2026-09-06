package com.desperta;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * A real, self-contained mission screen. Every successful result is backed by a scanner, camera, ML
 * Kit, sensor, speech recognizer, or exact user input; a decorative button cannot complete an
 * alarm.
 */
public class MissionActivity extends Activity implements SensorEventListener {
  public static final String EXTRA_TYPE = "type";
  public static final String EXTRA_TARGET = "target";

  /** All accepted values for a reusable QR/barcode mission. */
  public static final String EXTRA_TARGETS = "targets";

  public static final String EXTRA_COUNT = "count";
  public static final String EXTRA_MODE = "mode";
  public static final String RESULT_TARGET = "mission_target";

  /** Marks a result that was already delivered directly to the foreground alarm service. */
  public static final String RESULT_SERVICE_REPORTED = "mission_service_reported";

  private static final int REQUEST_CAMERA_PERMISSION = 101;
  private static final int REQUEST_AUDIO_PERMISSION = 102;
  private static final int REQUEST_ACTIVITY_RECOGNITION = 103;
  private static final int REQUEST_CAMERA = 105;
  private static final int CAMERA_PHOTO = 1;
  private static final int CAMERA_OBJECT = 2;
  private static final int CAMERA_SQUAT = 3;
  private static final int HASH_SIZE = 8;

  private static final int BG = Color.rgb(12, 12, 15);
  private static final int CARD = Color.rgb(28, 28, 32);
  private static final int CARD_ALT = Color.rgb(42, 42, 48);
  private static final int WHITE = Color.rgb(246, 246, 248);
  private static final int MUTED = Color.rgb(178, 178, 187);
  private static final int PINK = Color.rgb(255, 45, 83);
  private static final int CYAN = Color.rgb(41, 193, 224);

  private static final int[] COLOR_VALUES = {
    Color.rgb(245, 72, 78), Color.rgb(255, 155, 54), Color.rgb(255, 216, 74),
    Color.rgb(75, 207, 103), Color.rgb(52, 189, 224), Color.rgb(82, 111, 236),
    Color.rgb(169, 91, 224), Color.rgb(244, 103, 172)
  };
  private static final String[] COLOR_NAMES = {
    "red", "orange", "yellow", "green", "cyan", "blue", "purple", "pink"
  };
  private static final String[] COLOR_DISPLAY_NAMES = {
    "vermelho", "laranja", "amarelo", "verde", "ciano", "azul", "roxo", "rosa"
  };

  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final Random random = new Random();
  private final Map<Integer, String> permissionMessages = new HashMap<>();

  private String type;
  private String target;
  private final ArrayList<String> targets = new ArrayList<>();
  private String mode;
  private int alarmId = -1;
  private int missionIndex = -1;
  private int requiredCount;
  private int progress;
  private boolean finishing;
  private boolean restored;
  private String lastStatus = "";

  private LinearLayout content;
  private TextView titleView;
  private TextView promptView;
  private TextView statusView;
  private TextView progressView;
  private TextView typingTargetView;
  private TextView rhythmBeatView;
  private Button primaryButton;
  private EditText input;

  private int pendingCameraPurpose;
  private boolean cameraPending;
  private boolean scannerPending;
  private boolean sensorWanted;
  private boolean sensorActive;
  private long lastShake = -1L;
  private SensorManager sensorManager;

  private int mathA;
  private int mathB;
  private char mathOperator = '+';
  private int mathExpected;
  private boolean mathInitialized;
  private int expectedColor;
  private String expectedColorName;
  private int colorRound;
  private int colorSeed;
  private GridLayout colorGrid;

  private MissionLogic.SquatState squatState =
      new MissionLogic.SquatState(MissionLogic.SquatPhase.UP, 0);
  private SpeechRecognizer speechRecognizer;
  private long lastSpeechMatch = -1L;
  private long rhythmStartAt = -1L;
  private long currentSpeechStart = -1L;
  private Runnable rhythmPulse;
  private ImageLabeler imageLabeler;
  private PoseDetector poseDetector;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    configureWindow();

    Intent launch = getIntent();
    type =
        MissionLogic.canonicalType(
            firstNonEmpty(
                launch.getStringExtra(EXTRA_TYPE), launch.getStringExtra("mission_type")));
    target =
        firstNonEmpty(launch.getStringExtra(EXTRA_TARGET), launch.getStringExtra("mission_target"));
    readTargets(launch.getStringArrayListExtra(EXTRA_TARGETS));
    if (targets.isEmpty() && target != null && !target.trim().isEmpty()) {
      targets.add(target);
    }
    mode = firstNonEmpty(launch.getStringExtra(EXTRA_MODE), "alarm").toLowerCase(Locale.ROOT);
    alarmId = launch.getIntExtra("alarm_id", -1);
    missionIndex = launch.getIntExtra("mission_index", -1);
    requiredCount = MissionLogic.safeCount(launch.getIntExtra(EXTRA_COUNT, 1));
    permissionMessages.put(
        REQUEST_CAMERA_PERMISSION, "A permissão da câmera é necessária para esta missão.");
    permissionMessages.put(
        REQUEST_AUDIO_PERMISSION, "A permissão do microfone é necessária para esta missão.");
    permissionMessages.put(
        REQUEST_ACTIVITY_RECOGNITION,
        "A permissão de atividade física é necessária para contar passos.");

    if (savedInstanceState != null) restoreState(savedInstanceState);
    buildShell();
    configureMission();
    if (savedInstanceState != null && savedInstanceState.containsKey("feedback"))
      setStatus(savedInstanceState.getString("feedback", ""));
  }

  private void configureWindow() {
    Window window = getWindow();
    window.setStatusBarColor(Color.BLACK);
    window.setNavigationBarColor(Color.BLACK);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      window.getDecorView().setSystemUiVisibility(0);
    }
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  }

  private void restoreState(Bundle state) {
    restored = true;
    String restoredTarget = state.getString(EXTRA_TARGET);
    if (restoredTarget != null) target = restoredTarget;
    ArrayList<String> restoredTargets = state.getStringArrayList(EXTRA_TARGETS);
    if (restoredTargets != null) {
      targets.clear();
      readTargets(restoredTargets);
    }
    if (targets.isEmpty() && target != null && !target.trim().isEmpty()) {
      targets.add(target);
    }
    alarmId = state.getInt("alarm_id", alarmId);
    missionIndex = state.getInt("mission_index", missionIndex);
    progress = Math.max(0, state.getInt("progress", 0));
    mathA = state.getInt("mathA", 0);
    mathB = state.getInt("mathB", 0);
    mathOperator = state.getChar("mathOperator", '+');
    mathExpected = state.getInt("mathExpected", 0);
    mathInitialized = state.getBoolean("mathInitialized", mathExpected != 0);
    expectedColor = state.getInt("expectedColor", 0);
    expectedColorName = state.getString("expectedColorName", "");
    colorRound = state.getInt("colorRound", 0);
    colorSeed = state.getInt("colorSeed", 0);
    lastShake = state.getLong("lastShake", -1L);
    int squatPhase = state.getInt("squatPhase", 0);
    int squatRepetitions = state.getInt("squatRepetitions", 0);
    squatState =
        new MissionLogic.SquatState(
            squatPhase == 1 ? MissionLogic.SquatPhase.DOWN : MissionLogic.SquatPhase.UP,
            squatRepetitions);
  }

  private void buildShell() {
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setBackgroundColor(BG);

    LinearLayout outer = new LinearLayout(this);
    outer.setOrientation(LinearLayout.VERTICAL);
    outer.setPadding(dp(18), dp(18), dp(18), dp(24));
    scroll.addView(
        outer,
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    LinearLayout toolbar = new LinearLayout(this);
    toolbar.setGravity(Gravity.CENTER_VERTICAL);
    TextView close = label("×", 36, WHITE);
    close.setGravity(Gravity.CENTER);
    close.setContentDescription("Fechar missão");
    close.setOnClickListener(v -> cancelMission());
    toolbar.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
    titleView = label("Missão do despertador", 22, WHITE);
    titleView.setGravity(Gravity.CENTER);
    toolbar.addView(titleView, new LinearLayout.LayoutParams(0, dp(48), 1));
    TextView spacer = label("", 22, WHITE);
    toolbar.addView(spacer, new LinearLayout.LayoutParams(dp(48), dp(48)));
    outer.addView(toolbar);

    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(20), dp(20), dp(20), dp(20));
    card.setBackground(round(CARD, 28));
    outer.addView(
        card,
        marginParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 12, 0, 0));

    promptView = label("Complete a missão para desligar o alarme.", 18, WHITE);
    promptView.setGravity(Gravity.CENTER_HORIZONTAL);
    card.addView(
        promptView,
        marginParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 12));
    progressView = label("", 14, MUTED);
    progressView.setGravity(Gravity.CENTER_HORIZONTAL);
    card.addView(
        progressView,
        marginParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0, 0, 12));
    content = card;

    statusView = label("", 15, MUTED);
    statusView.setGravity(Gravity.CENTER);
    outer.addView(
        statusView,
        marginParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 14, 0, 0));

    scroll.setOnApplyWindowInsetsListener(
        (view, insets) -> {
          int top;
          int bottom;
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            top = bars.top;
            bottom = bars.bottom;
          } else {
            //noinspection deprecation
            top = insets.getSystemWindowInsetTop();
            //noinspection deprecation
            bottom = insets.getSystemWindowInsetBottom();
          }
          view.setPadding(0, top, 0, bottom);
          return insets;
        });
    setContentView(scroll);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) scroll.requestApplyInsets();
    updateProgress();
  }

  private void configureMission() {
    titleView.setText(titleFor(type));
    switch (type) {
      case "barcode":
        promptView.setText(
            isRegisterMode()
                ? "Escaneie o código de barras que será usado neste alarme."
                : "Escaneie o código de barras registrado para desligar o alarme.");
        addPrimaryButton("Escanear QR / código de barras", v -> launchBarcodeScanner());
        setStatus("A entrada aceita é exclusivamente o scanner da câmera.");
        if (!restored) mainHandler.postDelayed(this::launchBarcodeScanner, 300L);
        break;
      case "photo":
        promptView.setText(
            isRegisterMode()
                ? "Tire uma foto real. Ela será armazenada de forma privada como alvo da missão."
                : "Tire uma foto que corresponda à imagem registrada neste alarme.");
        addPrimaryButton("Abrir câmera", v -> requestCameraAndLaunch(CAMERA_PHOTO));
        setStatus("A missão compara uma assinatura visual; a iluminação pode afetar o resultado.");
        break;
      case "object":
        promptView.setText(
            isRegisterMode()
                ? "Fotografe um objeto. O primeiro objeto reconhecido será o alvo."
                : "Fotografe o objeto registrado para que o classificador do aparelho o"
                    + " verifique.");
        addPrimaryButton("Abrir câmera", v -> requestCameraAndLaunch(CAMERA_OBJECT));
        setStatus("A verificação precisa de um objeto visível e bem iluminado.");
        imageLabeler =
            ImageLabeling.getClient(
                new ImageLabelerOptions.Builder().setConfidenceThreshold(0.55f).build());
        break;
      case "squat":
        promptView.setText(
            "Alterne entre agachado e em pé. A repetição só conta depois que as duas poses forem"
                + " detectadas.");
        addPrimaryButton("Capturar pose", v -> requestCameraAndLaunch(CAMERA_SQUAT));
        setStatus("A detecção roda em cada captura real. Mantenha o corpo inteiro visível.");
        poseDetector =
            PoseDetection.getClient(
                new PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                    .build());
        break;
      case "steps":
        promptView.setText("Caminhe até o contador atingir a quantidade necessária de passos.");
        addPrimaryButton("Iniciar sensor de passos", v -> startSteps());
        setStatus("Caminhe com o celular para registrar os passos.");
        if (!restored) mainHandler.postDelayed(this::startSteps, 250L);
        break;
      case "shake":
        promptView.setText(
            "Agite o celular com firmeza até o contador atingir a quantidade necessária.");
        addPrimaryButton("Iniciar sensor de agitação", v -> startShake());
        setStatus("Cada agitação passa por um intervalo de segurança.");
        break;
      case "rhythm":
        String spokenTarget = rhythmWord();
        promptView.setText("Diga “" + spokenTarget + "” no ritmo de " + rhythmBpm() + " BPM.");
        addPrimaryButton("Iniciar microfone", v -> startRhythm());
        rhythmBeatView = label("●  Ritmo: " + rhythmBpm() + " BPM", 15, CYAN);
        rhythmBeatView.setGravity(Gravity.CENTER);
        content.addView(
            rhythmBeatView, marginParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40), 0, 10, 0, 0));
        setStatus(
            "O reconhecimento de voz precisa ouvir a palavra; o resultado não pode ser editado.");
        break;
      case "typing":
        if (target == null || target.isEmpty()) target = "DESPERTA";
        promptView.setText("Digite exatamente o texto mostrado abaixo.");
        typingTargetView = label("Texto a digitar: " + target, 20, CYAN);
        typingTargetView.setGravity(Gravity.CENTER);
        content.addView(
            typingTargetView,
            marginParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0,
                10,
                0,
                0));
        addInputAndSubmit("Digite: " + target, "Verificar texto", this::checkTyping);
        break;
      case "math":
        prepareMathQuestion();
        addInputAndSubmit("", "Verificar resposta", this::checkMath);
        renderMathQuestion();
        break;
      case "colors":
        promptView.setText("Encontre o bloco da cor solicitada.");
        setupColorRound();
        break;
      default:
        promptView.setText("Este tipo de missão não está disponível neste aparelho.");
        setStatus("Não há um fluxo de conclusão habilitado para esta missão.");
        break;
    }
  }

  private String titleFor(String missionType) {
    if ("math".equals(missionType)) return "Matemática";
    if ("typing".equals(missionType)) return "Digitação";
    if ("shake".equals(missionType)) return "Sacudir";
    if ("squat".equals(missionType)) return "Agachamento";
    if ("photo".equals(missionType)) return "Foto";
    if ("barcode".equals(missionType)) return "QR / código de barras";
    if ("colors".equals(missionType)) return "Encontre as cores";
    if ("steps".equals(missionType)) return "Missão de passos";
    if ("rhythm".equals(missionType)) return "Diga a palavra no ritmo";
    if ("object".equals(missionType)) return "Caça ao objeto";
    if (missionType == null || missionType.isEmpty()) return "Missão do despertador";
    return Character.toUpperCase(missionType.charAt(0)) + missionType.substring(1);
  }

  private boolean isRegisterMode() {
    return "register".equals(mode);
  }

  private void addPrimaryButton(String text, View.OnClickListener listener) {
    primaryButton = new Button(this);
    primaryButton.setText(text);
    primaryButton.setTextColor(WHITE);
    primaryButton.setTextSize(16);
    primaryButton.setAllCaps(false);
    primaryButton.setBackground(round(PINK, 20));
    primaryButton.setOnClickListener(listener);
    content.addView(
        primaryButton, marginParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54), 0, 14, 0, 0));
  }

  private void addInputAndSubmit(String hint, String buttonText, View.OnClickListener listener) {
    input = new EditText(this);
    input.setTextColor(WHITE);
    input.setHintTextColor(MUTED);
    input.setHint(hint);
    input.setTextSize(18);
    input.setSingleLine(true);
    input.setPadding(dp(16), 0, dp(16), 0);
    input.setBackground(round(CARD_ALT, 14));
    content.addView(input, marginParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54), 0, 14, 0, 0));
    addPrimaryButton(buttonText, listener);
  }

  private void checkTyping(View ignored) {
    if (MissionLogic.typingMatches(target, input == null ? null : input.getText().toString())) {
      acceptOneProgress();
      if (input != null) input.setText("");
    } else {
      setStatus("O texto precisa ser exatamente igual.");
    }
  }

  private void prepareMathQuestion() {
    if (restored && mathInitialized) return;
    MathQuestion parsed = parseMathTarget(target);
    if (parsed != null) {
      mathA = parsed.a;
      mathB = parsed.b;
      mathOperator = parsed.operator;
    } else {
      mathA = 2 + random.nextInt(19);
      mathB = 2 + random.nextInt(19);
      mathOperator = random.nextBoolean() ? '+' : '-';
      if (mathOperator == '-' && mathB > mathA) {
        int swap = mathA;
        mathA = mathB;
        mathB = swap;
      }
    }
    mathExpected = mathOperator == '+' ? mathA + mathB : mathA - mathB;
    mathInitialized = true;
  }

  private void renderMathQuestion() {
    promptView.setText("Quanto é " + mathA + " " + mathOperator + " " + mathB + "?");
  }

  private void checkMath(View ignored) {
    if (MissionLogic.mathAnswerMatches(
        mathExpected, input == null ? null : input.getText().toString())) {
      acceptOneProgress();
      if (!MissionLogic.isComplete(progress, requiredCount)) {
        restored = false;
        prepareMathQuestion();
        renderMathQuestion();
      }
      if (input != null) input.setText("");
    } else {
      setStatus("Essa resposta não está correta. Tente novamente.");
    }
  }

  private void setupColorRound() {
    if (colorGrid != null) content.removeView(colorGrid);
    if (expectedColor == 0 || !restored && colorRound == 0) {
      colorSeed = random.nextInt();
      int index = Math.floorMod(colorSeed + colorRound, COLOR_VALUES.length);
      expectedColor = COLOR_VALUES[index];
      expectedColorName = COLOR_NAMES[index];
    }
    promptView.setText("Toque no bloco " + displayColorName(expectedColorName) + ".");
    GridLayout grid = new GridLayout(this);
    colorGrid = grid;
    grid.setColumnCount(2);
    grid.setRowCount((COLOR_VALUES.length + 1) / 2);
    grid.setUseDefaultMargins(true);
    for (int i = 0; i < COLOR_VALUES.length; i++) {
      final int color = COLOR_VALUES[i];
      TextView tile = label(COLOR_DISPLAY_NAMES[i].toUpperCase(Locale.ROOT), 15, Color.WHITE);
      tile.setGravity(Gravity.CENTER);
      tile.setBackground(round(color, 18));
      tile.setContentDescription("Bloco da cor " + COLOR_DISPLAY_NAMES[i]);
      tile.setOnClickListener(
          v -> {
            if (MissionLogic.colorMatches(expectedColor, color, 0)) {
              acceptOneProgress();
              if (!MissionLogic.isComplete(progress, requiredCount)) {
                colorRound++;
                expectedColor = 0;
                setupColorRound();
              }
            } else {
              setStatus("Esse bloco não tem a cor solicitada.");
            }
          });
      GridLayout.LayoutParams params = new GridLayout.LayoutParams();
      params.width = 0;
      params.height = dp(66);
      params.columnSpec = GridLayout.spec(i % 2, 1f);
      params.rowSpec = GridLayout.spec(i / 2);
      params.setMargins(dp(4), dp(4), dp(4), dp(4));
      grid.addView(tile, params);
    }
    content.addView(
        grid,
        marginParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, 14, 0, 0));
  }

  private String displayColorName(String internalName) {
    for (int i = 0; i < COLOR_NAMES.length; i++) {
      if (COLOR_NAMES[i].equals(internalName)) return COLOR_DISPLAY_NAMES[i];
    }
    return internalName == null ? "solicitada" : internalName;
  }

  private void launchBarcodeScanner() {
    if (finishing || scannerPending) return;
    IntentIntegrator integrator = new IntentIntegrator(this);
    integrator.setCaptureActivity(AlarmCaptureActivity.class);
    integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
    integrator.setPrompt(
        isRegisterMode()
            ? "Escaneie o código de barras para registrá-lo"
            : "Escaneie o código de barras registrado");
    integrator.setBeepEnabled(true);
    // Keep the embedded scanner's calibrated landscape camera surface. The custom activity adds
    // lock-screen flags without changing the decode geometry used by the device camera pipeline.
    integrator.setOrientationLocked(false);
    scannerPending = true;
    try {
      integrator.initiateScan();
    } catch (RuntimeException error) {
      scannerPending = false;
      setStatus("O scanner de código de barras não pôde ser aberto neste aparelho.");
    }
  }

  private void requestCameraAndLaunch(int purpose) {
    pendingCameraPurpose = purpose;
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this, new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
      return;
    }
    launchCamera(purpose);
  }

  private void launchCamera(int purpose) {
    if (cameraPending || finishing) return;
    Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (camera.resolveActivity(getPackageManager()) == null) {
      setStatus("Não há um aplicativo de câmera disponível neste aparelho.");
      return;
    }
    pendingCameraPurpose = purpose;
    cameraPending = true;
    try {
      startActivityForResult(camera, REQUEST_CAMERA);
    } catch (RuntimeException error) {
      cameraPending = false;
      setStatus("A câmera não pôde ser aberta.");
    }
  }

  private void handleBarcodeResult(String contents) {
    if (contents == null || contents.trim().isEmpty()) {
      setStatus("Nenhum código foi lido. Tente novamente.");
      return;
    }
    if (isRegisterMode()) {
      completeMission(contents);
    } else if (matchesAnyBarcode(contents)) {
      completeMission(null);
    } else {
      setStatus("Esse código de barras não corresponde ao código registrado.");
    }
  }

  private void handleCapturedBitmap(Bitmap bitmap) {
    if (bitmap == null || bitmap.getWidth() < 2 || bitmap.getHeight() < 2) {
      setStatus("A câmera não retornou uma imagem utilizável. Tente novamente.");
      return;
    }
    if (pendingCameraPurpose == CAMERA_PHOTO) {
      handlePhoto(bitmap);
    } else if (pendingCameraPurpose == CAMERA_OBJECT) {
      handleObject(bitmap);
    } else if (pendingCameraPurpose == CAMERA_SQUAT) {
      handleSquat(bitmap);
    }
  }

  private void handlePhoto(Bitmap bitmap) {
    if (!hasMeaningfulPhotoDetail(bitmap)) {
      setStatus("A imagem está uniforme demais. Fotografe um objeto ou cenário com detalhes.");
      return;
    }
    if (isRegisterMode()) {
      File saved = savePrivatePhoto(bitmap);
      if (saved == null) {
        setStatus("A foto não pôde ser salva de forma privada.");
      } else {
        completeMission(saved.getAbsolutePath());
      }
      return;
    }
    File registered = target == null ? null : new File(target);
    if (registered == null || !registered.isFile()) {
      setStatus("A foto registrada não está disponível neste aparelho.");
      return;
    }
    Bitmap expected = android.graphics.BitmapFactory.decodeFile(registered.getAbsolutePath());
    if (expected == null) {
      setStatus("A foto registrada não pôde ser lida.");
      return;
    }
    boolean match = MissionLogic.photoMatches(bitmapHash(expected), bitmapHash(bitmap));
    expected.recycle();
    if (match) {
      completeMission(null);
    } else {
      setStatus("A foto não corresponde à imagem registrada. Tente novamente.");
    }
  }

  private void handleObject(Bitmap bitmap) {
    if (imageLabeler == null) {
      setStatus("O reconhecimento de objetos não está disponível neste aparelho.");
      return;
    }
    imageLabeler
        .process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener(
            labels -> {
              ImageLabel best = bestLabel(labels);
              if (best == null) {
                setStatus(
                    "Nenhum objeto reconhecível foi encontrado. Aproxime-se e melhore a"
                        + " iluminação.");
                return;
              }
              if (isRegisterMode()) {
                completeMission(best.getText());
              } else if (MissionLogic.labelMatches(target, best.getText())) {
                completeMission(null);
              } else {
                setStatus("Detectado “" + best.getText() + "”, mas não é o objeto registrado.");
              }
            })
        .addOnFailureListener(
            error -> setStatus("O reconhecimento do objeto falhou. Tente novamente."));
  }

  private ImageLabel bestLabel(List<ImageLabel> labels) {
    ImageLabel best = null;
    if (labels == null) return null;
    for (ImageLabel candidate : labels) {
      if (candidate == null || candidate.getConfidence() < 0.55f) continue;
      if (best == null || candidate.getConfidence() > best.getConfidence()) best = candidate;
    }
    return best;
  }

  private void handleSquat(Bitmap bitmap) {
    if (poseDetector == null) {
      setStatus("O reconhecimento de pose não está disponível neste aparelho.");
      return;
    }
    poseDetector
        .process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener(
            pose -> {
              Float angle = kneeAngle(pose);
              if (angle == null) {
                setStatus(
                    "A pose do corpo inteiro não foi detectada. Afaste-se e tente novamente.");
                return;
              }
              MissionLogic.SquatState updated = MissionLogic.updateSquat(squatState, angle);
              if (updated == squatState
                  || (updated.phase == squatState.phase
                      && updated.repetitions == squatState.repetitions)) {
                if (squatState.phase == MissionLogic.SquatPhase.UP) {
                  setStatus(
                      String.format(
                          Locale.ROOT,
                          "Ângulo %.0f°. Agache mais para iniciar a repetição.",
                          angle));
                } else {
                  setStatus(
                      String.format(
                          Locale.ROOT, "Ângulo %.0f°. Fique totalmente em pé para contar.", angle));
                }
              }
              squatState = updated;
              progress = Math.min(requiredCount, updated.repetitions);
              updateProgress();
              if (MissionLogic.isComplete(progress, requiredCount)) {
                completeMission(null);
              } else {
                setStatus(
                    squatState.phase == MissionLogic.SquatPhase.UP
                        ? "Fique em pé e capture um agachamento profundo."
                        : "Agora fique totalmente em pé e capture novamente.");
              }
            })
        .addOnFailureListener(
            error -> setStatus("O reconhecimento de pose falhou. Tente novamente."));
  }

  private Float kneeAngle(Pose pose) {
    if (pose == null) return null;
    Float left =
        angleForSide(pose, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE);
    Float right =
        angleForSide(
            pose, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE);
    if (left == null) return right;
    if (right == null) return left;
    return (left + right) / 2f;
  }

  private Float angleForSide(Pose pose, int hipType, int kneeType, int ankleType) {
    PoseLandmark hip = pose.getPoseLandmark(hipType);
    PoseLandmark knee = pose.getPoseLandmark(kneeType);
    PoseLandmark ankle = pose.getPoseLandmark(ankleType);
    if (hip == null || knee == null || ankle == null) return null;
    PointF h = hip.getPosition();
    PointF k = knee.getPosition();
    PointF a = ankle.getPosition();
    double ux = h.x - k.x;
    double uy = h.y - k.y;
    double vx = a.x - k.x;
    double vy = a.y - k.y;
    double denominator = Math.sqrt(ux * ux + uy * uy) * Math.sqrt(vx * vx + vy * vy);
    if (denominator <= 0.0001d) return null;
    double cosine = (ux * vx + uy * vy) / denominator;
    cosine = Math.max(-1d, Math.min(1d, cosine));
    return (float) Math.toDegrees(Math.acos(cosine));
  }

  private void startSteps() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this,
          new String[] {Manifest.permission.ACTIVITY_RECOGNITION},
          REQUEST_ACTIVITY_RECOGNITION);
      return;
    }
    registerSensor(Sensor.TYPE_STEP_DETECTOR);
  }

  private void startShake() {
    registerSensor(Sensor.TYPE_ACCELEROMETER);
  }

  private void registerSensor(int sensorType) {
    if (sensorManager == null) sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    Sensor sensor = sensorManager == null ? null : sensorManager.getDefaultSensor(sensorType);
    if (sensor == null) {
      setStatus("Este aparelho não oferece o sensor necessário.");
      return;
    }
    sensorWanted = true;
    sensorActive = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
    if (!sensorActive) setStatus("O sensor não pôde ser iniciado.");
    else setStatus("Sensor ativo. Continue até o contador atingir o alvo.");
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event == null || event.sensor == null || finishing) return;
    if ("steps".equals(type)
        && event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR
        && event.values.length > 0
        && event.values[0] > 0f) {
      acceptOneProgress();
    } else if ("shake".equals(type)
        && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER
        && event.values.length >= 3) {
      long now = android.os.SystemClock.elapsedRealtime();
      if (MissionLogic.shakeDetected(
          event.values[0], event.values[1], event.values[2], now, lastShake)) {
        lastShake = now;
        acceptOneProgress();
      }
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    // No threshold decision is made from sensor accuracy alone.
  }

  private void startRhythm() {
    if (!SpeechRecognizer.isRecognitionAvailable(this)) {
      setStatus("O reconhecimento de voz não está disponível neste aparelho.");
      return;
    }
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this, new String[] {Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
      return;
    }
    if (speechRecognizer == null) {
      speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
      speechRecognizer.setRecognitionListener(
          new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
              if (rhythmStartAt < 0L) rhythmStartAt = android.os.SystemClock.elapsedRealtime();
              startRhythmPulse();
              setStatus("Ouvindo no ritmo de " + rhythmBpm() + " BPM…");
            }

            @Override
            public void onBeginningOfSpeech() {
              currentSpeechStart = android.os.SystemClock.elapsedRealtime();
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
              scheduleListeningRestart();
            }

            @Override
            public void onError(int error) {
              scheduleListeningRestart();
            }

            @Override
            public void onResults(Bundle results) {
              handleSpeechResults(results);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
          });
    }
    if (rhythmStartAt < 0L) rhythmStartAt = android.os.SystemClock.elapsedRealtime();
    startRhythmPulse();
    Intent recognize = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    recognize.putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    recognize.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
    recognize.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
    recognize.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
    try {
      speechRecognizer.startListening(recognize);
    } catch (RuntimeException error) {
      setStatus("O microfone não pôde ser iniciado.");
    }
  }

  private void handleSpeechResults(Bundle results) {
    if (results == null || finishing) return;
    ArrayList<String> heard = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    if (heard == null) {
      scheduleListeningRestart();
      return;
    }
    long now = android.os.SystemClock.elapsedRealtime();
    long spokenAt = currentSpeechStart >= 0L ? currentSpeechStart : now;
    currentSpeechStart = -1L;
    for (String phrase : heard) {
      if (!MissionLogic.spokenWordMatches(rhythmWord(), phrase)) continue;
      if (!isOnRhythmBeat(spokenAt)) {
        setStatus("A palavra foi ouvida, mas fora do ritmo solicitado.");
        break;
      }
      if (lastSpeechMatch < 0L || spokenAt - lastSpeechMatch >= 350L) {
        lastSpeechMatch = spokenAt;
        acceptOneProgress();
      }
      break;
    }
    scheduleListeningRestart();
  }

  private boolean isOnRhythmBeat(long now) {
    int bpm = rhythmBpm();
    if (bpm <= 0 || rhythmStartAt < 0L) return false;
    long period = 60000L / bpm;
    long tolerance = Math.max(350L, Math.round(period * 0.35d));
    long elapsed = Math.max(0L, now - rhythmStartAt);
    long remainder = elapsed % period;
    long distance = Math.min(remainder, period - remainder);
    return distance <= tolerance;
  }

  private void startRhythmPulse() {
    if (rhythmBeatView == null || rhythmPulse != null) return;
    rhythmPulse =
        new Runnable() {
          @Override
          public void run() {
            if (finishing || rhythmBeatView == null) return;
            long period = Math.max(250L, 60000L / Math.max(1, rhythmBpm()));
            long elapsed =
                rhythmStartAt < 0L ? 0L : android.os.SystemClock.elapsedRealtime() - rhythmStartAt;
            boolean beat = ((elapsed / period) % 2L) == 0L;
            rhythmBeatView.setText(beat ? "●  BATA" : "○  aguarde");
            rhythmBeatView.setTextColor(beat ? CYAN : MUTED);
            mainHandler.postDelayed(this, Math.max(80L, period / 2L));
          }
        };
    mainHandler.post(rhythmPulse);
  }

  private void scheduleListeningRestart() {
    if (finishing || speechRecognizer == null) return;
    mainHandler.postDelayed(
        () -> {
          if (!finishing && speechRecognizer != null) startRhythm();
        },
        350L);
  }

  private String rhythmWord() {
    String raw = target == null || target.trim().isEmpty() ? "wake" : target.trim();
    int at = raw.indexOf('@');
    return at >= 0 ? raw.substring(0, at).trim() : raw;
  }

  private int rhythmBpm() {
    if (target == null) return 60;
    int at = target.indexOf('@');
    if (at < 0) return 60;
    String suffix = target.substring(at + 1).trim().toLowerCase(Locale.ROOT);
    if (suffix.startsWith("bpm=")) suffix = suffix.substring(4);
    return MissionLogic.parsePositiveInt(suffix, 60);
  }

  private void acceptOneProgress() {
    if (finishing) return;
    progress = MissionLogic.incrementBounded(progress, requiredCount);
    updateProgress();
    if (MissionLogic.isComplete(progress, requiredCount)) completeMission(null);
    else setStatus("Progresso registrado. Continue.");
  }

  private void updateProgress() {
    if (progressView == null) return;
    progressView.setText(progress + " / " + requiredCount);
  }

  private void completeMission(String registeredTarget) {
    if (finishing) return;
    finishing = true;
    stopResources();
    Intent result = new Intent();
    result.putExtra(EXTRA_TYPE, type);
    result.putExtra(EXTRA_COUNT, progress);
    if (registeredTarget != null) {
      result.putExtra(EXTRA_TARGET, registeredTarget);
      result.putExtra(RESULT_TARGET, registeredTarget);
    }
    // Deliver accepted answers directly to the active service instead of relying solely
    // on the nested Activity result chain. The expected cursor rejects stale callbacks;
    // the result marker prevents RingActivity from reporting the same answer twice.
    if (!isRegisterMode() && alarmId > 0) {
      AlarmService.missionResult(this, alarmId, missionIndex, true);
      result.putExtra(RESULT_SERVICE_REPORTED, true);
    }
    setResult(RESULT_OK, result);
    finish();
  }

  private void cancelMission() {
    if (finishing) return;
    stopResources();
    setResult(RESULT_CANCELED);
    finish();
  }

  private void setStatus(String status) {
    lastStatus = status == null ? "" : status;
    if (statusView != null) statusView.setText(lastStatus);
  }

  private File savePrivatePhoto(Bitmap bitmap) {
    File directory = new File(getFilesDir(), "mission_photos");
    if (!directory.exists() && !directory.mkdirs()) return null;
    File file;
    try {
      file = File.createTempFile("registered_", ".jpg", directory);
    } catch (IOException error) {
      return null;
    }
    try (FileOutputStream output = new FileOutputStream(file)) {
      if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
        //noinspection ResultOfMethodCallIgnored
        file.delete();
        return null;
      }
      return file;
    } catch (IOException error) {
      //noinspection ResultOfMethodCallIgnored
      file.delete();
      return null;
    }
  }

  private int[] bitmapHash(Bitmap bitmap) {
    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, HASH_SIZE, HASH_SIZE, true);
    int[] grayscale = new int[HASH_SIZE * HASH_SIZE];
    for (int y = 0; y < HASH_SIZE; y++) {
      for (int x = 0; x < HASH_SIZE; x++) {
        int pixel = scaled.getPixel(x, y);
        grayscale[y * HASH_SIZE + x] =
            (int)
                (0.299f * Color.red(pixel)
                    + 0.587f * Color.green(pixel)
                    + 0.114f * Color.blue(pixel));
      }
    }
    if (scaled != bitmap) scaled.recycle();
    return MissionLogic.averageHash(grayscale);
  }

  private boolean hasMeaningfulPhotoDetail(Bitmap bitmap) {
    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, HASH_SIZE, HASH_SIZE, true);
    int[] grayscale = new int[HASH_SIZE * HASH_SIZE];
    for (int y = 0; y < HASH_SIZE; y++) {
      for (int x = 0; x < HASH_SIZE; x++) {
        int pixel = scaled.getPixel(x, y);
        grayscale[y * HASH_SIZE + x] =
            (int)
                (0.299f * Color.red(pixel)
                    + 0.587f * Color.green(pixel)
                    + 0.114f * Color.blue(pixel));
      }
    }
    if (scaled != bitmap) scaled.recycle();
    return MissionLogic.hasMeaningfulVariance(grayscale);
  }

  private void stopResources() {
    if (sensorManager != null) {
      sensorManager.unregisterListener(this);
      sensorActive = false;
    }
    if (speechRecognizer != null) {
      try {
        speechRecognizer.stopListening();
        speechRecognizer.cancel();
        speechRecognizer.destroy();
      } catch (RuntimeException ignored) {
        // The recognizer may already have been torn down by the platform.
      }
      speechRecognizer = null;
    }
    mainHandler.removeCallbacksAndMessages(null);
    rhythmPulse = null;
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (sensorManager != null) sensorManager.unregisterListener(this);
    sensorActive = false;
    if (speechRecognizer != null) {
      try {
        speechRecognizer.cancel();
      } catch (RuntimeException ignored) {
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!finishing && sensorWanted && !sensorActive) {
      if ("steps".equals(type)) startSteps();
      else if ("shake".equals(type)) registerSensor(Sensor.TYPE_ACCELEROMETER);
    }
  }

  @Override
  protected void onDestroy() {
    stopResources();
    if (imageLabeler != null) imageLabeler.close();
    if (poseDetector != null) poseDetector.close();
    super.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putString("feedback", lastStatus);
    outState.putString(EXTRA_TARGET, target);
    outState.putStringArrayList(EXTRA_TARGETS, new ArrayList<>(targets));
    outState.putInt("alarm_id", alarmId);
    outState.putInt("mission_index", missionIndex);
    outState.putInt("progress", progress);
    outState.putInt("mathA", mathA);
    outState.putInt("mathB", mathB);
    outState.putChar("mathOperator", mathOperator);
    outState.putInt("mathExpected", mathExpected);
    outState.putBoolean("mathInitialized", mathInitialized);
    outState.putInt("expectedColor", expectedColor);
    outState.putString("expectedColorName", expectedColorName);
    outState.putInt("colorRound", colorRound);
    outState.putInt("colorSeed", colorSeed);
    outState.putLong("lastShake", lastShake);
    outState.putInt("squatPhase", squatState.phase == MissionLogic.SquatPhase.DOWN ? 1 : 0);
    outState.putInt("squatRepetitions", squatState.repetitions);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    boolean granted =
        grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    if (!granted) {
      setStatus(permissionMessages.get(requestCode) + " A missão continua pendente.");
      return;
    }
    if (requestCode == REQUEST_CAMERA_PERMISSION) launchCamera(pendingCameraPurpose);
    else if (requestCode == REQUEST_AUDIO_PERMISSION) startRhythm();
    else if (requestCode == REQUEST_ACTIVITY_RECOGNITION) startSteps();
  }

  @Override
  @SuppressWarnings("deprecation")
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    IntentResult barcode = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
    if (barcode != null) {
      scannerPending = false;
      handleBarcodeResult(barcode.getContents());
      return;
    }
    if (requestCode == REQUEST_CAMERA) {
      cameraPending = false;
      if (resultCode != RESULT_OK || data == null) {
        setStatus("A captura foi cancelada. A missão continua pendente.");
      } else {
        Bitmap bitmap = extractBitmap(data);
        handleCapturedBitmap(bitmap);
      }
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @SuppressWarnings("deprecation")
  private Bitmap extractBitmap(Intent data) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return data.getParcelableExtra("data", Bitmap.class);
    }
    return (Bitmap) data.getParcelableExtra("data");
  }

  @Override
  public void onBackPressed() {
    cancelMission();
  }

  private static String firstNonEmpty(String value, String fallback) {
    return value == null || value.trim().isEmpty() ? fallback : value;
  }

  private void readTargets(ArrayList<String> incoming) {
    if (incoming == null) return;
    for (String value : incoming) {
      if (value != null && !value.isEmpty() && !targets.contains(value)) targets.add(value);
    }
  }

  private boolean matchesAnyBarcode(String contents) {
    if (contents == null) return false;
    if (!targets.isEmpty()) {
      for (String accepted : targets) {
        if (MissionLogic.barcodeMatches(accepted, contents)) return true;
      }
      return false;
    }
    return MissionLogic.barcodeMatches(target, contents);
  }

  private TextView label(String text, float size, int color) {
    TextView view = new TextView(this);
    view.setText(text);
    view.setTextSize(size);
    view.setTextColor(color);
    return view;
  }

  private android.graphics.drawable.GradientDrawable round(int color, float radiusDp) {
    android.graphics.drawable.GradientDrawable drawable =
        new android.graphics.drawable.GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(dpF(radiusDp));
    return drawable;
  }

  private LinearLayout.LayoutParams marginParams(
      int width, int height, int left, int top, int right, int bottom) {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
    params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
    return params;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private float dpF(float value) {
    return value * getResources().getDisplayMetrics().density;
  }

  private static final class MathQuestion {
    final int a;
    final int b;
    final char operator;

    MathQuestion(int a, int b, char operator) {
      this.a = a;
      this.b = b;
      this.operator = operator;
    }
  }

  private MathQuestion parseMathTarget(String candidate) {
    if (candidate == null) return null;
    String compact = candidate.replaceAll("\\s+", "");
    if (!compact.matches("\\d+[+-]\\d+")) return null;
    int plus = compact.indexOf('+');
    int minus = compact.indexOf('-', 1);
    int operatorIndex = plus >= 0 ? plus : minus;
    if (operatorIndex <= 0) return null;
    try {
      int a = Integer.parseInt(compact.substring(0, operatorIndex));
      int b = Integer.parseInt(compact.substring(operatorIndex + 1));
      return new MathQuestion(a, b, compact.charAt(operatorIndex));
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
