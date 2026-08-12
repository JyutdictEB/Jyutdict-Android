package cc.ecisr.jyutdict;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

import cc.ecisr.jyutdict.utils.DiskTextCache;
import cc.ecisr.jyutdict.utils.EnumConst;
import cc.ecisr.jyutdict.utils.HttpUtil;
import cc.ecisr.jyutdict.utils.LocationArticleRepository;
import cc.ecisr.jyutdict.utils.ThemeUtil;
import cc.ecisr.jyutdict.utils.ToastUtil;

public class SettingsActivity extends AppCompatActivity {
    private static final String EXTRA_THEME_CHANGED = "theme_changed";

    Button btnCheckVersion;
    SettingHandler mHandler;
    final HttpUtil versionQuery = new HttpUtil(HttpUtil.GET);

    static SharedPreferences sp;
    static SharedPreferences.Editor editor;
    int v0This, v1This, v2This; // 版本号

    SettingsFragment settingsFragment = new SettingsFragment();

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeUtil.isNightMode(this) ? R.style.DarkSettingsTheme : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("settings", Context.MODE_PRIVATE);
        editor = sp.edit();
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        v0This = Integer.parseInt(getResources().getString(R.string.app_version_0));
        v1This = Integer.parseInt(getResources().getString(R.string.app_version_1));
        v2This = Integer.parseInt(getResources().getString(R.string.app_version_2));

        mHandler = new SettingHandler(getMainLooper(), msg -> {
            switch (msg.what) {
                case EnumConst.CHECKING_VERSION:
                    try {
                        JSONArray version = new JSONObject(
                                msg.obj.toString()
                        ).getJSONArray("app_version");
                        int v0 = version.getInt(0); // 服務器記錄的最新版本號
                        int v1 = version.getInt(1);
                        int v2 = version.getInt(2);
                        if (v0>v0This || v1>v1This || v2>v2This) { // 如果有更新
                            ToastUtil.msg(SettingsActivity.this, getResources().getString(R.string.tips_version_detected));
                            String downloadUrl = String.format(Locale.CHINA,
                                    "https://jyutdict.org/release/%d-%d-%d.apk", v0, v1, v2);
                            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData mClipData = ClipData.newPlainText("泛粤典下载", downloadUrl);
                            if (cm != null) {
                                cm.setPrimaryClip(mClipData);
                            } // else {}
                        } else {
                            ToastUtil.msg(SettingsActivity.this, getResources().getString(R.string.tips_version_checked));
                        }
                    } catch (Exception ignored) {}
                    btnCheckVersion.setEnabled(true);
                    break;
                case HttpUtil.REQUEST_CONTENT_FAIL:
                    ToastUtil.msg(SettingsActivity.this,
                            getString(R.string.error_tips_network, msg.obj.toString()));
                    btnCheckVersion.setEnabled(true);
                    break;
                default:
                    break;
            }
        });


        btnCheckVersion = findViewById(R.id.btn_check_version);
        btnCheckVersion.setText(getResources().getString(R.string.app_version, v0This, v1This, v2This));
        btnCheckVersion.setOnLongClickListener(v -> { // 獲取地名列表
            versionQuery.setUrl("https://jyutdict.org/api/")
                    .setHandler(mHandler, EnumConst.CHECKING_VERSION)
                    .start();
            ToastUtil.msg(SettingsActivity.this, getResources().getString(R.string.tips_version_checking));
            v.setEnabled(false);
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        setResult(settingsFragment.saveSettings());  //  記錄本頁的所有設置
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        versionQuery.cancel();
        if (mHandler != null) mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SwitchPreference switchAdvancedSearch;
        SwitchPreference switchAreaColoring;
        SwitchPreference switchPhraseMeaningDomain;
        EditTextPreference editAreaColoringDarkenRatio;
        ListPreference listThemeMode;
        SwitchPreference switchIpaPresent;
        Preference clearCache;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            switchAdvancedSearch = findPreference("advanced_search");
            switchAreaColoring = findPreference("area_coloring");
            switchPhraseMeaningDomain = findPreference("phrase_meaning_domain");
            editAreaColoringDarkenRatio = findPreference("area_coloring_darken_ratio");
            listThemeMode = findPreference("theme_mode");
            switchIpaPresent = findPreference("ipa_presence");
            clearCache = findPreference("clear_cache");

            if (editAreaColoringDarkenRatio != null) {
                editAreaColoringDarkenRatio.setOnBindEditTextListener(editText ->
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL)
                );
            }
            if (listThemeMode != null) {
                listThemeMode.setOnPreferenceChangeListener((preference, newValue) -> {
                    sp.edit().putString("theme_mode", String.valueOf(newValue)).commit();
                    requireActivity().getIntent().putExtra(EXTRA_THEME_CHANGED, true);
                    requireActivity().getWindow().getDecorView().post(requireActivity()::recreate);
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

        int saveSettings() {
            int settings = 0;
            String newThemeMode = listThemeMode.getValue();
            String oldThemeMode = sp.getString("theme_mode", "follow_system");
            boolean themeChanged = requireActivity().getIntent()
                    .getBooleanExtra(EXTRA_THEME_CHANGED, false);
            settings |= themeChanged || !newThemeMode.equals(oldThemeMode) ? 1 << 1 : 0;

            editor.putBoolean("advanced_search", switchAdvancedSearch.isChecked());
            editor.putBoolean("area_coloring", switchAreaColoring.isChecked());
            editor.putBoolean("phrase_meaning_domain", switchPhraseMeaningDomain.isChecked());
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
    }

    static class SettingHandler extends Handler{
        IHandleMessageProcessor iHandleMessageProcessor;

        public SettingHandler(@NonNull Looper looper, IHandleMessageProcessor processor) {
            super(looper);
            iHandleMessageProcessor = processor;
        }

        @Override
        public void handleMessage(@Nullable Message msg) {
            iHandleMessageProcessor.handleMessage(msg);
        }

        public interface IHandleMessageProcessor {
            void handleMessage(Message msg);
        }
    }
}
