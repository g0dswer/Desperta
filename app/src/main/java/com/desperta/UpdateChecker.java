package com.desperta;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

/** Public GitHub release checker and signed APK verifier for Desperta. */
public final class UpdateChecker {
  public static final String REPOSITORY = "g0dswer/Desperta";
  public static final String RELEASES_ENDPOINT =
      "https://api.github.com/repos/" + REPOSITORY + "/releases?per_page=30";
  public static final String PREFS = Weather.PREFS;
  public static final String KEY_AUTO_CHECK = "updates_auto_check";
  public static final String KEY_INCLUDE_PREVIEWS = "updates_include_previews";
  public static final String KEY_LAST_CHECK = "updates_last_check";
  public static final String KEY_LAST_STATUS = "updates_last_status";
  public static final String KEY_LAST_VERSION = "updates_last_version";
  public static final String KEY_LAST_NAME = "updates_last_name";
  public static final String KEY_LAST_RELEASE_URL = "updates_last_release_url";
  public static final String KEY_LAST_DOWNLOAD_URL = "updates_last_download_url";
  public static final String KEY_LAST_ASSET_NAME = "updates_last_asset_name";
  public static final String KEY_LAST_CHECKSUM_URL = "updates_last_checksum_url";
  public static final String KEY_LAST_CHECKSUM = "updates_last_checksum";
  public static final String KEY_LAST_PRERELEASE = "updates_last_prerelease";

  private static final String STATUS_AVAILABLE = "available";
  private static final String STATUS_CURRENT = "current";
  private static final String STATUS_ERROR = "error";
  private static final long DAY_MS = 24L * 60L * 60L * 1000L;
  private static final int CONNECT_TIMEOUT_MS = 12_000;
  private static final int READ_TIMEOUT_MS = 20_000;
  private static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;
  private static final long MAX_APK_BYTES = 250L * 1024L * 1024L;
  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
  private static final AtomicBoolean CHECK_RUNNING = new AtomicBoolean(false);
  private static final AtomicBoolean DOWNLOAD_RUNNING = new AtomicBoolean(false);

  private UpdateChecker() {}

  public interface Callback {
    void onComplete(Result result);
  }

  public interface ProgressListener {
    void onProgress(long downloaded, long total);
  }

  public interface DownloadCallback {
    void onComplete(File apk);
  }

  public interface ErrorCallback {
    void onError(Exception error);
  }

  public static final class Result {
    public enum Status {
      UPDATE_AVAILABLE,
      UP_TO_DATE,
      ERROR
    }

    public final Status status;
    public final ReleaseInfo release;
    public final String installedVersion;
    public final String message;

    private Result(Status status, ReleaseInfo release, String installedVersion, String message) {
      this.status = status;
      this.release = release;
      this.installedVersion = installedVersion == null ? "0.0.0" : installedVersion;
      this.message = message == null ? "" : message;
    }

    static Result available(String installedVersion, ReleaseInfo release) {
      return new Result(Status.UPDATE_AVAILABLE, release, installedVersion, "");
    }

    static Result current(String installedVersion) {
      return new Result(
          Status.UP_TO_DATE, null, installedVersion, "Você já está na versão mais recente.");
    }

    static Result error(String installedVersion, String message) {
      return new Result(Status.ERROR, null, installedVersion, message);
    }
  }

  public static final class ReleaseInfo {
    public final String tagName;
    public final String version;
    public final String name;
    public final String releaseUrl;
    public final String downloadUrl;
    public final String assetName;
    public final String checksumUrl;
    public final String checksum;
    public final String publishedAt;
    public final boolean prerelease;

    ReleaseInfo(
        String tagName,
        String version,
        String name,
        String releaseUrl,
        String downloadUrl,
        String assetName,
        String checksumUrl,
        String checksum,
        String publishedAt,
        boolean prerelease) {
      this.tagName = tagName;
      this.version = version;
      this.name = name;
      this.releaseUrl = releaseUrl;
      this.downloadUrl = downloadUrl;
      this.assetName = assetName;
      this.checksumUrl = checksumUrl;
      this.checksum = checksum;
      this.publishedAt = publishedAt;
      this.prerelease = prerelease;
    }
  }

