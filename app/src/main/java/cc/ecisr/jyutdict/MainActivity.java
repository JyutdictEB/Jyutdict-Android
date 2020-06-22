package cc.ecisr.jyutdict;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;

import com.github.zackratos.ultimatebar.UltimateBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import cc.ecisr.jyutdict.struct.HeaderInfo;
import cc.ecisr.jyutdict.utils.StringUtil;
import cc.ecisr.jyutdict.utils.EnumConst;
import cc.ecisr.jyutdict.utils.HttpUtil;
import cc.ecisr.jyutdict.utils.ToastUtil;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "`MainActivity";
	private static final String URL_API_ROOT = "https://www.jyutdict.org/api/v0.9/";
	
	EditText inputEditText;
	Button btnQueryConfirm;
	Spinner spinnerQueryLocation;
	Switch switchQueryOpts1, switchQueryOpts2;
	ResultFragment resultFragment;
	ProgressBar loadingProgressBar;
	Toolbar toolbar;
	LinearLayout lyMain;
	
	String inputString;
	int previousColor;
	boolean isPrepared = false;
	ArrayAdapter<String> locationsAdapter;
	HeaderInfo headerInfo; // 他是静态的
	SharedPreferences sp;
	
	Handler mainHandler;
	HttpUtil query = new HttpUtil(HttpUtil.GET); // 封裝的HttpUtil，用於請求API
	
	int queryObjectWhat = EnumConst.QUERYING_CHARA; // 指示蒐索對象，根據這個狀態來解析JSON
	
	void getView() {
		lyMain = findViewById(R.id.whole_main_layout);
		inputEditText = findViewById(R.id.edit_text_input);
		btnQueryConfirm = findViewById(R.id.btn_query);
		spinnerQueryLocation = findViewById(R.id.locate_spinner);
		switchQueryOpts1 = findViewById(R.id.switch1);
		switchQueryOpts2 = findViewById(R.id.switch2);
		loadingProgressBar = findViewById(R.id.loading_progress);
		toolbar = findViewById(R.id.tool_bar);
		
		
//		lyMain.setBackgroundColor(getResources().getColor(R.color.colorSolarized));
		setSupportActionBar(toolbar);
		Log.i(TAG, "onCreate() -> getFragment()");
		locationsAdapter = new ArrayAdapter<>(this, R.layout.spinner_drop_down_item);
		spinnerQueryLocation.setAdapter(locationsAdapter);
		locationsAdapter.add("字/音");
		locationsAdapter.add("俗字");
		
	}
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		UltimateBar.Companion.with(this)
				.statusDark(true)                  // 状态栏灰色模式(Android 6.0+)
				.applyNavigation(true)              // 应用到导航栏
				.navigationDark(false)              // 不导航栏灰色模式(Android 8.0+)
				.create().immersionBar();
		getView();
		if (savedInstanceState == null) { // 沒用orz
			resultFragment = new ResultFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.result_fragment, resultFragment).commit();
		}
		initPermission();
		
		mainHandler = new Handler() {
			@Override
			public void handleMessage(@NonNull Message msg) {
				switch (msg.what) {
					case EnumConst.INITIALIZE_LOCATIONS:
						try {
							JSONArray headerArray = new JSONObject(
									msg.obj.toString()
							).getJSONArray("__valid_options");
							headerInfo = new HeaderInfo(headerArray);
							
							String[] cityList = HeaderInfo.getCityList();
							for (String cityName : cityList) {
								locationsAdapter.add(cityName);
							}
							isPrepared = true;
							if (inputEditText.getText().length() != 0) search();
						} catch (JSONException ignored) {}
						break;
					case HttpUtil.REQUEST_CONTENT_SUCCESSFULLY:
						resultFragment.parseJson(msg.obj.toString(), queryObjectWhat); // 解析json
						loadingProgressBar.setVisibility(View.GONE);
						break;
					case HttpUtil.REQUEST_CONTENT_FAIL:
						ToastUtil.msg(MainActivity.this, getResources().getString(R.string.error_tips_network, msg.obj.toString()));
						loadingProgressBar.setVisibility(View.GONE);
						break;
					default:
						break;
				}
				btnQueryConfirm.setEnabled(true);
			}
		};
		
		// 查詢按鈕
		btnQueryConfirm.setOnClickListener(v -> search());
		
		// 監聽輸入框的確認按鈕
		inputEditText.setOnEditorActionListener((v, actionId, event) -> {
			Log.i(TAG, "onEditorAction: " + actionId);
			if(actionId == EditorInfo.IME_ACTION_SEARCH){
				search();
				try { // 關閉軟鍵盤
					((InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE))
							.hideSoftInputFromWindow(MainActivity.this.getCurrentFocus().getWindowToken(),
									InputMethodManager.HIDE_NOT_ALWAYS);
					return true;
				} catch (NullPointerException ignored) {}
			}
			return false;
		});
		
		// 監聽輸入框的輸入 // 僅用於功能測試
		inputEditText.addTextChangedListener(new TextWatcher() { // 用來根據蒐字/蒐音變按鈕色
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				int presentColor = (StringUtil.isAlphaString(s.toString())) ?
						getResources().getColor(R.color.colorSecondary) :
						getResources().getColor(R.color.colorPrimary);
				ObjectAnimator objectAnimator;
				objectAnimator = ObjectAnimator.ofInt(btnQueryConfirm,"textColor", previousColor, presentColor);
				objectAnimator.setDuration(500);
				objectAnimator.setEvaluator(new ArgbEvaluator());
				objectAnimator.start();
				previousColor = presentColor;
			}
		});
		previousColor = getResources().getColor(R.color.colorPrimary);
		
		sp = getSharedPreferences("searching", MODE_PRIVATE);
		switchQueryOpts1.setChecked(sp.getBoolean("switch_1_is_checked", false));
		switchQueryOpts2.setChecked(sp.getBoolean("switch_2_is_checked", false));
		spinnerQueryLocation.setSelection(sp.getInt("spinner_selected_position", 0));
		switchQueryOpts1.setOnCheckedChangeListener((buttonView, isChecked) -> {
			setSearchView();
			sp.edit().putBoolean("switch_1_is_checked", isChecked).apply();
		});
		switchQueryOpts2.setOnCheckedChangeListener((buttonView, isChecked) -> {
			setSearchView();
			sp.edit().putBoolean("switch_2_is_checked", isChecked).apply();
		});
		spinnerQueryLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				sp.edit().putInt("spinner_selected_position", position).apply();
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			
			}
		});
		setSearchView();
		
		query.setUrl("http://jyutdict.org/api/v0.9/sheet?query=&header") // 獲取地名列表
			.setHandler(mainHandler, EnumConst.INITIALIZE_LOCATIONS)
			.start();
	}
	
	private void setSearchView() {
		boolean is1Checked = switchQueryOpts1.isChecked();
		boolean is2Checked = switchQueryOpts2.isChecked();
		String switch1Text;
		if (is1Checked) {
			switch1Text = getResources().getString(R.string.search_jyut_sheet);
			switchQueryOpts2.setVisibility(View.VISIBLE);
			int spinnerVisibility = is2Checked ? View.INVISIBLE : View.VISIBLE;
			spinnerQueryLocation.setVisibility(spinnerVisibility);
		} else {
			switch1Text = getResources().getString(R.string.search_common_sheet);
			switchQueryOpts2.setVisibility(View.INVISIBLE);
			spinnerQueryLocation.setVisibility(View.INVISIBLE);
		}
		switchQueryOpts1.setText(switch1Text);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.action_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.option_setting:
				Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
				startActivity(intent);
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		Log.i(TAG, "onRestoreInstanceState: ");
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
		Log.i(TAG, "onRestoreInstanceState: ");
		super.onRestoreInstanceState(savedInstanceState, persistentState);
	}
	
	
	private void setInputString(String string) {
		try {
			inputString = new String(string.getBytes("UTF-8"), "UTF-8");
		} catch (UnsupportedEncodingException ignored) {}
	}
	
	void search(String chara, int mode) {
		inputEditText.setText(chara);
		switch (mode) {
			case EnumConst.QUERYING_CHARA:
			case EnumConst.QUERYING_PRON:
				switchQueryOpts1.setChecked(false);
				break;
			case EnumConst.QUERYING_SHEET:
				switchQueryOpts1.setChecked(true);
				break;
		}
		switchQueryOpts2.setChecked(false);
		search();
	}
	
	private void search() {
		if (!isPrepared) {
			ToastUtil.msg(this, "正在獲取地方信息，請稍候");
			query.start(); // 重新發送請求
			return;
		}
		setInputString(inputEditText.getText().toString()); // 必须放在最前面
		ResultItemAdapter.ResultInfo.clearItem(); // 暴力清除列表，随後再刷新
		loadingProgressBar.setVisibility(View.VISIBLE);
		StringBuilder url = new StringBuilder(URL_API_ROOT);
		if (switchQueryOpts1.isChecked()) { // 檢索泛粵字表
			queryObjectWhat = EnumConst.QUERYING_SHEET;
			if (StringUtil.isAlphaString(inputString)) { // 音
				url.append(String.format("sheet?query=%s&trim", inputString));
			} else { // 字
				url.append(String.format("sheet?query=%s&fuzzy", inputString));
			}
			if (switchQueryOpts2.isChecked()) { // 反查
				url.append("&b");
			}
			int colNum = spinnerQueryLocation.getSelectedItemPosition() - 2; // [0]「字/音」 [1]「俗字」
			if (colNum >= 0) { // 指定地點
				String col = HeaderInfo.getCityNameByNumber(colNum);
				url.append("&col=").append(col);
			} else if (colNum == -1) {
				url.append("&col=").append(HeaderInfo.COLUMN_NAME_CONVENTIONAL);
			}
		} else { // 檢索通用字表
			if (StringUtil.isAlphaString(inputString)) { // 音
				queryObjectWhat = EnumConst.QUERYING_PRON;
				url.append("detail?pron=").append(inputString);
			} else { // 字
				queryObjectWhat = EnumConst.QUERYING_CHARA;
				url.append("detail?chara=").append(inputString);
			}
		}
		
		query.setUrl(url.toString()) // GET請求API
			.setHandler(mainHandler)
			.start();
		btnQueryConfirm.setEnabled(false);
	}
	
	private void initPermission() {
		String[] permissions = {
				Manifest.permission.INTERNET,
		};
		ArrayList<String> toApplyList = new ArrayList<>();
		
		for (String perm : permissions) {
			ContextCompat.checkSelfPermission(this, perm);
			if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
				toApplyList.add(perm);
			}
		}
		String[] tmpList = new String[toApplyList.size()];
		if (!toApplyList.isEmpty()) {
			ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (grantResults.length == 0 || grantResults[0]!=PackageManager.PERMISSION_GRANTED) {
			ToastUtil.msg(this, getResources().getString(R.string.permission_requesting));
			initPermission();
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
}
