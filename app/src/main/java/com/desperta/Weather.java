package com.desperta;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Small, dependency-free Open-Meteo client used by the alarm weather reminder.
 *
 * <p>The network work is always performed off the main thread. Results are kept in the app's
 * private preferences so the rest of the app can display the last successful result while offline.
 * The callback is always delivered on the main thread, including errors.
 */
public final class Weather {
  /** Shared with Store, MainActivity, and the alarm engine. */
  public static final String PREFS = "desperta";

  public static final String KEY_TEXT = "weather_text";
  public static final String KEY_TIME = "weather_time";
  public static final String KEY_CITY = "city";

  private static final String GEOCODING_ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search";
  private static final String FORECAST_ENDPOINT = "https://api.open-meteo.com/v1/forecast";
  private static final int CONNECT_TIMEOUT_MS = 10_000;
  private static final int READ_TIMEOUT_MS = 15_000;
  private static final int MAX_RESPONSE_BYTES = 1_048_576;

  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
  private static final Handler MAIN = new Handler(Looper.getMainLooper());

  private Weather() {}

  /**
   * Refreshes weather for {@code city} using Open-Meteo geocoding followed by its current forecast
   * endpoint. The supplied callback may be null.
   *
   * <p>Before the request starts, stale text and timestamp are removed. On a failure the text
   * contains a concise Portuguese error and the timestamp is zero, so callers never mistake an
   * error for fresh cached weather.
   */
  public static void refresh(Context context, String city, Runnable finished) {
    if (context == null) {
      dispatch(finished);
      return;
    }

    final Context app = context.getApplicationContext();
    final String requestedCity = city == null ? "" : city.trim();
    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CITY, requestedCity)
        .putString(KEY_TEXT, "Atualizando clima…")
        .putLong(KEY_TIME, 0L)
        .apply();