  /** Semantic version with a small, deterministic prerelease ordering. */
  public static final class Version implements Comparable<Version> {
    public final int major;
    public final int minor;
    public final int patch;
    public final List<String> prerelease;

    private Version(int major, int minor, int patch, List<String> prerelease) {
      this.major = major;
      this.minor = minor;
      this.patch = patch;
      this.prerelease = Collections.unmodifiableList(new ArrayList<>(prerelease));
    }

    public static Version parse(String raw) {
      String value = normalize(raw);
      String[] mainAndPre = value.split("-", 2);
      String[] numbers = mainAndPre[0].split("\\.");
      int major = number(numbers, 0);
      int minor = number(numbers, 1);
      int patch = number(numbers, 2);
      List<String> pre = new ArrayList<>();
      if (mainAndPre.length == 2 && !mainAndPre[1].isEmpty()) {
        for (String item : mainAndPre[1].split("\\.")) {
          if (!item.isEmpty()) pre.add(item.toLowerCase(Locale.ROOT));
        }
      }
      return new Version(major, minor, patch, pre);
    }

    private static int number(String[] values, int index) {
      if (index >= values.length) return 0;
      try {
        return Math.max(0, Integer.parseInt(values[index].replaceAll("[^0-9].*", "")));
      } catch (NumberFormatException ignored) {
        return 0;
      }
    }

    @Override
    public int compareTo(Version other) {
      int result = Integer.compare(major, other.major);
      if (result != 0) return result;
      result = Integer.compare(minor, other.minor);
      if (result != 0) return result;
      result = Integer.compare(patch, other.patch);
      if (result != 0) return result;
      if (prerelease.isEmpty() && other.prerelease.isEmpty()) return 0;
      if (prerelease.isEmpty()) return 1;
      if (other.prerelease.isEmpty()) return -1;
      int count = Math.min(prerelease.size(), other.prerelease.size());
      for (int i = 0; i < count; i++) {
        String left = prerelease.get(i);
        String right = other.prerelease.get(i);
        boolean leftNumber = left.matches("[0-9]+");
        boolean rightNumber = right.matches("[0-9]+");
        if (leftNumber && rightNumber) {
          result = Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        } else if (leftNumber != rightNumber) {
          result = leftNumber ? -1 : 1;
        } else {
          result = left.compareTo(right);
        }
        if (result != 0) return result;
      }
      return Integer.compare(prerelease.size(), other.prerelease.size());
    }

    @Override
    public String toString() {
      String base = major + "." + minor + "." + patch;
      return prerelease.isEmpty() ? base : base + "-" + String.join(".", prerelease);
    }
  }

  /** Performs a network check off the main thread and delivers its result on the main thread. */
  public static boolean check(Context context, boolean includePreviews, Callback callback) {
    if (!CHECK_RUNNING.compareAndSet(false, true)) return false;
    if (context == null) {
      CHECK_RUNNING.set(false);
      if (callback != null) callback.onComplete(Result.error("0.0.0", "Contexto indisponível."));
      return true;
    }
    final Context app = context.getApplicationContext();
    final String installed = getInstalledVersionName(app);
    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
        .apply();
    EXECUTOR.execute(
        () -> {
          try {
            Result result;
            try {
              String payload = request(RELEASES_ENDPOINT, "application/vnd.github+json");
              result = parseReleaseList(payload, installed, includePreviews);
            } catch (Exception error) {
              result = Result.error(installed, userMessage(error));
            }
            final Result completed = result;
            saveCachedResult(app, completed);
            if (callback != null) {
              new android.os.Handler(android.os.Looper.getMainLooper())
                  .post(() -> callback.onComplete(completed));
            }
          } finally {
            CHECK_RUNNING.set(false);
          }
        });
    return true;
  }

  /** Starts at most one silent daily check when the application process opens. */
  public static void maybeCheckDaily(Context context) {
    if (context == null) return;
    Context app = context.getApplicationContext();
    android.content.SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    if (!prefs.getBoolean(KEY_AUTO_CHECK, true)) return;
    long last = prefs.getLong(KEY_LAST_CHECK, 0L);
    if (last > 0L && System.currentTimeMillis() - last < DAY_MS) return;
    check(app, prefs.getBoolean(KEY_INCLUDE_PREVIEWS, true), null);
  }

