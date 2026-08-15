package cc.ecisr.jyutdict.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/** Stateful JSON API client for authenticated Jyutdict requests. */
public final class ApiClient {
    public static final String BASE_URL = "https://jyutdict.org";
    private static final Pattern SESSION_COOKIE = Pattern.compile(
            "(?:^|;\\s*)PHPSESSID=([^;]*)", Pattern.CASE_INSENSITIVE);
    private static volatile ApiClient instance;

    private final SecureSessionStore sessionStore;
    // Session-cookie operations must remain ordered (notably /auth/me -> nonce -> login).
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static ApiClient getInstance(Context context) {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) instance = new ApiClient(context.getApplicationContext());
            }
        }
        return instance;
    }

    private ApiClient(Context context) {
        sessionStore = new SecureSessionStore(context);
    }

    public SecureSessionStore getSessionStore() {
        return sessionStore;
    }

    public void get(String pathAndQuery, Callback callback) {
        request("GET", pathAndQuery, null, false, callback);
    }

    public void post(String path, JSONObject body, boolean includeCsrf, Callback callback) {
        request("POST", path, body, includeCsrf, callback);
    }

    public void delete(String path, JSONObject body, Callback callback) {
        request("DELETE", path, body, true, callback);
    }

    private void request(
            String method,
            String pathAndQuery,
            JSONObject body,
            boolean includeCsrf,
            Callback callback
    ) {
        executor.execute(() -> execute(method, pathAndQuery, body, includeCsrf, callback));
    }

    private void execute(
            String method,
            String pathAndQuery,
            JSONObject body,
            boolean includeCsrf,
            Callback callback
    ) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(pathAndQuery.startsWith("http")
                    ? pathAndQuery
                    : BASE_URL + pathAndQuery);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty("User-Agent", "Jyutdict-Android");

            String sessionId = sessionStore.getSessionId();
            if (!sessionId.isEmpty()) {
                connection.setRequestProperty("Cookie", "PHPSESSID=" + sessionId);
            }
            if (includeCsrf) {
                String csrfToken = sessionStore.getCsrfToken();
                if (!csrfToken.isEmpty()) {
                    connection.setRequestProperty("X-CSRF-Token", csrfToken);
                }
            }

            if (body != null) {
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(bytes.length);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }
            }

            int status = connection.getResponseCode();
            captureSessionCookie(connection.getHeaderFields());
            InputStream rawStream = status >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();
            String rawBody = readBody(rawStream, connection.getContentEncoding());
            JSONObject json = rawBody.isEmpty() ? new JSONObject() : new JSONObject(rawBody);
            if (status >= 200 && status < 300) {
                deliver(callback, json, null);
            } else {
                deliver(callback, null, json.optString("error", "HTTP " + status));
            }
        } catch (IOException | JSONException exception) {
            deliver(callback, null, exception.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void captureSessionCookie(Map<String, List<String>> headers) {
        if (headers == null) return;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null || !"Set-Cookie".equalsIgnoreCase(entry.getKey())) continue;
            for (String header : entry.getValue()) {
                Matcher matcher = SESSION_COOKIE.matcher(header);
                if (!matcher.find()) continue;
                String sessionId = matcher.group(1);
                sessionStore.setSessionId(sessionId == null ? "" : sessionId);
            }
        }
    }

    private String readBody(InputStream stream, String contentEncoding) throws IOException {
        if (stream == null) return "";
        InputStream decoded = "gzip".equalsIgnoreCase(contentEncoding)
                ? new GZIPInputStream(stream)
                : stream;
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(decoded, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }

    private void deliver(Callback callback, JSONObject response, String error) {
        String message = response == null && (error == null || error.isEmpty())
                ? "Network request failed" : error;
        mainHandler.post(() -> callback.onComplete(response, message));
    }

    public interface Callback {
        void onComplete(JSONObject response, String errorMessage);
    }
}
