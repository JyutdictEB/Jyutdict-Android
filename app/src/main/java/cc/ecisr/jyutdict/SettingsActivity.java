package cc.ecisr.jyutdict;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
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
	Handler mHandler;
	
	static SharedPreferences sp;
	static SharedPreferences.Editor editor;
	int v0This, v1This, v2This; // 版本号
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.settings, new SettingsFragment())
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
		
		mHandler = new Handler() {
			@Override
			public void handleMessage(@NonNull Message msg) {
				switch (msg.what) {
					case EnumConst.CHECKING_VERSION:
						try {
							JSONArray version = new JSONObject(
								msg.obj.toString()
							).getJSONArray("app_version");
							int v0 = version.getInt(0);
							int v1 = version.getInt(1);
							int v2 = version.getInt(2);
							if (v0>v0This || v1>v1This || v2>v2This) {
								ToastUtil.msg(SettingsActivity.this, "有新版本");
								String downloadUrl = String.format(Locale.CHINA,
										"http://jyutdict.org/release/%d-%d-%d.apk", v0, v1, v2);
								ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
								ClipData mClipData = ClipData.newPlainText("泛粤典下载", downloadUrl);
								if (cm != null) {
									cm.setPrimaryClip(mClipData);
								}
							} else {
								ToastUtil.msg(SettingsActivity.this, "已为最新");
							}
						} catch (Exception ignored) {}
						break;
					default:
						break;
				}
			}
		};
		
		
		
		
		btnCheckVersion = findViewById(R.id.btn_check_version);
		btnCheckVersion.setText(getResources().getString(R.string.app_version, v0This, v1This, v2This));
		btnCheckVersion.setOnLongClickListener(v -> {
			HttpUtil query = new HttpUtil(HttpUtil.GET);
			query.setUrl("http://jyutdict.org/api/"); // 獲取地名列表
			query.setHandler(mHandler, EnumConst.CHECKING_VERSION);
			query.start();
			ToastUtil.msg(SettingsActivity.this, "正在检测更新版本");
			btnCheckVersion.setEnabled(false);
			return true;
		});
	}
	
	@Override
	protected void onDestroy() {
		editor.apply();
		super.onDestroy();
	}
	
	public static class SettingsFragment extends PreferenceFragmentCompat {
		private static final String TAG = "`SettingsFragment";
		SwitchPreference switchAreaColoring;
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.root_preferences, rootKey);
			switchAreaColoring = findPreference("area_coloring");
			if (switchAreaColoring!=null) {
				switchAreaColoring.setOnPreferenceChangeListener((preference, newValue) -> {
					Log.i(TAG, preference.toString() + "onPreferenceChange: " + newValue.toString());
					if ("true".equals(newValue.toString())) {
						SettingsActivity.editor.putBoolean("area_coloring", true);
					} else {
						SettingsActivity.editor.putBoolean("area_coloring", false);
					}
					return true;
				});
			}
		}
		
	}
}