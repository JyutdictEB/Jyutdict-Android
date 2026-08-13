package cc.ecisr.jyutdict.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Stores the bearer session cookie encrypted with an Android Keystore key. */
public final class SecureSessionStore {
    private static final String TAG = "SecureSessionStore";
    private static final String PREFS = "secure_api_session_v1";
    private static final String KEY_ALIAS = "jyutdict_api_session_aes_v1";
    private static final String KEY_SESSION = "session";
    private static final String KEY_CSRF = "csrf";
    private static final int GCM_TAG_BITS = 128;

    private final SharedPreferences preferences;

    public SecureSessionStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized String getSessionId() {
        return read(KEY_SESSION);
    }

    public synchronized void setSessionId(String sessionId) {
        write(KEY_SESSION, sessionId);
    }

    public synchronized String getCsrfToken() {
        return read(KEY_CSRF);
    }

    public synchronized void setCsrfToken(String csrfToken) {
        write(KEY_CSRF, csrfToken);
    }

    public synchronized void clear() {
        preferences.edit().clear().apply();
    }

    private String read(String key) {
        String stored = preferences.getString(key, "");
        if (stored == null || stored.isEmpty()) return "";
        try {
            String[] parts = stored.split("\\.", 2);
            if (parts.length != 2) throw new IllegalArgumentException("Invalid ciphertext");
            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP | Base64.URL_SAFE);
            byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP | Base64.URL_SAFE);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            Log.w(TAG, "Unable to decrypt stored session value", exception);
            preferences.edit().remove(key).apply();
            return "";
        }
    }

    private void write(String key, String value) {
        if (value == null || value.isEmpty()) {
            preferences.edit().remove(key).apply();
            return;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.encodeToString(
                    cipher.getIV(), Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING)
                    + "."
                    + Base64.encodeToString(
                    encrypted, Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING);
            preferences.edit().putString(key, encoded).apply();
        } catch (Exception exception) {
            Log.e(TAG, "Unable to encrypt session value", exception);
            preferences.edit().remove(key).apply();
        }
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
