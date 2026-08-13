package cc.ecisr.jyutdict.auth;

import android.content.Context;

import org.json.JSONObject;

import cc.ecisr.jyutdict.network.ApiClient;
import cc.ecisr.jyutdict.network.SecureSessionStore;

/** Owns the website-compatible PHP session and the current signed-in user. */
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
        User user = currentUser;
        return user != null && ("admin".equals(user.role) || "owner".equals(user.role));
    }

    public void initialize(Completion completion) {
        apiClient.get("/api/auth/me", new ApiClient.Callback() {
            @Override
            public void onSuccess(ApiClient.ApiResponse response) {
                applyAuthResponse(response.body);
                completion.onComplete(true, null);
            }

            @Override
            public void onFailure(ApiClient.ApiFailure failure) {
                currentUser = null;
                completion.onComplete(false, failure.message);
            }
        });
    }

    public void requestLoginNonce(ValueCallback<String> callback) {
        apiClient.get("/api/auth/mobile", new ApiClient.Callback() {
            @Override
            public void onSuccess(ApiClient.ApiResponse response) {
                String nonce = response.body.optString("nonce", "");
                if (nonce.isEmpty()) callback.onResult(null, "Server returned no login challenge");
                else callback.onResult(nonce, null);
            }

            @Override
            public void onFailure(ApiClient.ApiFailure failure) {
                callback.onResult(null, failure.message);
            }
        });
    }

    public void completeGoogleLogin(String idToken, Completion completion) {
        JSONObject body = new JSONObject();
        try {
            body.put("id_token", idToken);
        } catch (Exception exception) {
            completion.onComplete(false, exception.getMessage());
            return;
        }
        apiClient.post("/api/auth/mobile", body, false, new ApiClient.Callback() {
            @Override
            public void onSuccess(ApiClient.ApiResponse response) {
                applyAuthResponse(response.body);
                completion.onComplete(currentUser != null,
                        currentUser == null ? "Server returned no user" : null);
            }

            @Override
            public void onFailure(ApiClient.ApiFailure failure) {
                completion.onComplete(false, failure.message);
            }
        });
    }

    public void logout(Completion completion) {
        apiClient.post("/api/auth/logout", new JSONObject(), false, new ApiClient.Callback() {
            @Override
            public void onSuccess(ApiClient.ApiResponse response) {
                clearLocalSession();
                completion.onComplete(true, null);
            }

            @Override
            public void onFailure(ApiClient.ApiFailure failure) {
                clearLocalSession();
                completion.onComplete(false, failure.message);
            }
        });
    }

    private void applyAuthResponse(JSONObject body) {
        JSONObject userJson = body.optJSONObject("user");
        if (userJson == null) {
            currentUser = null;
            sessionStore.setCsrfToken("");
            return;
        }
        currentUser = new User(
                userJson.optInt("id"),
                userJson.optString("email", ""),
                userJson.optString("nickname", ""),
                userJson.optString("role", "user")
        );
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
        public final String email;
        public final String nickname;
        public final String role;

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
