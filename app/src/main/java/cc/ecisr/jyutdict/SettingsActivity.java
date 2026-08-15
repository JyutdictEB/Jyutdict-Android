package cc.ecisr.jyutdict;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.ecisr.jyutdict.auth.AuthRepository;
import cc.ecisr.jyutdict.auth.GoogleSignInCoordinator;
import cc.ecisr.jyutdict.databinding.ActivitySettingsBinding;
import cc.ecisr.jyutdict.databinding.DialogVersionInfoBinding;
import cc.ecisr.jyutdict.utils.DiskTextCache;
import cc.ecisr.jyutdict.utils.HttpUtil;
import cc.ecisr.jyutdict.utils.LocationArticleRepository;
import cc.ecisr.jyutdict.utils.ImmersiveBarUtil;
import cc.ecisr.jyutdict.utils.MotionUtil;
import cc.ecisr.jyutdict.utils.ThemeUtil;
import cc.ecisr.jyutdict.utils.ToastUtil;

public class SettingsActivity extends AppCompatActivity {
    private static final String EXTRA_THEME_CHANGED = "theme_changed";
    private static final String EXTRA_THEME_TRANSITION = "theme_transition";
    private static final String SETTINGS_FRAGMENT_TAG = "settings_fragment";
    private static final Pattern SEMANTIC_VERSION_PATTERN = Pattern.compile(
            "^(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+].*)?$");

    DialogVersionInfoBinding versionDialogBinding;
    AlertDialog versionDialog;
    final HttpUtil versionQuery = new HttpUtil();
    AuthRepository authRepository;
    GoogleSignInCoordinator googleSignIn;

    String versionNameThis;
    int[] currentVersion;

    SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeUtil.isNightMode(this) ? R.style.DarkSettingsTheme : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        ActivitySettingsBinding binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (savedInstanceState != null
                && getIntent().getBooleanExtra(EXTRA_THEME_TRANSITION, false)) {
            android.view.View content = findViewById(android.R.id.content);
            content.setAlpha(0f);
            content.post(() -> MotionUtil.fadeIn(content));
            getIntent().removeExtra(EXTRA_THEME_TRANSITION);
        }
        ImmersiveBarUtil.setImmersiveBar(this, false, !ThemeUtil.isNightMode(this));
        setSupportActionBar(binding.toolbar);
        ImmersiveBarUtil.applyToolbarInsets(binding.toolbar);
        authRepository = AuthRepository.getInstance(this);
        googleSignIn = new GoogleSignInCoordinator(this);
        androidx.fragment.app.Fragment restoredFragment = getSupportFragmentManager()
                .findFragmentByTag(SETTINGS_FRAGMENT_TAG);
        if (restoredFragment instanceof SettingsFragment) {
            settingsFragment = (SettingsFragment) restoredFragment;
        } else {
            settingsFragment = new SettingsFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, settingsFragment, SETTINGS_FRAGMENT_TAG)
                    .commitNow();
        }
        authRepository.initialize((success, errorMessage) -> {
            if (settingsFragment.isAdded()) settingsFragment.refreshAccountPreference();
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setResult(settingsFragment.saveSettings());
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        versionNameThis = getInstalledVersionName();
        currentVersion = parseSemanticVersion(versionNameThis);


        binding.btnCheckVersion.setText(getString(R.string.app_version, versionNameThis));
        binding.btnCheckVersion.setOnClickListener(v -> showVersionDialog());
        binding.btnAbout.setOnClickListener(v ->
                startActivity(new Intent(this, InfoActivity.class)));
    }

    private void showVersionDialog() {
        if (versionDialog != null && versionDialog.isShowing()) return;
        versionDialogBinding = DialogVersionInfoBinding.inflate(getLayoutInflater());
        versionDialogBinding.versionCurrent.setText(getString(
                R.string.version_current_value, versionNameThis));
        versionDialogBinding.versionCheckAction.setOnClickListener(view -> checkVersion());
        versionDialogBinding.versionDownloadBaidu.setOnClickListener(view ->
                openUrl("https://pan.baidu.com/s/1r7mo35tEwZ0zAjQHIacf8w"));
        versionDialogBinding.versionDownloadTianyi.setOnClickListener(view ->
                openUrl("https://cloud.189.cn/t/yA7FVnUzQZj2"));
        versionDialogBinding.versionDownloadGithub.setOnClickListener(view ->
                openUrl("https://github.com/EcRal5t/Jyutdict-Android/releases"));

        versionDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.version_dialog_title)
                .setView(versionDialogBinding.getRoot())
                .setNegativeButton(R.string.location_close, null)
                .create();
        versionDialog.setOnDismissListener(dialog -> {
            versionDialog = null;
            versionDialogBinding = null;
        });
        versionDialog.show();
    }

    private void showAccountDialog() {
        AuthRepository.User user = authRepository.getCurrentUser();
        if (user == null) {
            googleSignIn.signIn(settingsFragment::refreshAccountPreference);
            return;
        }

        String message = user.email + "\n" + getString(R.string.auth_role, user.role);
        new AlertDialog.Builder(this)
                .setTitle(user.displayName())
                .setMessage(message)
                .setPositiveButton(R.string.button_confirm, null)
                .setNegativeButton(R.string.auth_sign_out, (dialog, which) ->
                        googleSignIn.signOut(settingsFragment::refreshAccountPreference))
                .show();
    }

    private void checkVersion() {
        setVersionStatus(getString(R.string.version_status_checking));
        setVersionCheckEnabled(false);
        versionQuery.enqueue("https://jyutdict.org/api/", new HttpUtil.Callback() {
            @Override
            public void onSuccess(String body) {
                applyVersionResponse(body);
            }

            @Override
            public void onFailure(HttpUtil.RequestError error) {
                setVersionStatus(getString(R.string.version_status_failed));
                setVersionCheckEnabled(true);
            }
        });
    }

    private void applyVersionResponse(String raw) {
        try {
            JSONObject response = new JSONObject(raw);
            JSONArray version = response.optJSONArray("app_version");
            if (version == null) version = response.getJSONArray("version");
            int major = version.getInt(0), minor = version.getInt(1), patch = version.getInt(2);
            setVersionStatus(isNewerVersion(major, minor, patch)
                    ? getString(R.string.version_status_update_available, major, minor, patch)
                    : getString(R.string.version_status_current));
        } catch (Exception ignored) {
            setVersionStatus(getString(R.string.version_status_invalid));
        }
        setVersionCheckEnabled(true);
    }

    private boolean isNewerVersion(int major, int minor, int patch) {
        if (major != currentVersion[0]) return major > currentVersion[0];
        if (minor != currentVersion[1]) return minor > currentVersion[1];
        return patch > currentVersion[2];
    }

    private String getInstalledVersionName() {
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;
            return versionName == null ? "0.0.0" : versionName;
        } catch (PackageManager.NameNotFoundException exception) {
            return "0.0.0";
        }
    }

    private int[] parseSemanticVersion(String versionName) {
        Matcher matcher = SEMANTIC_VERSION_PATTERN.matcher(versionName);
        if (!matcher.matches()) return new int[]{0, 0, 0};
        return new int[]{
                Integer.parseInt(Objects.requireNonNull(matcher.group(1))),
                Integer.parseInt(Objects.requireNonNull(matcher.group(2))),
                Integer.parseInt(Objects.requireNonNull(matcher.group(3)))
        };
    }

    private void setVersionStatus(String status) {
        if (versionDialogBinding != null) {
            MotionUtil.setText(versionDialogBinding.versionCheckStatus, status);
        }
    }

    private void setVersionCheckEnabled(boolean enabled) {
        if (versionDialogBinding != null) {
            versionDialogBinding.versionCheckAction.setEnabled(enabled);
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception exception) {
            ToastUtil.msg(this, getString(R.string.version_link_unavailable));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        versionQuery.cancel();
        if (versionDialog != null) versionDialog.dismiss();
        super.onDestroy();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SwitchPreferenceCompat switchAdvancedSearch;
        SwitchPreferenceCompat switchAreaColoring;
        SwitchPreferenceCompat switchPhraseMeaningDomain;
        EditTextPreference editAreaColoringDarkenRatio;
        SwitchPreferenceCompat switchLongPressLocationCard;
        ListPreference listThemeMode;
        SwitchPreferenceCompat switchIpaPresent;
        Preference clearCache;
        Preference account;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            switchAdvancedSearch = findPreference("advanced_search");
            switchAreaColoring = findPreference("area_coloring");
            switchPhraseMeaningDomain = findPreference("phrase_meaning_domain");
            switchLongPressLocationCard = findPreference("long_press_location_card");
            editAreaColoringDarkenRatio = findPreference("area_coloring_darken_ratio");
            listThemeMode = findPreference("theme_mode");
            switchIpaPresent = findPreference("ipa_presence");
            clearCache = findPreference("clear_cache");
            account = findPreference("account");

            if (account != null) {
                account.setOnPreferenceClickListener(preference -> {
                    ((SettingsActivity) requireActivity()).showAccountDialog();
                    return true;
                });
                refreshAccountPreference();
            }

            if (editAreaColoringDarkenRatio != null) {
                editAreaColoringDarkenRatio.setOnBindEditTextListener(editText ->
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL)
                );
            }
            if (listThemeMode != null) {
                listThemeMode.setOnPreferenceChangeListener((preference, newValue) -> {
                    preferences().edit()
                            .putString("theme_mode", String.valueOf(newValue))
                            .apply();
                    requireActivity().getIntent().putExtra(EXTRA_THEME_CHANGED, true);
                    requireActivity().getIntent().putExtra(EXTRA_THEME_TRANSITION, true);
                    ViewGroup content = requireActivity()
                            .findViewById(android.R.id.content);
                    int surface = ContextCompat.getColor(requireContext(),
                            ThemeUtil.isNightMode(requireContext())
                                    ? R.color.colorBackgroundDark
                                    : R.color.colorBackground);
                    MotionUtil.fadeThroughColor(
                            content, surface, requireActivity()::recreate);
                    return true;
                });
            }
            if (clearCache != null) {
                clearCache.setOnPreferenceClickListener(preference -> {
                    int deleted = DiskTextCache.clear(requireContext());
                    LocationArticleRepository.clearMemoryCache();
                    ToastUtil.msg(
                            requireContext(),
                            getString(R.string.cache_cleared, deleted)
                    );
                    return true;
                });
            }
        }

        void refreshAccountPreference() {
            if (account == null || !isAdded()) return;
            AuthRepository.User user = AuthRepository.getInstance(requireContext())
                    .getCurrentUser();
            if (user == null) {
                account.setTitle(R.string.auth_sign_in);
                account.setSummary(R.string.auth_account_summary);
            } else {
                account.setTitle(getString(R.string.auth_signed_in_as, user.displayName()));
                account.setSummary(R.string.auth_account_summary);
            }
        }

        int saveSettings() {
            SharedPreferences preferences = preferences();
            SharedPreferences.Editor editor = preferences.edit();
            int settings = 0;
            String newThemeMode = listThemeMode.getValue();
            String oldThemeMode = preferences.getString("theme_mode", "follow_system");
            boolean themeChanged = requireActivity().getIntent()
                    .getBooleanExtra(EXTRA_THEME_CHANGED, false);
            settings |= themeChanged || !newThemeMode.equals(oldThemeMode) ? 1 << 1 : 0;

            editor.putBoolean("advanced_search", switchAdvancedSearch.isChecked());
            editor.putBoolean("area_coloring", switchAreaColoring.isChecked());
            editor.putBoolean("phrase_meaning_domain", switchPhraseMeaningDomain.isChecked());
            editor.putBoolean("long_press_location_card", switchLongPressLocationCard == null || switchLongPressLocationCard.isChecked());
            editor.putString("theme_mode", newThemeMode);
            editor.putBoolean("ipa_presence", switchIpaPresent.isChecked());
            float darkenRatio = 0.92f;
            try {
                darkenRatio = Float.parseFloat(editAreaColoringDarkenRatio.getText());
            } catch (NumberFormatException ignored) {}
            darkenRatio = Math.max(0.2f, Math.min(2.0f, darkenRatio));
            editor.putFloat("area_coloring_darken_ratio", darkenRatio);
            editor.apply();
            settings |= switchAdvancedSearch.isChecked() ? 1 : 0;

            return settings;
        }

        private SharedPreferences preferences() {
            return requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        }
    }

}
