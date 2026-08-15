package cc.ecisr.jyutdict.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/** Small cancellable GET client which always delivers its latest result on the main thread. */
public final class HttpUtil {
    private static final String TAG = "HttpUtil";

    public enum ErrorKind { INVALID_REQUEST, TIMEOUT, NETWORK, HTTP }

    public interface Callback {
        void onSuccess(String body);
        void onFailure(RequestError error);
    }

    public record RequestError(ErrorKind kind, int statusCode, String detail) {
            public RequestError(ErrorKind kind, int statusCode, String detail) {
                this.kind = kind;
                this.statusCode = statusCode;
                this.detail = detail == null ? "" : detail;
            }

            @NonNull
            @Override
            public String toString() {
                return kind == ErrorKind.HTTP && statusCode > 0
                        ? String.valueOf(statusCode)
                        : kind.name().toLowerCase(Locale.ROOT);
            }
        }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int connectTimeoutMillis = 5000, readTimeoutMillis = 5000;
    private int generation;
    private volatile Thread worker;
    private volatile HttpURLConnection connection;

    public synchronized HttpUtil setTimeouts(int connectMillis, int readMillis) {
        connectTimeoutMillis = Math.max(1000, connectMillis);
        readTimeoutMillis = Math.max(1000, readMillis);
        return this;
    }

    public synchronized void enqueue(String requestUrl, Callback callback) {
        cancelActive();
        int requestGeneration = ++generation;
        if (requestUrl == null || requestUrl.isEmpty()) {
            deliver(requestGeneration, () -> callback.onFailure(new RequestError(
                    ErrorKind.INVALID_REQUEST, 0, "Missing URL")));
            return;
        }
        int connectTimeout = connectTimeoutMillis, readTimeout = readTimeoutMillis;
        worker = new Thread(() -> execute(requestGeneration, requestUrl, callback,
                connectTimeout, readTimeout), "JyutdictHttp");
        worker.start();
    }

    public synchronized void cancel() {
        generation++;
        cancelActive();
    }

    private void cancelActive() {
        HttpURLConnection activeConnection = connection;
        connection = null;
        if (activeConnection != null) activeConnection.disconnect();
        Thread activeWorker = worker;
        worker = null;
        if (activeWorker != null) activeWorker.interrupt();
    }

    private synchronized boolean isCurrent(int requestGeneration) {
        return requestGeneration == generation;
    }

    private void deliver(int requestGeneration, Runnable callback) {
        mainHandler.post(() -> {
            if (isCurrent(requestGeneration)) callback.run();
        });
    }

    private void execute(int requestGeneration, String requestUrl, Callback callback,
                         int connectTimeout, int readTimeout) {
        HttpURLConnection activeConnection = null;
        try {
            URL url = new URL(requestUrl);
            Log.i(TAG, "request: " + url.getPath());
            activeConnection = (HttpURLConnection) url.openConnection();
            synchronized (this) {
                if (!isCurrent(requestGeneration)) return;
                connection = activeConnection;
            }
            activeConnection.setRequestMethod("GET");
            activeConnection.setConnectTimeout(connectTimeout);
            activeConnection.setReadTimeout(readTimeout);
            activeConnection.setUseCaches(false);
            activeConnection.setRequestProperty("Accept", "application/json");

            int statusCode = activeConnection.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
                InputStream stream = activeConnection.getInputStream();
                String encoding = activeConnection.getContentEncoding();
                if (encoding != null && encoding.toLowerCase(Locale.ROOT).contains("gzip")) {
                    stream = new GZIPInputStream(stream);
                }
                String body = readBody(stream);
                Log.i(TAG, "received, LEN = " + body.length());
                deliver(requestGeneration, () -> callback.onSuccess(body));
            } else {
                RequestError error = new RequestError(
                        ErrorKind.HTTP, statusCode, "HTTP " + statusCode);
                deliver(requestGeneration, () -> callback.onFailure(error));
            }
        } catch (SocketTimeoutException exception) {
            RequestError error = new RequestError(
                    ErrorKind.TIMEOUT, 0, exception.getMessage());
            deliver(requestGeneration, () -> callback.onFailure(error));
        } catch (IOException exception) {
            if (isCurrent(requestGeneration) && !Thread.currentThread().isInterrupted()) {
                Log.w(TAG, "request failed: " + exception.getClass().getSimpleName());
                RequestError error = new RequestError(
                        ErrorKind.NETWORK, 0, exception.getMessage());
                deliver(requestGeneration, () -> callback.onFailure(error));
            }
        } finally {
            if (activeConnection != null) activeConnection.disconnect();
            synchronized (this) {
                if (connection == activeConnection) connection = null;
                if (worker == Thread.currentThread()) worker = null;
            }
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (InputStream input = stream;
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
             BufferedReader buffered = new BufferedReader(reader)) {
            String line;
            while ((line = buffered.readLine()) != null) result.append(line).append('\n');
        }
        return result.toString();
    }
}
