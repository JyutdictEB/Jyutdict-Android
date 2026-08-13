package cc.ecisr.jyutdict.comments;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.ecisr.jyutdict.network.ApiClient;

/** Maps char/sheet comment operations onto the website's existing API. */
public final class CommentRepository {
    public static final String TYPE_CHAR = "char";
    public static final String TYPE_SHEET = "sheet";

    private final ApiClient apiClient;

    public CommentRepository(Context context) {
        apiClient = ApiClient.getInstance(context);
    }

    public void getComments(String type, String target, CommentsCallback callback) {
        String parameter = TYPE_CHAR.equals(type) ? "chara" : "key";
        apiClient.get(endpoint(type) + "?" + parameter + "=" + encode(target),
                new ApiClient.Callback() {
                    @Override
                    public void onSuccess(ApiClient.ApiResponse response) {
                        JSONArray array = response.body.optJSONArray("comments");
                        ArrayList<Comment> comments = new ArrayList<>();
                        if (array != null) {
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.optJSONObject(i);
                                if (object != null) comments.add(Comment.fromJson(object));
                            }
                        }
                        callback.onResult(comments, null);
                    }

                    @Override
                    public void onFailure(ApiClient.ApiFailure failure) {
                        callback.onResult(null, failure.message);
                    }
                });
    }

    public void postComment(String type, String target, String content, Completion callback) {
        JSONObject body = new JSONObject();
        try {
            body.put(TYPE_CHAR.equals(type) ? "chara" : "sheet_key", target);
            body.put("content", content);
        } catch (JSONException exception) {
            callback.onComplete(false, exception.getMessage());
            return;
        }
        apiClient.post(endpoint(type), body, true, completionCallback(callback));
    }

    public void deleteComment(String type, int commentId, Completion callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("comment_id", commentId);
        } catch (JSONException exception) {
            callback.onComplete(false, exception.getMessage());
            return;
        }
        apiClient.delete(endpoint(type), body, completionCallback(callback));
    }

    public void getCounts(String type, List<String> targets, CountsCallback callback) {
        if (targets.isEmpty()) {
            callback.onResult(new LinkedHashMap<>(), null);
            return;
        }
        StringBuilder joined = new StringBuilder();
        for (String target : targets) {
            if (joined.length() > 0) joined.append(',');
            joined.append(target);
        }
        apiClient.get("/api/v1.0/comments/counts?type=" + encode(type)
                        + "&targets=" + encode(joined.toString()),
                new ApiClient.Callback() {
                    @Override
                    public void onSuccess(ApiClient.ApiResponse response) {
                        JSONObject countsJson = response.body.optJSONObject("counts");
                        Map<String, Integer> counts = new LinkedHashMap<>();
                        for (String target : targets) {
                            counts.put(target, countsJson == null ? 0 : countsJson.optInt(target, 0));
                        }
                        callback.onResult(counts, null);
                    }

                    @Override
                    public void onFailure(ApiClient.ApiFailure failure) {
                        callback.onResult(null, failure.message);
                    }
                });
    }

    private ApiClient.Callback completionCallback(Completion completion) {
        return new ApiClient.Callback() {
            @Override
            public void onSuccess(ApiClient.ApiResponse response) {
                completion.onComplete(true, null);
            }

            @Override
            public void onFailure(ApiClient.ApiFailure failure) {
                completion.onComplete(false, failure.message);
            }
        };
    }

    private String endpoint(String type) {
        if (!TYPE_CHAR.equals(type) && !TYPE_SHEET.equals(type)) {
            throw new IllegalArgumentException("Unknown comment type: " + type);
        }
        return "/api/v1.0/comments/" + type;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 is unavailable", exception);
        }
    }

    public interface CommentsCallback {
        void onResult(List<Comment> comments, String errorMessage);
    }

    public interface CountsCallback {
        void onResult(Map<String, Integer> counts, String errorMessage);
    }

    public interface Completion {
        void onComplete(boolean success, String errorMessage);
    }
}