  /** Downloads and verifies a release off the main thread. */
  public static void downloadAsync(
      Context context,
      ReleaseInfo release,
      ProgressListener progress,
      DownloadCallback success,
      ErrorCallback failure) {
    if (context == null || release == null) {
      if (failure != null) failure.onError(new IOException("Download inválido."));
      return;
    }
    if (!DOWNLOAD_RUNNING.compareAndSet(false, true)) {
      if (failure != null) failure.onError(new IOException("Outro download já está em andamento."));
      return;
    }
    Context app = context.getApplicationContext();
    android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    ProgressListener mainProgress =
        progress == null
            ? null
            : (downloaded, total) -> main.post(() -> progress.onProgress(downloaded, total));
    EXECUTOR.execute(
        () -> {
          try {
            File file = downloadAndVerify(app, release, mainProgress);
            if (success != null) {
              main.post(() -> success.onComplete(file));
            }
          } catch (Exception error) {
            if (failure != null) {
              main.post(() -> failure.onError(error));
            }
          } finally {
            DOWNLOAD_RUNNING.set(false);
          }
        });
  }

  public static boolean autoCheckEnabled(Context context) {
    return context != null
        && context
            .getApplicationContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_CHECK, true);
  }

  public static void setAutoCheckEnabled(Context context, boolean enabled) {
    if (context == null) return;
    context
        .getApplicationContext()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_AUTO_CHECK, enabled)
        .apply();
  }

  public static boolean includePreviews(Context context) {
    return context == null
        || context
            .getApplicationContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_INCLUDE_PREVIEWS, true);
  }

  public static void setIncludePreviews(Context context, boolean enabled) {
    if (context == null) return;
    context
        .getApplicationContext()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_INCLUDE_PREVIEWS, enabled)
        .apply();
  }

  public static Result cachedResult(Context context) {
    if (context == null) return Result.error("0.0.0", "Nenhuma consulta foi feita ainda.");
    android.content.SharedPreferences prefs =
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    String status = prefs.getString(KEY_LAST_STATUS, "");
    String installed = getInstalledVersionName(context);
    if (STATUS_AVAILABLE.equals(status)) {
      String version = prefs.getString(KEY_LAST_VERSION, "");
      if (!version.isEmpty() && Version.parse(version).compareTo(Version.parse(installed)) > 0) {
        ReleaseInfo release =
            new ReleaseInfo(
                version,
                version,
                prefs.getString(KEY_LAST_NAME, version),
                prefs.getString(KEY_LAST_RELEASE_URL, ""),
                prefs.getString(KEY_LAST_DOWNLOAD_URL, ""),
                prefs.getString(KEY_LAST_ASSET_NAME, ""),
                prefs.getString(KEY_LAST_CHECKSUM_URL, ""),
                prefs.getString(KEY_LAST_CHECKSUM, ""),
                "",
                prefs.getBoolean(KEY_LAST_PRERELEASE, false));
        return Result.available(installed, release);
      }
    }
    if (STATUS_ERROR.equals(status)) {
      return Result.error(
          installed, prefs.getString("updates_last_message", "Verificação indisponível."));
    }
    return Result.current(installed);
  }

  public static String summary(Context context) {
    Result result = cachedResult(context);
    if (result.status == Result.Status.UPDATE_AVAILABLE)
      return "Nova versão " + result.release.version;
    if (result.status == Result.Status.ERROR) return "Verificação pendente";
    return "v" + result.installedVersion;
  }

  /** Selects the newest published release with a downloadable APK from GitHub's releases list. */
  static Result parseReleaseList(String json, String installedVersion, boolean includePreviews) {
    String installed = normalize(installedVersion);
    List<ReleaseInfo> candidates = new ArrayList<>();
    try {
      JSONArray releases = new JSONArray(json == null ? "[]" : json);
      for (int i = 0; i < releases.length(); i++) {
        JSONObject item = releases.optJSONObject(i);
        if (item == null || item.optBoolean("draft", false)) continue;
        boolean prerelease = item.optBoolean("prerelease", false);
        if (prerelease && !includePreviews) continue;
        String tag = item.optString("tag_name", "").trim();
        if (tag.isEmpty()) continue;
        String version = normalize(tag);
        if (Version.parse(version).compareTo(Version.parse(installed)) <= 0) continue;
        JSONArray assets = item.optJSONArray("assets");
        if (assets == null) continue;
        JSONObject apk = null;
        JSONObject sums = null;
        for (int j = 0; j < assets.length(); j++) {
          JSONObject asset = assets.optJSONObject(j);
          if (asset == null) continue;
          String name = asset.optString("name", "");
          if (name.toLowerCase(Locale.ROOT).endsWith(".apk") && apk == null) apk = asset;
          if (isChecksumAsset(name) && sums == null) sums = asset;
        }
        if (apk == null) continue;
        String download = apk.optString("browser_download_url", "");
        if (!isHttpsGithubUrl(download)) continue;
        String checksumUrl = sums == null ? "" : sums.optString("browser_download_url", "");
        if (!checksumUrl.isEmpty() && !isHttpsGithubUrl(checksumUrl)) checksumUrl = "";
        String assetName = apk.optString("name", "Desperta-" + version + ".apk");
        String checksum = item.optString("sha256", "");
        candidates.add(
            new ReleaseInfo(
                tag,
                version,
                item.optString("name", tag),
                item.optString("html_url", ""),
                download,
                assetName,
                checksumUrl,
                checksum,
                item.optString("published_at", ""),
                prerelease));
      }
    } catch (Exception error) {
      return Result.error(installed, "Resposta inválida do GitHub.");
    }
    candidates.sort(
        (left, right) -> {
          int version = Version.parse(right.version).compareTo(Version.parse(left.version));
          if (version != 0) return version;
          return right.publishedAt.compareTo(left.publishedAt);
        });
    return candidates.isEmpty()
        ? Result.current(installed)
        : Result.available(installed, candidates.get(0));
  }

  /** Downloads, checks the optional SHA-256, package identity, version, and signing certificate. */
  public static File downloadAndVerify(
      Context context, ReleaseInfo release, ProgressListener listener) throws IOException {
    if (context == null || release == null || !isHttpsGithubUrl(release.downloadUrl)) {
      throw new IOException("Download inválido.");
    }
    File destination = new File(context.getCacheDir(), "desperta-update.apk");
    if (destination.exists() && !destination.delete())
      throw new IOException("Não foi possível substituir o download anterior.");
    String expected =
        release.checksum == null ? "" : release.checksum.trim().toLowerCase(Locale.ROOT);
    if (expected.isEmpty() && release.checksumUrl != null && !release.checksumUrl.isEmpty()) {
      String sums = request(release.checksumUrl, "text/plain");
      expected = findChecksum(sums, release.assetName);
    }
    MessageDigest digest = sha256();
    HttpURLConnection connection = null;
    try {
      connection = connection(release.downloadUrl, "application/octet-stream");
      int code = connection.getResponseCode();
      if (code < 200 || code >= 300)
        throw new IOException("GitHub não entregou o APK (" + code + ").");
      long total = connection.getContentLengthLong();
      if (total > MAX_APK_BYTES) throw new IOException("O APK excede o tamanho permitido.");
      try (InputStream input = new BufferedInputStream(connection.getInputStream());
          FileOutputStream output = new FileOutputStream(destination)) {
        byte[] buffer = new byte[64 * 1024];
        long count = 0L;
        int read;
        while ((read = input.read(buffer)) != -1) {
          count += read;
          if (count > MAX_APK_BYTES) throw new IOException("O APK excede o tamanho permitido.");
          output.write(buffer, 0, read);
          digest.update(buffer, 0, read);
          if (listener != null) listener.onProgress(count, total);
        }
      }
    } finally {
      if (connection != null) connection.disconnect();
    }
    String actual = hex(digest.digest());
    if (!expected.isEmpty() && !expected.equals(actual)) {
      destination.delete();
      throw new IOException("A verificação SHA-256 do APK falhou.");
    }
    verifyPackage(context, destination, release);
    return destination;
  }

  static String findChecksum(String sums, String assetName) {
    if (sums == null || assetName == null) return "";
    for (String line : sums.split("\\R")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
      String[] parts = trimmed.split("\\s+", 2);
      if (parts.length == 2 && parts[1].replaceFirst("^[*]", "").trim().equals(assetName)) {
        String hash = parts[0].trim().toLowerCase(Locale.ROOT);
        if (hash.matches("[0-9a-f]{64}")) return hash;
      }
    }
    return "";
  }

  private static void verifyPackage(Context context, File apk, ReleaseInfo release)
      throws IOException {
    PackageManager manager = context.getPackageManager();
    int flags =
        Build.VERSION.SDK_INT >= 28
            ? PackageManager.GET_SIGNING_CERTIFICATES
            : PackageManager.GET_SIGNATURES;
    PackageInfo archive = manager.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
    if (archive == null || !context.getPackageName().equals(archive.packageName)) {
      throw new IOException("O arquivo não é um APK Desperta.");
    }
    long installedCode = getInstalledVersionCode(context);
    long archiveCode =
        Build.VERSION.SDK_INT >= 28 ? archive.getLongVersionCode() : archive.versionCode;
    if (archiveCode <= installedCode)
      throw new IOException("O APK não é mais novo que o instalado.");
    if (!release.version.isEmpty() && !sameCoreVersion(archive.versionName, release.version)) {
      throw new IOException("A versão do APK não corresponde ao release informado.");
    }
    if (!sameSigningCertificate(context, archive)) {
      throw new IOException("A assinatura do APK não corresponde ao Desperta instalado.");
    }
  }

  private static boolean sameCoreVersion(String left, String right) {
    Version first = Version.parse(left);
    Version second = Version.parse(right);
    return first.major == second.major
        && first.minor == second.minor
        && first.patch == second.patch;
  }

  private static boolean sameSigningCertificate(Context context, PackageInfo archive)
      throws IOException {
    try {
      PackageManager manager = context.getPackageManager();
      PackageInfo installed =
          manager.getPackageInfo(
              context.getPackageName(),
              Build.VERSION.SDK_INT >= 28
                  ? PackageManager.GET_SIGNING_CERTIFICATES
                  : PackageManager.GET_SIGNATURES);
      Signature[] expected = signatures(installed);
      Signature[] actual = signatures(archive);
      if (expected.length == 0 || actual.length == 0) return false;
      Set<String> expectedDigests = new HashSet<>();
      for (Signature signature : expected)
        expectedDigests.add(hex(sha256().digest(signature.toByteArray())));
      Set<String> actualDigests = new HashSet<>();
      for (Signature signature : actual) {
        actualDigests.add(hex(sha256().digest(signature.toByteArray())));
      }
      return expectedDigests.equals(actualDigests);
    } catch (PackageManager.NameNotFoundException error) {
      throw new IOException("Não foi possível localizar o Desperta instalado.", error);
    } catch (RuntimeException error) {
      throw new IOException("Não foi possível verificar a assinatura do APK.", error);
    }
  }

  private static Signature[] signatures(PackageInfo info) {
    if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) {
      if (info.signingInfo.hasPastSigningCertificates())
        return info.signingInfo.getApkContentsSigners();
      return info.signingInfo.getApkContentsSigners();
    }
    return info.signatures == null ? new Signature[0] : info.signatures;
  }

  static String normalize(String raw) {
    if (raw == null) return "0.0.0";
    String value = raw.trim().toLowerCase(Locale.ROOT);
    while (value.startsWith("v")) value = value.substring(1);
    int plus = value.indexOf('+');
    if (plus >= 0) value = value.substring(0, plus);
    return value.isEmpty() ? "0.0.0" : value;
  }

  public static String getInstalledVersionName(Context context) {
    if (context == null) return "0.0.0";
    try {
      PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      return info.versionName == null || info.versionName.isEmpty() ? "0.0.0" : info.versionName;
    } catch (PackageManager.NameNotFoundException ignored) {
      return "0.0.0";
    }
  }

  static long getInstalledVersionCode(Context context) {
    if (context == null) return 0L;
    try {
      PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    } catch (PackageManager.NameNotFoundException ignored) {
      return 0L;
    }
  }

  private static void saveCachedResult(Context context, Result result) {
    if (context == null || result == null) return;
    android.content.SharedPreferences.Editor editor =
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .putString(
                KEY_LAST_STATUS,
                result.status == Result.Status.UPDATE_AVAILABLE
                    ? STATUS_AVAILABLE
                    : result.status == Result.Status.ERROR ? STATUS_ERROR : STATUS_CURRENT)
            .putString("updates_last_message", result.message);
    if (result.release != null) {
      editor
          .putString(KEY_LAST_VERSION, result.release.version)
          .putString(KEY_LAST_NAME, result.release.name)
          .putString(KEY_LAST_RELEASE_URL, result.release.releaseUrl)
          .putString(KEY_LAST_DOWNLOAD_URL, result.release.downloadUrl)
          .putString(KEY_LAST_ASSET_NAME, result.release.assetName)
          .putString(KEY_LAST_CHECKSUM_URL, result.release.checksumUrl)
          .putString(KEY_LAST_CHECKSUM, result.release.checksum)
          .putBoolean(KEY_LAST_PRERELEASE, result.release.prerelease);
    }
    editor.apply();
  }

  private static String request(String endpoint, String accept) throws IOException {
    HttpURLConnection connection = null;
    try {
      connection = connection(endpoint, accept);
      int code = connection.getResponseCode();
      if (code < 200 || code >= 300) throw new IOException("GitHub respondeu com " + code + ".");
      return readLimited(connection.getInputStream(), MAX_RESPONSE_BYTES);
    } finally {
      if (connection != null) connection.disconnect();
    }
  }

  private static HttpURLConnection connection(String endpoint, String accept) throws IOException {
    URL url = new URL(endpoint);
    if (!"https".equalsIgnoreCase(url.getProtocol()) || !isGithubHost(url.getHost())) {
      throw new IOException("Origem de atualização não permitida.");
    }
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
    connection.setReadTimeout(READ_TIMEOUT_MS);
    connection.setInstanceFollowRedirects(true);
    connection.setRequestProperty("Accept", accept);
    connection.setRequestProperty("User-Agent", "Desperta-Android");
    return connection;
  }

  private static String readLimited(InputStream stream, int limit) throws IOException {
    try (InputStream input = stream;
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      StringBuilder result = new StringBuilder();
      char[] buffer = new char[8192];
      int count = 0;
      int read;
      while ((read = reader.read(buffer)) != -1) {
        count += read;
        if (count > limit) throw new IOException("Resposta muito grande.");
        result.append(buffer, 0, read);
      }
      return result.toString();
    }
  }

  private static boolean isChecksumAsset(String name) {
    String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
    return lower.equals("sha256sums.txt")
        || lower.endsWith(".sha256")
        || lower.endsWith(".sha256sum");
  }

  private static boolean isHttpsGithubUrl(String value) {
    if (value == null || value.isEmpty()) return false;
    try {
      URL url = new URL(value);
      return "https".equalsIgnoreCase(url.getProtocol()) && isGithubHost(url.getHost());
    } catch (Exception ignored) {
      return false;
    }
  }

  private static boolean isGithubHost(String host) {
    return "github.com".equalsIgnoreCase(host)
        || "objects.githubusercontent.com".equalsIgnoreCase(host)
        || "github-releases.githubusercontent.com".equalsIgnoreCase(host)
        || "api.github.com".equalsIgnoreCase(host);
  }

  private static String userMessage(Exception error) {
    if (error instanceof java.net.UnknownHostException) return "Sem conexão com a internet.";
    String message = error.getMessage();
    return message == null || message.isEmpty()
        ? "Não foi possível verificar atualizações agora."
        : message;
  }

  private static MessageDigest sha256() throws IOException {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (java.security.NoSuchAlgorithmException error) {
      throw new IOException("SHA-256 indisponível.", error);
    }
  }

  private static String hex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) builder.append(String.format(Locale.ROOT, "%02x", value & 0xff));
    return builder.toString();
  }
}
