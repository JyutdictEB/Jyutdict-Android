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
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

import cc.ecisr.jyutdict.utils.EnumConst;
import cc.ecisr.jyutdict.utils.HttpUtil;
import cc.ecisr.jyutdict.utils.ToastUtil;

public class SettingsActivity extends AppCompatActivity {
	Button btnCheckVersion;
	SettingHandler mHandler;
	
	static SharedPreferences sp;
	static SharedPreferences.Editor editor;
	int v0This, v1This, v2This; // 版本号
	
	SettingsFragment settingsFragment = new SettingsFragment();
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.settings, settingsFragment)
				.commit();
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		
		sp = getSharedPreferences("settings", Context.MODE_PRIVATE);
		editor = sp.edit();
		
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
									"http://jyutdict.org/release/%d-%d-%d.apk", v0, v1, v2);
							ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
							ClipData mClipData = ClipData.newPlainText("泛粤典下载", downloadUrl);
							if (cm != null) {
								cm.setPrimaryClip(mClipData);
							} // else {}
						} else {
							ToastUtil.msg(SettingsActivity.this, getResources().getString(R.string.tips_version_checked));
						}
					} catch (Exception ignored) {}
					break;
				default:
					break;
			}
		});
		
		
		btnCheckVersion = findViewById(R.id.btn_check_version);
		btnCheckVersion.setText(getResources().getString(R.string.app_version, v0This, v1This, v2This));
		btnCheckVersion.setOnLongClickListener(v -> { // 獲取地名列表
			new HttpUtil(HttpUtil.GET)
					.setUrl("http://jyutdict.org/api/")
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
	protected void onDestroy() {
		super.onDestroy();
	}
	
	public static class SettingsFragment extends PreferenceFragmentCompat {
		SwitchPreference switchAdvancedSearch;
		SwitchPreference switchAreaColoring;
		SwitchPreference switchPhraseMeaningDomain;
		EditTextPreference editAreaColoringDarkenRatio;
		SwitchPreference switchNightMode;
		SwitchPreference switchIpaPresent;
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.root_preferences, rootKey);
			switchAdvancedSearch = findPreference("advanced_search");
			switchAreaColoring = findPreference("area_coloring");
			switchPhraseMeaningDomain = findPreference("phrase_meaning_domain");
			editAreaColoringDarkenRatio = findPreference("area_coloring_darken_ratio");
			switchNightMode = findPreference("night_mode");
			switchIpaPresent = findPreference("ipa_presence");
			
			if (editAreaColoringDarkenRatio != null) {
				editAreaColoringDarkenRatio.setOnBindEditTextListener(editText ->
						editText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL)
				);
			}
		}
		
		int saveSettings() {
			int settings = 0;
			settings |= switchNightMode.isChecked()^sp.getBoolean("night_mode", false) ? 1 << 1 : 0;
			
			editor.putBoolean("advanced_search", switchAdvancedSearch.isChecked());
			editor.putBoolean("area_coloring", switchAreaColoring.isChecked());
			editor.putBoolean("phrase_meaning_domain", switchPhraseMeaningDomain.isChecked());
			editor.putBoolean("night_mode", switchNightMode.isChecked());
			editor.putBoolean("ipa_presence", switchIpaPresent.isChecked());
			editor.putFloat("area_coloring_darken_ratio", Float.parseFloat(editAreaColoringDarkenRatio.getText()));
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