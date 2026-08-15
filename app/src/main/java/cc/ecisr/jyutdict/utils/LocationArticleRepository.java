package cc.ecisr.jyutdict.utils;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.ecisr.jyutdict.struct.LocationInfo;

/**
 * 地點文章目錄及別名的一次性讀取層。
 *
 * 目錄快取一天；同一進程的並發卡片共用一次請求。這比逐地點發送 exists 請求更省伺服器。
 */
public final class LocationArticleRepository {
    private static final String URL =
            "https://jyutdict.org/api/v1.0/articles/?list=1&type=location";
    private static final String CACHE_KEY = "location_article_directory_v1";
    private static final long MAX_AGE = 24L * 60L * 60L * 1000L;

    private static final Object LOCK = new Object();
    private static final Map<String, String> RESOLVED_NAMES = new HashMap<>();
    private static final Map<String, String> LOCATION_INFO_NAMES = new HashMap<>();
    private static final Set<String> AVAILABLE_NAMES = new HashSet<>();
    private static final ArrayList<Pending> PENDING = new ArrayList<>();
    private static boolean ready;
    private static boolean loading;
    private static HttpUtil request;

    private LocationArticleRepository() {
    }

    public interface Callback {
        void onResult(Result result);
    }

    public static final class Result {
        public final String requestedName;
        public final String resolvedName;
        public final boolean articleAvailable;
        public final LocationInfo.Location location;

        Result(String requestedName, String resolvedName, String locationInfoName,
               boolean articleAvailable) {
            this.requestedName = requestedName;
            this.resolvedName = resolvedName;
            this.articleAvailable = articleAvailable;
            this.location = LocationInfo.findByName(locationInfoName);
        }

        public boolean redirected() {
            return !requestedName.equals(resolvedName);
        }
    }

    public static void lookup(Context context, String requestedName, Callback callback) {
        Context appContext = context.getApplicationContext();
        String cleanName = requestedName == null ? "" : requestedName.trim();
        synchronized (LOCK) {
            if (!ready) {
                String cached = DiskTextCache.readFresh(appContext, CACHE_KEY, MAX_AGE);
                if (cached != null) ready = parseDirectory(cached);
            }
            if (ready) {
                callback.onResult(resultFor(cleanName));
                return;
            }
            PENDING.add(new Pending(cleanName, callback));
            if (loading) return;
            loading = true;
        }

        request = new HttpUtil();
        request.enqueue(URL, new HttpUtil.Callback() {
            @Override
            public void onSuccess(String body) {
                finishLoad(appContext, body);
            }

            @Override
            public void onFailure(HttpUtil.RequestError error) {
                finishLoad(appContext, null);
            }
        });
    }

    private static void finishLoad(Context context, String response) {
        boolean parsed = response != null && parseDirectory(response);
        if (!parsed) {
            String stale = DiskTextCache.readAny(context, CACHE_KEY);
            if (stale != null) parsed = parseDirectory(stale);
        }
        if (parsed && response != null) DiskTextCache.write(context, CACHE_KEY, response);
        ArrayList<Pending> callbacks;
        synchronized (LOCK) {
            ready = parsed;
            loading = false;
            callbacks = new ArrayList<>(PENDING);
            PENDING.clear();
        }
        for (Pending pending : callbacks) {
            pending.callback.onResult(resultFor(pending.requestedName));
        }
    }

    public static void clearMemoryCache() {
        synchronized (LOCK) {
            if (request != null) request.cancel();
            request = null;
            ready = false;
            loading = false;
            PENDING.clear();
            RESOLVED_NAMES.clear();
            LOCATION_INFO_NAMES.clear();
            AVAILABLE_NAMES.clear();
        }
    }

    private static Result resultFor(String requestedName) {
        synchronized (LOCK) {
            String resolved = RESOLVED_NAMES.containsKey(requestedName)
                    ? RESOLVED_NAMES.get(requestedName)
                    : requestedName;
            return new Result(
                    requestedName,
                    resolved,
                    LOCATION_INFO_NAMES.containsKey(resolved)
                            ? LOCATION_INFO_NAMES.get(resolved)
                            : resolved,
                    AVAILABLE_NAMES.contains(requestedName)
                            || AVAILABLE_NAMES.contains(resolved)
            );
        }
    }

    private static boolean parseDirectory(String raw) {
        try {
            JSONObject root = new JSONObject(raw);
            JSONArray articles = root.optJSONArray("articles");
            if (articles == null) return false;
            synchronized (LOCK) {
                RESOLVED_NAMES.clear();
                LOCATION_INFO_NAMES.clear();
                AVAILABLE_NAMES.clear();
                for (int i = 0; i < articles.length(); i++) {
                    JSONObject article = articles.optJSONObject(i);
                    if (article == null) continue;
                    String canonical = article.optString("location_name", "").trim();
                    if (canonical.isEmpty()) continue;
                    RESOLVED_NAMES.put(canonical, canonical);
                    AVAILABLE_NAMES.add(canonical);
                    JSONArray aliases = article.optJSONArray("aliases");
                    ArrayList<String> articleNames = new ArrayList<>();
                    articleNames.add(canonical);
                    if (aliases != null) {
                        for (int aliasIndex = 0; aliasIndex < aliases.length(); aliasIndex++) {
                            String alias = aliases.optString(aliasIndex, "").trim();
                            if (alias.isEmpty()) continue;
                            articleNames.add(alias);
                            RESOLVED_NAMES.put(alias, canonical);
                            AVAILABLE_NAMES.add(alias);
                        }
                    }
                    for (String articleName : articleNames) {
                        if (LocationInfo.findByName(articleName) != null) {
                            LOCATION_INFO_NAMES.put(canonical, articleName);
                            break;
                        }
                    }
                }
            }
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    private static final class Pending {
        final String requestedName;
        final Callback callback;

        Pending(String requestedName, Callback callback) {
            this.requestedName = requestedName;
            this.callback = callback;
        }
    }
}
