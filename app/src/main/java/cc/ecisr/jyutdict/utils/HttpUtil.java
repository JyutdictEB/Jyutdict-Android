package cc.ecisr.jyutdict.utils;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

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

/**
 * 輕量只讀 HTTP 客戶端。
 *
 * 每次 start() 都會快照 URL、Handler 和成功消息代碼，並取消同一實例的舊請求。
 * 只有最新一代請求可以向 Handler 投遞結果。
 */
public class HttpUtil {
    private static final String TAG = "HttpUtil";

    public static final Boolean GET = false;

    public static final int EMPTY_URL_OR_HANDLER = 9100;
    public static final int REQUEST_CONTENT_SUCCESSFULLY = 9200;
    public static final int REQUEST_CONTENT_FAIL = 9201;

    public enum ErrorKind {
        INVALID_REQUEST,
        TIMEOUT,
        NETWORK,
        HTTP
    }

    public static final class RequestError {
        public final ErrorKind kind;
        public final int statusCode;
        public final String detail;

        RequestError(ErrorKind kind, int statusCode, String detail) {
            this.kind = kind;
            this.statusCode = statusCode;
            this.detail = detail == null ? "" : detail;
        }

        @Override
        public String toString() {
            if (kind == ErrorKind.HTTP && statusCode > 0) {
                return String.valueOf(statusCode);
            }
            return kind.name().toLowerCase(Locale.ROOT);
        }
    }

    private String urlStr = "";
    private Handler handler;
    private final Boolean mode;
    private int messageWhat = REQUEST_CONTENT_SUCCESSFULLY;
    private int failureWhat = REQUEST_CONTENT_FAIL;

    private int generation = 0;
    private volatile GetThread getThread;
    private volatile HttpURLConnection activeConnection;

    public HttpUtil(Boolean mode) {
        this.mode = mode;
    }

    public synchronized HttpUtil setUrl(String urlStr) {
        this.urlStr = urlStr;
        return this;
    }

    public synchronized HttpUtil setHandler(Handler handler) {
        this.handler = handler;
        this.messageWhat = REQUEST_CONTENT_SUCCESSFULLY;
        this.failureWhat = REQUEST_CONTENT_FAIL;
        return this;
    }

    public synchronized HttpUtil setHandler(Handler handler, int what) {
        this.handler = handler;
        this.messageWhat = what;
        this.failureWhat = REQUEST_CONTENT_FAIL;
        return this;
    }

    public synchronized HttpUtil setHandler(Handler handler, int successWhat, int failureWhat) {
        this.handler = handler;
        this.messageWhat = successWhat;
        this.failureWhat = failureWhat;
        return this;
    }

    public synchronized void start() {
        cancelActiveLocked();
        final int requestGeneration = ++generation;
        final String requestUrl = urlStr;
        final Handler requestHandler = handler;
        final int successWhat = messageWhat;
        final int requestFailureWhat = failureWhat;

        if (mode != GET || requestUrl == null || requestUrl.isEmpty() || requestHandler == null) {
            if (requestHandler != null) {
                deliver(requestHandler, requestGeneration, requestFailureWhat,
                        new RequestError(ErrorKind.INVALID_REQUEST, 0, "Missing URL or handler"));
            }
            return;
        }

        GetThread thread = new GetThread(
                requestGeneration,
                requestUrl,
                requestHandler,
                successWhat,
                requestFailureWhat
        );
        getThread = thread;
        thread.start();
    }

    public synchronized void cancel() {
        generation++;
        cancelActiveLocked();
    }

    private void cancelActiveLocked() {
        HttpURLConnection connection = activeConnection;
        activeConnection = null;
        if (connection != null) {
            connection.disconnect();
        }

        GetThread thread = getThread;
        getThread = null;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private synchronized boolean isCurrent(int requestGeneration) {
        return requestGeneration == generation;
    }

    private void deliver(Handler target, int requestGeneration, int what, Object payload) {
        target.post(() -> {
            if (!isCurrent(requestGeneration)) return;
            Message message = Message.obtain();
            message.what = what;
            message.obj = payload;
            target.dispatchMessage(message);
        });
    }

    private final class GetThread extends Thread {
        private final int requestGeneration;
        private final String requestUrl;
        private final Handler requestHandler;
        private final int successWhat;
        private final int failureWhat;

        GetThread(int requestGeneration, String requestUrl, Handler requestHandler,
                  int successWhat, int failureWhat) {
            this.requestGeneration = requestGeneration;
            this.requestUrl = requestUrl;
            this.requestHandler = requestHandler;
            this.successWhat = successWhat;
            this.failureWhat = failureWhat;
        }

        @Override
        public void run() {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(requestUrl);
                Log.i(TAG, "request: " + url.getPath());
                connection = (HttpURLConnection) url.openConnection();

                synchronized (HttpUtil.this) {
                    if (!isCurrent(requestGeneration)) return;
                    activeConnection = connection;
                }

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");

                int statusCode = connection.getResponseCode();
                if (statusCode >= 200 && statusCode < 300) {
                    InputStream responseStream = connection.getInputStream();
                    String contentEncoding = connection.getContentEncoding();
                    if (contentEncoding != null
                            && contentEncoding.toLowerCase(Locale.ROOT).contains("gzip")) {
                        responseStream = new GZIPInputStream(responseStream);
                    }
                    String body = readBody(responseStream);
                    Log.i(TAG, "received, LEN = " + body.length() + ", WHAT = " + successWhat);
                    deliver(requestHandler, requestGeneration, successWhat, body);
                } else {
                    deliver(requestHandler, requestGeneration, failureWhat,
                            new RequestError(ErrorKind.HTTP, statusCode, "HTTP " + statusCode));
                }
            } catch (SocketTimeoutException e) {
                if (isCurrent(requestGeneration)) {
                    deliver(requestHandler, requestGeneration, failureWhat,
                            new RequestError(ErrorKind.TIMEOUT, 0, e.getMessage()));
                }
            } catch (IOException e) {
                if (isCurrent(requestGeneration) && !isInterrupted()) {
                    Log.w(TAG, "request failed: " + e.getClass().getSimpleName());
                    deliver(requestHandler, requestGeneration, failureWhat,
                            new RequestError(ErrorKind.NETWORK, 0, e.getMessage()));
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                synchronized (HttpUtil.this) {
                    if (activeConnection == connection) activeConnection = null;
                    if (getThread == this) getThread = null;
                }
            }
        }

        private String readBody(InputStream inputStream) throws IOException {
            StringBuilder result = new StringBuilder();
            try (InputStream is = inputStream;
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(reader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line).append('\n');
                }
            }
            return result.toString();
        }
    }
}
