package com.desperta;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Deterministic rules used by the wake-up missions.
 *
 * <p>This class deliberately has no Android dependencies. That makes the parts which decide whether
 * a user actually completed a mission easy to exercise with local unit tests and keeps the Activity
 * from silently accepting a button press as proof of completion.
 */
public final class MissionLogic {
  private MissionLogic() {}

  public static final int DEFAULT_COUNT = 1;
  public static final float SHAKE_G_FORCE = 2.7f;
  public static final long SHAKE_DEBOUNCE_MILLIS = 650L;
  public static final float SQUAT_DOWN_ANGLE = 105f;
  public static final float SQUAT_UP_ANGLE = 160f;

  public static String canonicalType(String value) {
    if (value == null) return "typing";
    String type = value.trim().toLowerCase(Locale.ROOT);
    switch (type) {
      case "barcode":
      case "qr":
      case "qr/barcode":
        return "barcode";
      case "math":
        return "math";
      case "typing":
        return "typing";
      case "colors":
      case "color":
      case "find color tiles":
        return "colors";
      case "shake":
        return "shake";
      case "step":
      case "steps":
        return "steps";
      case "photo":
        return "photo";
      case "squat":
        return "squat";
      case "object":
      case "household item hunt":
        return "object";
      case "rhythm":
      case "say the word on beat":
        return "rhythm";
      default:
        return type.isEmpty() ? "typing" : type;
    }
  }

  public static int safeCount(int count) {
    return Math.max(1, Math.min(100, count <= 0 ? DEFAULT_COUNT : count));
  }

  public static int incrementBounded(int current, int required) {
    int limit = safeCount(required);
    return Math.max(0, Math.min(limit, current + 1));
  }

  public static boolean isComplete(int current, int required) {
    return current >= safeCount(required);
  }

  /** Barcode values are intentionally exact; a different payload must not dismiss an alarm. */
  public static boolean barcodeMatches(String expected, String scanned) {
    if (expected == null || scanned == null) return false;
    return !expected.isEmpty() && expected.equals(scanned);
  }

  /** Typing missions do not trim input: an accidental missing or extra character must fail. */
  public static boolean typingMatches(String expected, String entered) {
    return expected != null && entered != null && !expected.isEmpty() && expected.equals(entered);
  }

  public static String normalizeSpokenText(String value) {
    if (value == null) return "";
    String withoutAccents =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    return withoutAccents
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^\\p{L}\\p{N}]+", " ")
        .trim()
        .replaceAll("\\s+", " ");
  }

  public static boolean spokenWordMatches(String expected, String recognized) {
    String wanted = normalizeSpokenText(expected);
    String heard = normalizeSpokenText(recognized);
    if (wanted.isEmpty() || heard.isEmpty()) return false;
    if (wanted.equals(heard)) return true;
    // Speech recognition often returns a short sentence around the requested word. Require
    // the whole expected phrase to occur as a token sequence, rather than accepting a loose
    // substring such as "cat" inside "cattle".
    return (" " + heard + " ").contains(" " + wanted + " ");
  }

  public static boolean labelMatches(String expected, String actual) {
    String wanted = normalizeSpokenText(expected);
    String observed = normalizeSpokenText(actual);
    if (wanted.isEmpty() || observed.isEmpty()) return false;
    if (wanted.equals(observed)) return true;
    String[] tokens = wanted.split(" ");
    for (String token : tokens) {
      if (!(" " + observed + " ").contains(" " + token + " ")) return false;
    }
    return true;
  }

  public static boolean mathAnswerMatches(int expected, String entered) {
    if (entered == null) return false;
    try {
      return Integer.parseInt(entered.trim()) == expected;
    } catch (NumberFormatException ignored) {
      return false;
    }
  }

  public static boolean colorMatches(int expectedRgb, int actualRgb, int tolerance) {
    int safeTolerance = Math.max(0, tolerance);
    int dr = ((expectedRgb >> 16) & 0xff) - ((actualRgb >> 16) & 0xff);
    int dg = ((expectedRgb >> 8) & 0xff) - ((actualRgb >> 8) & 0xff);
    int db = (expectedRgb & 0xff) - (actualRgb & 0xff);
    return Math.sqrt(dr * dr + dg * dg + db * db) <= safeTolerance;
  }

  /**
   * Returns an average-hash signature. Input is a row-major grayscale image, normally resized to
   * 8x8 by the Activity. The hash is resistant to a moderate lighting change.
   */
  public static int[] averageHash(int[] grayscale) {
    if (grayscale == null || grayscale.length == 0) return new int[0];
    long sum = 0;
    for (int value : grayscale) sum += Math.max(0, Math.min(255, value));
    double mean = sum / (double) grayscale.length;
    int[] result = new int[grayscale.length];
    for (int i = 0; i < grayscale.length; i++) {
      result[i] = grayscale[i] >= mean ? 1 : 0;
    }
    return result;
  }

  /** Rejects a fully blank/constant image before it can become a reusable photo target. */
  public static boolean hasMeaningfulVariance(int[] grayscale) {
    if (grayscale == null || grayscale.length < 2) return false;
    double mean = 0d;
    for (int value : grayscale) mean += value;
    mean /= grayscale.length;
    double variance = 0d;
    for (int value : grayscale) {
      double delta = value - mean;
      variance += delta * delta;
    }
    variance /= grayscale.length;
    return variance >= 4d;
  }

  public static double signatureSimilarity(int[] first, int[] second) {
    if (first == null || second == null || first.length == 0 || first.length != second.length) {
      return 0d;
    }
    int equal = 0;
    for (int i = 0; i < first.length; i++) {
      if (first[i] == second[i]) equal++;
    }
    return equal / (double) first.length;
  }

  /** Uses a conservative threshold so two unrelated photos do not pass accidentally. */
  public static boolean photoMatches(int[] registeredHash, int[] capturedHash) {
    return signatureSimilarity(registeredHash, capturedHash) >= 0.78d;
  }

  public static boolean shakeDetected(float x, float y, float z, long now, long lastShake) {
    float gForce = (float) (Math.sqrt(x * x + y * y + z * z) / 9.80665d);
    return gForce >= SHAKE_G_FORCE && (lastShake < 0L || now - lastShake >= SHAKE_DEBOUNCE_MILLIS);
  }

  public enum SquatPhase {
    UP,
    DOWN
  }

  public static final class SquatState {
    public final SquatPhase phase;
    public final int repetitions;

    public SquatState(SquatPhase phase, int repetitions) {
      this.phase = phase == null ? SquatPhase.UP : phase;
      this.repetitions = Math.max(0, repetitions);
    }
  }

  /** Counts only a complete down -> up cycle. A single frame or two down frames can never pass. */
  public static SquatState updateSquat(SquatState state, float kneeAngle) {
    SquatState current = state == null ? new SquatState(SquatPhase.UP, 0) : state;
    if (Float.isNaN(kneeAngle) || Float.isInfinite(kneeAngle)) return current;
    if (current.phase == SquatPhase.UP && kneeAngle <= SQUAT_DOWN_ANGLE) {
      return new SquatState(SquatPhase.DOWN, current.repetitions);
    }
    if (current.phase == SquatPhase.DOWN && kneeAngle >= SQUAT_UP_ANGLE) {
      return new SquatState(SquatPhase.UP, current.repetitions + 1);
    }
    return current;
  }

  public static int parsePositiveInt(String value, int fallback) {
    if (value == null) return fallback;
    try {
      int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : fallback;
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }
}
