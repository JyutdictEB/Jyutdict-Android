package cc.ecisr.jyutdict.auth;

import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import cc.ecisr.jyutdict.BuildConfig;
import cc.ecisr.jyutdict.R;
import cc.ecisr.jyutdict.utils.ToastUtil;

/** Runs the Credential Manager flow from the account section in Settings. */
public final class GoogleSignInCoordinator {
    private static final String TAG = "GoogleSignIn";

    private final AppCompatActivity activity;
    private final AuthRepository authRepository;
    private final CredentialManager credentialManager;
    private boolean signInInProgress;

    public GoogleSignInCoordinator(AppCompatActivity activity) {
        this.activity = activity;
        authRepository = AuthRepository.getInstance(activity);
        credentialManager = CredentialManager.create(activity);
    }

    public void signIn(@Nullable Runnable afterSignIn) {
        if (signInInProgress) return;
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) {
            ToastUtil.msg(activity, activity.getString(R.string.auth_not_configured));
            return;
        }

        signInInProgress = true;
        authRepository.requestLoginNonce((nonce, errorMessage) -> {
            if (nonce == null) {
                finishSignInFailure(errorMessage);
                return;
            }

            GetSignInWithGoogleOption googleOption;
            try {
                googleOption = new GetSignInWithGoogleOption.Builder(
                        BuildConfig.GOOGLE_WEB_CLIENT_ID)
                        .setNonce(nonce)
                        .build();
            } catch (IllegalArgumentException exception) {
                finishSignInFailure(exception.getMessage());
                return;
            }

            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build();
            credentialManager.getCredentialAsync(
                    activity,
                    request,
                    new CancellationSignal(),
                    ContextCompat.getMainExecutor(activity),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(@NonNull GetCredentialResponse result) {
                            handleGoogleCredential(result.getCredential(), afterSignIn);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException exception) {
                            finishSignInFailure(exception.getMessage());
                        }
                    }
            );
        });
    }

    private void handleGoogleCredential(Credential credential, @Nullable Runnable afterSignIn) {
        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(
                credential.getType())) {
            finishSignInFailure(activity.getString(R.string.auth_invalid_credential));
            return;
        }

        try {
            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(
                    credential.getData());
            authRepository.completeGoogleLogin(googleCredential.getIdToken(),
                    (success, errorMessage) -> {
                        signInInProgress = false;
                        if (!success) {
                            ToastUtil.msg(activity, activity.getString(
                                    R.string.auth_sign_in_failed,
                                    errorMessage == null ? "" : errorMessage));
                            return;
                        }
                        AuthRepository.User user = authRepository.getCurrentUser();
                        if (user != null) {
                            ToastUtil.msg(activity, activity.getString(
                                    R.string.auth_signed_in_as, user.displayName()));
                        }
                        if (afterSignIn != null) afterSignIn.run();
                    });
        } catch (RuntimeException exception) {
            finishSignInFailure(exception.getMessage());
        }
    }

    private void finishSignInFailure(String errorMessage) {
        signInInProgress = false;
        ToastUtil.msg(activity, activity.getString(
                R.string.auth_sign_in_failed, errorMessage == null ? "" : errorMessage));
    }

    public void signOut(@Nullable Runnable afterSignOut) {
        authRepository.logout((success, errorMessage) -> {
            credentialManager.clearCredentialStateAsync(
                    new ClearCredentialStateRequest(
                            ClearCredentialStateRequest.TYPE_CLEAR_CREDENTIAL_STATE),
                    null,
                    ContextCompat.getMainExecutor(activity),
                    new CredentialManagerCallback<Void, ClearCredentialException>() {
                        @Override
                        public void onResult(Void result) {}

                        @Override
                        public void onError(@NonNull ClearCredentialException exception) {
                            Log.w(TAG, "Unable to clear credential provider state", exception);
                        }
                    }
            );
            ToastUtil.msg(activity, activity.getString(R.string.auth_signed_out));
            if (afterSignOut != null) afterSignOut.run();
        });
    }
}