    EXECUTOR.execute(
        () -> {
          String resultText;
          boolean success = false;
          try {
            if (requestedCity.isEmpty()) {
              throw new IOException("Informe uma cidade");
            }
            Coordinates coordinates = geocode(requestedCity);
            resultText = forecast(coordinates);
            success = true;
          } catch (Exception error) {
            resultText = errorMessage(error);
          }

          final String text = resultText;
          final boolean ok = success;
          MAIN.post(
              () -> {
                android.content.SharedPreferences.Editor editor =
                    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY_TEXT, text);
                // A timestamp is only meaningful for a successful response.
                editor.putLong(KEY_TIME, ok ? System.currentTimeMillis() : 0L).apply();
                dispatch(finished);
              });
        });
  }

  public static String getCachedText(Context context) {
    if (context == null) return "";
    return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TEXT, "");
  }

  public static long getCachedTime(Context context) {
    if (context == null) return 0L;
    return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_TIME, 0L);
  }

  public static String getCity(Context context) {
    if (context == null) return "";
    return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CITY, "");
  }

  /** Clears all weather state, including the city entered by the user. */
  public static void clear(Context context) {
    if (context == null) return;
    context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_TEXT)
        .remove(KEY_TIME)
        .remove(KEY_CITY)
        .apply();
  }

  private static void dispatch(Runnable callback) {
    if (callback != null) MAIN.post(callback);
  }

  private static Coordinates geocode(String city) throws IOException, JSONException {
    String query =
        GEOCODING_ENDPOINT + "?name=" + encode(city) + "&count=1&language=pt&format=json";
    JSONObject root = getJson(query);
    JSONArray results = root.optJSONArray("results");
    if (results == null || results.length() == 0) {
      throw new IOException("Cidade não encontrada");
    }
    JSONObject first = results.getJSONObject(0);
    if (!first.has("latitude") || !first.has("longitude")) {
      throw new IOException("Localização sem coordenadas");
    }
    return new Coordinates(
        first.getDouble("latitude"),
        first.getDouble("longitude"),
        first.optString("name", city),
        first.optString("country", ""));
  }

  private static String forecast(Coordinates coordinates) throws IOException, JSONException {
    String query =
        FORECAST_ENDPOINT
            + "?latitude="
            + coordinates.latitude
            + "&longitude="
            + coordinates.longitude
            + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
            + "&timezone=auto";
    JSONObject root = getJson(query);
    JSONObject current = root.optJSONObject("current");
    if (current == null || !current.has("temperature_2m")) {
      throw new IOException("Previsão indisponível");
    }

    double temperature = current.getDouble("temperature_2m");
    int code = current.optInt("weather_code", -1);
    double wind = current.optDouble("wind_speed_10m", Double.NaN);
    int humidity = current.optInt("relative_humidity_2m", -1);
    StringBuilder text = new StringBuilder().append(coordinates.name);
    if (!coordinates.country.isEmpty()) text.append(", ").append(coordinates.country);
    text.append(" · ")
        .append(String.format(Locale.getDefault(), "%.0f°C", temperature))
        .append(" · ")
        .append(description(code));
    if (humidity >= 0) text.append(" · umidade ").append(humidity).append("%");
    if (!Double.isNaN(wind)) {
      text.append(" · vento ").append(String.format(Locale.getDefault(), "%.0f km/h", wind));
    }
    return text.toString();
  }

  private static JSONObject getJson(String address) throws IOException, JSONException {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) URI.create(address).toURL().openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
      connection.setReadTimeout(READ_TIMEOUT_MS);
      connection.setRequestProperty("Accept", "application/json");
      connection.setUseCaches(false);
      int response = connection.getResponseCode();
      InputStream stream =
          response >= 200 && response < 300
              ? connection.getInputStream()
              : connection.getErrorStream();
      String body = readLimited(stream);
      if (response < 200 || response >= 300) {
        throw new IOException("Servidor retornou HTTP " + response);
      }
      return new JSONObject(body);
    } finally {
      if (connection != null) connection.disconnect();
    }
  }

  private static String readLimited(InputStream stream) throws IOException {
    if (stream == null) throw new IOException("Resposta vazia");
    try (InputStream input = stream;
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      StringBuilder body = new StringBuilder();
      String line;
      int bytes = 0;
      while ((line = reader.readLine()) != null) {
        bytes += line.getBytes(StandardCharsets.UTF_8).length + 1;
        if (bytes > MAX_RESPONSE_BYTES) throw new IOException("Resposta muito grande");
        body.append(line);
      }
      return body.toString();
    }
  }

  private static String encode(String value) throws IOException {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    } catch (Exception error) {
      throw new IOException("Não foi possível codificar a cidade", error);
    }
  }

  private static String errorMessage(Exception error) {
    String message = error.getMessage();
    if (message == null || message.trim().isEmpty()) message = "não foi possível atualizar";
    return "Clima indisponível: " + message;
  }

  private static String description(int code) {
    switch (code) {
      case 0:
        return "céu limpo";
      case 1:
      case 2:
        return "parcialmente nublado";
      case 3:
        return "nublado";
      case 45:
      case 48:
        return "neblina";
      case 51:
      case 53:
      case 55:
        return "garoa";
      case 56:
      case 57:
        return "garoa congelante";
      case 61:
      case 63:
      case 65:
        return "chuva";
      case 66:
      case 67:
        return "chuva congelante";
      case 71:
      case 73:
      case 75:
      case 77:
        return "neve";
      case 80:
      case 81:
      case 82:
        return "pancadas de chuva";
      case 85:
      case 86:
        return "pancadas de neve";
      case 95:
        return "trovoada";
      case 96:
      case 99:
        return "trovoada com granizo";
      default:
        return "condição desconhecida";
    }
  }

  private static final class Coordinates {
    final double latitude;
    final double longitude;
    final String name;
    final String country;

    Coordinates(double latitude, double longitude, String name, String country) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.name = name;
      this.country = country;
    }
  }
}
