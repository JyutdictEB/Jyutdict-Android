package cc.ecisr.jyutdict.auth;

import android.content.Context;

import org.json.JSONObject;

import cc.ecisr.jyutdict.network.ApiClient;
import cc.ecisr.jyutdict.network.SecureSessionStore;

/** Owns the website-compatible PHP session and current signed-in user. */
public final class AuthRepository {
    private static volatile AuthRepository instance;
    private final ApiClient apiClient;
    private final SecureSessionStore sessionStore;
    private volatile User currentUser;

    public static AuthRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) instance = new AuthRepository(context.getApplicationContext());
            }
        }
        return instance;
    }

    private AuthRepository(Context context) {
        apiClient = ApiClient.getInstance(context);
        sessionStore = apiClient.getSessionStore();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null
                && ("admin".equals(currentUser.role) || "owner".equals(currentUser.role));
    }

    public void initialize(Completion completion) {
        apiClient.get("/api/auth/me", (body, error) -> {
            if (error == null) applyAuthResponse(body);
            else currentUser = null;
            completion.onComplete(error == null, error);
        });
    }

    public void requestLoginNonce(ValueCallback<String> callback) {
        apiClient.get("/api/auth/mobile", (body, error) -> {
            if (error != null) {
                callback.onResult(null, error);
                return;
            }
            String nonce = body.optString("nonce", "");
            callback.onResult(nonce.isEmpty() ? null : nonce,
                    nonce.isEmpty() ? "Server returned no login challenge" : null);
        });
    }

    public void completeGoogleLogin(String idToken, Completion completion) {
        JSONObject request = new JSONObject();
        try {
            request.put("id_token", idToken);
        } catch (Exception exception) {
            completion.onComplete(false, exception.getMessage());
            return;
        }
        apiClient.post("/api/auth/mobile", request, false, (body, error) -> {
            if (error == null) applyAuthResponse(body);
            boolean success = error == null && currentUser != null;
            completion.onComplete(success,
                    error != null ? error : success ? null : "Server returned no user");
        });
    }

    public void logout(Completion completion) {
        apiClient.post("/api/auth/logout", new JSONObject(), false, (body, error) -> {
            clearLocalSession();
            completion.onComplete(error == null, error);
        });
    }

    private void applyAuthResponse(JSONObject body) {
        JSONObject json = body.optJSONObject("user");
        if (json == null) {
            currentUser = null;
            sessionStore.setCsrfToken("");
            return;
        }
        currentUser = new User(json.optInt("id"), json.optString("email", ""),
                json.optString("nickname", ""), json.optString("role", "user"));
        sessionStore.setCsrfToken(body.optString("csrf_token", ""));
    }

    private void clearLocalSession() {
        currentUser = null;
        sessionStore.clear();
    }

    public interface Completion {
        void onComplete(boolean success, String errorMessage);
    }

    public interface ValueCallback<T> {
        void onResult(T value, String errorMessage);
    }

    public static final class User {
        public final int id;
        public final String email, nickname, role;

        User(int id, String email, String nickname, String role) {
            this.id = id;
            this.email = email;
            this.nickname = nickname;
            this.role = role;
        }

        public String displayName() {
            if (!nickname.isEmpty()) return nickname;
            int at = email.indexOf('@');
            return at > 0 ? email.substring(0, at) : email;
        }
    }
}
