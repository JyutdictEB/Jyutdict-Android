package cc.ecisr.jyutdict;

import static cc.ecisr.jyutdict.utils.EnumConst.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.github.zackratos.ultimatebar.UltimateBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;

import cc.ecisr.jyutdict.struct.FjbHeaderInfo;
import cc.ecisr.jyutdict.struct.GeneralCharacterManager;
import cc.ecisr.jyutdict.utils.JyutpingUtil;
import cc.ecisr.jyutdict.utils.StringUtil;
import cc.ecisr.jyutdict.utils.HttpUtil;
import cc.ecisr.jyutdict.utils.ToastUtil;
import cc.ecisr.jyutdict.widget.EditTextWithClear;
import cc.ecisr.jyutdict.widget.SwitchCustomized;

/**
 * app 的主頁面，包含一個查詢結果的 fragment
 */
public class MainActivity extends AppCompatActivity {
	private static final String TAG = "`MainActivity";
	private static final String URL_API_ROOT = "https://www.jyutdict.org/api/v0.9/";
	
	EditTextWithClear inputEditText;
	Button btnQueryConfirm, btnQueryClear, btnFilterArea, btnColoringJppPartial;
	Spinner spinnerQueryLocation;
	SwitchCustomized switchQueryOpts1, switchQueryOptsRev, switchQueryOptsRegex;
	ResultFragment resultFragment;
	ProgressBar loadingProgressBar;
	Toolbar toolbar;
	LinearLayout lyMain, lyAdvancedSearch;
	
	// 在輸入框輸入的字符串，在按下查詢按鈕時更新
	String inputString;
	
	// 查詢按鈕字體的顏色，僅用於功能測試
	int previousColor;
	
	// 是否已成功獲取到泛粵字表表頭並且完成初始化步驟
	boolean isPrepared = false;
	
	// 指示是否剛初始化完畢，當點擊過查詢按鈕時纔變 False
	boolean isJustInitialized = true;
	
	// 下拉選擇框的 Adapter，存放的是可供查詢的查詢地名
	ArrayAdapter<String> locationsAdapter;
	
	// 用於獲取用戶的設置，與存儲各開關的狀態
	SharedPreferences sp;
	
	// 用於網絡線程與主線程間的通信
	MainHandler mainHandler;
	// 用於向服務器發送請求，與接收回應
	HttpUtil query = new HttpUtil(HttpUtil.GET);
	
	// 指示搜索模式，查通用表字/查通用表音/查泛粵表
	// 在按下查詢按鈕時更新
	// 並根據這個狀態來解析JSON
	int queryingMode = QUERYING_CHARA;
	int queryingModeConfig = 0;
	
	// 夜间模式
	//private static boolean isNightMode = false;
	
	/**
	 * 初始化界面，獲取界面上各物件的視圖
	 */
	void getView() {
		lyMain = findViewById(R.id.whole_main_layout);
		inputEditText = findViewById(R.id.edit_text_input);
		btnQueryConfirm = findViewById(R.id.btn_query);
		btnQueryClear = findViewById(R.id.btn_clear);
		btnFilterArea = findViewById(R.id.btn_filter_area);
		btnColoringJppPartial = findViewById(R.id.btn_coloring_jpp_partial);
		spinnerQueryLocation = findViewById(R.id.locate_spinner);
		lyAdvancedSearch = findViewById(R.id.input_advanced_switch);
		switchQueryOpts1 = findViewById(R.id.switch_select_sheet);
		switchQueryOptsRev = findViewById(R.id.switch_reverse_search);
		switchQueryOptsRegex = findViewById(R.id.switch_use_regex);
		loadingProgressBar = findViewById(R.id.loading_progress);
		toolbar = findViewById(R.id.tool_bar);
		
		setSupportActionBar(toolbar);
		locationsAdapter = new ArrayAdapter<>(this, R.layout.spinner_drop_down_item);
		spinnerQueryLocation.setAdapter(locationsAdapter);
		locationsAdapter.add(getString(R.string.select_drop_down_standard));
		locationsAdapter.add(getString(R.string.select_drop_down_convenience));
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sp = getSharedPreferences("settings", MODE_PRIVATE); // 要讀取夜間模式設置，所以 sp 放前面
		applyLightDarkTheme();
		setContentView(R.layout.activity_main);
		UltimateBar.Companion.with(this)
				.statusDark(true)           // 状态栏灰色模式(Android 6.0+)
				.applyNavigation(true)      // 应用到导航栏
				.navigationDark(false)      // 不导航栏灰色模式(Android 8.0+)
				.create().immersionBar();
		getView();
		if (savedInstanceState == null) {
			resultFragment = new ResultFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.result_fragment, resultFragment).commit();
		} else {
			resultFragment = (ResultFragment) getSupportFragmentManager().getFragment(savedInstanceState, "result_fragment");
		}
		initPermission();

		mainHandler = new MainHandler(Looper.getMainLooper(), msg -> {
			loadingProgressBar.setVisibility(View.GONE);
			btnColoringJppPartial.setEnabled(true);
			switch (msg.what) {
				case INITIALIZE_LOCATIONS: // 初始化表頭
					try {
						JSONArray headerArray = new JSONObject( // TODO 改用其它第三方JSON來解析
								msg.obj.toString()
						).getJSONArray("__valid_options");
						FjbHeaderInfo.load(headerArray);
						setLocationsAdapter();
						if (!isJustInitialized  && inputEditText.getText()!=null && inputEditText.getText().length() != 0) search();
					} catch (JSONException ignored) {}
					break;
				case HttpUtil.REQUEST_CONTENT_SUCCESSFULLY:
					resultFragment.parseJson(msg.obj.toString(), queryingMode | queryingModeConfig);
					break;
				case HttpUtil.REQUEST_CONTENT_FAIL:
					String toastMessage = "0".equals(msg.obj.toString()) ?
							getString(R.string.error_tips_network_out_of_time) :
							getString(R.string.error_tips_network, msg.obj.toString());
					ToastUtil.msg(this, toastMessage);
					break;
				default:
					break;
			}
			btnQueryConfirm.setEnabled(true);
		});
		
		// 查詢按鈕
		btnQueryConfirm.setOnClickListener(v -> {
			isJustInitialized = false;
			if (!isPrepared) {
				ToastUtil.msg(this, "正在獲取地方信息，請稍候");
				query.start(); // 重新向服務器發送請求
				return;
			}
			search();
		});
		btnQueryClear.setOnClickListener(v -> inputEditText.setText(""));
		
		// 監聽焦點在輸入框內的軟鍵盤的確認按鈕
		inputEditText.setOnEditorActionListener((v, actionId, event) -> {
			Log.i(TAG, "onEditorAction: " + actionId);
			if(actionId==EditorInfo.IME_ACTION_SEARCH && MainActivity.this.getCurrentFocus()!=null){
				search();
				((InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE))
						.hideSoftInputFromWindow(MainActivity.this.getCurrentFocus().getWindowToken(),
								InputMethodManager.HIDE_NOT_ALWAYS);
				return true;
			}
			return false;
		});
		
		// 監聽輸入框的輸入 // 僅用於功能測試
		inputEditText.addTextChangedListener(new TextWatcher() { // 用來根據搜字/搜音變按鈕色
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if ("".equals(s.toString())) {
					btnQueryClear.setVisibility(View.GONE);
				} else {
					btnQueryClear.setVisibility(View.VISIBLE);
				}
				
			}
			@Override
			public void afterTextChanged(Editable s) {
				boolean isJpp = JyutpingUtil.isValidJpp(s.toString());

				int presentColor = isJpp ?
						getResources().getColor(R.color.colorSecondary) :
						getResources().getColor(R.color.colorPrimary);
				if (previousColor == presentColor) return;
				ObjectAnimator objectAnimator;
				objectAnimator = ObjectAnimator.ofInt(btnQueryConfirm,"textColor", previousColor, presentColor);
				objectAnimator.setDuration(500);
				objectAnimator.setEvaluator(new ArgbEvaluator());
				objectAnimator.start();
				previousColor = presentColor;
				btnFilterArea.setEnabled(!isJpp);
			}
		});
		previousColor = getResources().getColor(R.color.colorPrimary);
		btnQueryClear.setVisibility(View.GONE);
		
		// 讀取幾個開關之前的狀態
		switchQueryOpts1.setSetCheckedListener(this::setInputEditTextHint);
		switchQueryOptsRev.setSetCheckedListener(this::setInputEditTextHint);
		switchQueryOpts1.setChecked(sp.getBoolean("switch_1_is_checked", false));
		switchQueryOptsRev.setChecked(sp.getBoolean("switch_2_is_checked", false));
		switchQueryOptsRegex.setChecked(sp.getBoolean("switch_3_is_checked", false));
		lyAdvancedSearch.setVisibility(sp.getBoolean("advanced_search", false) ? View.VISIBLE : View.GONE);
		switchQueryOpts1.setOnCheckedChangeListener((buttonView, isChecked) -> setSearchView());
		switchQueryOptsRev.setOnCheckedChangeListener((buttonView, isChecked) -> setSearchView());
		//inputEditText.setOnClickListener(v -> toggleNightTheme());
		GeneralCharacterManager.cityFilter = (HashSet<String>) sp.getStringSet("querying_filter_city", new HashSet<>());

		queryingModeConfig = sp.getInt("querying_mode_config", 0);
		btnColoringJppPartial.setOnClickListener(view -> {
			AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
			dialog.setTitle(R.string.search_coloring_jpp_partial_notice);
			dialog.setNegativeButton(R.string.search_coloring_jpp_partial_inter, (dialogInter, which) -> {
				queryingModeConfig = (queryingModeConfig & ~DISPLAY_CHECKING_IS_INNER);
				resultFragment.refreshResult(queryingModeConfig);
				saveLayoutStatus();
			});
			dialog.setPositiveButton(R.string.search_coloring_jpp_partial_inner, (dialogInner, which) -> {
				queryingModeConfig = (queryingModeConfig & ~DISPLAY_CHECKING_IS_INNER) | DISPLAY_CHECKING_IS_INNER;
				resultFragment.refreshResult(queryingModeConfig);
				saveLayoutStatus();
			});
			dialog.setMultiChoiceItems(
					new String[]{
							getString(R.string.syllable_initial),
							getString(R.string.syllable_final),
							getString(R.string.syllable_tone)},
					new boolean[]{
							(queryingModeConfig&DISPLAY_CHECKING_INI) != 0,
							(queryingModeConfig&DISPLAY_CHECKING_FIN) != 0,
							(queryingModeConfig&DISPLAY_CHECKING_TON) != 0,
					}, (dialog1, which, isChecked) -> {
						switch (which) {
							case 0: queryingModeConfig = (queryingModeConfig&~DISPLAY_CHECKING_INI) | (isChecked?DISPLAY_CHECKING_INI:0); break;
							case 1: queryingModeConfig = (queryingModeConfig&~DISPLAY_CHECKING_FIN) | (isChecked?DISPLAY_CHECKING_FIN:0); break;
							case 2: queryingModeConfig = (queryingModeConfig&~DISPLAY_CHECKING_TON) | (isChecked?DISPLAY_CHECKING_TON:0); break;
						}
			}).create();
			dialog.show();
		});
		btnFilterArea.setOnClickListener(v -> {
			ArrayList<String> cityList = GeneralCharacterManager.cityList;
			HashSet<String> cityFilter = GeneralCharacterManager.cityFilter;
			boolean[] values = new boolean[cityList.size()];
			for (int index=0; index<cityList.size(); index++) {
				values[index] = !cityFilter.contains(cityList.get(index));
			}
			AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
			dialog.setTitle(R.string.search_filtering_area);
			dialog.setPositiveButton(R.string.button_confirm, (dialogPos, which) -> {
				resultFragment.refreshResult();
				saveLayoutStatus();
			});
			dialog.setNeutralButton(R.string.button_all_uncheck, (dialogNeg, which) -> {
				GeneralCharacterManager.cityFilter = new HashSet<>(GeneralCharacterManager.cityList);
				resultFragment.refreshResult();
			});
			dialog.setNegativeButton(R.string.button_all_check, (dialogNeg, which) -> {
				GeneralCharacterManager.cityFilter = new HashSet<>();
				resultFragment.refreshResult();
			});
			dialog.setMultiChoiceItems(cityList.toArray(new String[0]), values, (dialog1, which, isChecked) -> {
				if (isChecked) {
					GeneralCharacterManager.cityFilter.remove(cityList.get(which));
				} else {
					GeneralCharacterManager.cityFilter.add(cityList.get(which));
				}
			});
			dialog.create();
			dialog.show();
		});
		setSearchView();
		
		// 獲取泛粵字表的表頭
		setLocationsAdapter();

		boolean hadCheckedInfoActivity = sp.getBoolean("had_checked_info_activity_2", false);
		if (hadCheckedInfoActivity) {
			if (!isPrepared) { query.start(); }
		} else {
			displayTipsMessageBox();
		}
	}
	
	private void applyLightDarkTheme() {
		boolean isNightMode = sp.getBoolean("night_mode", false);
		if (isNightMode) {
			setTheme(R.style.DarkTheme);
		} else {
			setTheme(R.style.AppTheme);
		}
	}
	
	private void setLocationsAdapter() {
		if (isPrepared) return;
		if (FjbHeaderInfo.isLoaded) {
			locationsAdapter.addAll(FjbHeaderInfo.getCityList());
			spinnerQueryLocation.setSelection(
					sp.getInt("spinner_selected_position", 0));
			isPrepared = true;
			query.setHandler(mainHandler);
		} else {
			query.setUrl(URL_API_ROOT + "sheet?query=&header")
					.setHandler(mainHandler, INITIALIZE_LOCATIONS);
		}
	}
	
	/**
	 * 設置幾個開關的顯示與隱藏
	 */
	private void setSearchView() {
		boolean is1Checked = switchQueryOpts1.isChecked();
		boolean is2Checked = switchQueryOptsRev.isChecked();
		String switch1Text;
		if (is1Checked) {
			switch1Text = getString(R.string.search_jyut_sheet);
			switchQueryOptsRev.setVisibility(View.VISIBLE);
			int spinnerVisibility = is2Checked ? View.GONE : View.VISIBLE;
			spinnerQueryLocation.setVisibility(spinnerVisibility);
			btnFilterArea.setVisibility(View.GONE);
			btnColoringJppPartial.setVisibility(View.GONE);
		} else {
			switch1Text = getString(R.string.search_common_sheet);
			switchQueryOptsRev.setVisibility(View.GONE);
			spinnerQueryLocation.setVisibility(View.GONE);
			btnFilterArea.setVisibility(View.VISIBLE);
			btnColoringJppPartial.setVisibility(View.VISIBLE);
		}
		switchQueryOpts1.setText(switch1Text);
		switchQueryOptsRegex.setEnabled(is1Checked);
	}
	
	/**
	 * 設置標題欄右側的按鈕
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.action_menu, menu);
		return true;
	}
	
	/**
	 * 響應標題欄右側按鈕的按下事件
	 *
	 * REQUESTING_SETTING 表示打開設置界面的 request code
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		Intent intent;
		int itemId = item.getItemId();
		if (itemId == R.id.menu_setting) {
			intent = new Intent(MainActivity.this, SettingsActivity.class);
			startActivityForResult(intent, ACTIVITY_REQUESTING_SETTING);
		} else if (itemId == R.id.menu_info) {
			intent = new Intent(MainActivity.this, InfoActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}

	
	/**
	 * saveLayoutStatus()
	 *
	 * 儲存主頁面幾個開關與下拉欄的的狀態
	 */
	private void saveLayoutStatus() {
		SharedPreferences.Editor editor = sp.edit();
		editor.putBoolean("switch_1_is_checked", switchQueryOpts1.isChecked());
		editor.putBoolean("switch_2_is_checked", switchQueryOptsRev.isChecked());
		editor.putBoolean("switch_3_is_checked", switchQueryOptsRegex.isChecked());
		editor.putInt("spinner_selected_position", spinnerQueryLocation.getSelectedItemPosition());
		editor.putInt("querying_mode_config", queryingModeConfig);
		editor.putStringSet("querying_filter_city", GeneralCharacterManager.cityFilter);
		editor.apply();
	}
	
	
	/**
	 * 更新輸入框中的字符串到 {@code this.inputString} 中
	 *
	 * 在將發起查詢時調用
	 *
	 * @param string 輸入框中的字符串
	 */
	private void setInputString(String string) {
		try {
			inputString = new String(string.getBytes("UTF-8"), "UTF-8")
					.replace('&',' ');
		} catch (UnsupportedEncodingException ignored) {}
	}
	
	/**
	 * 用指定字符串以指定模式發起查詢
	 *
	 * 該方法是對其它類開放的，可以在其它地方調用
	 * 將會改動主界面的開關
	 *
	 * @param chara 包含查詢內容的字符串
	 * @param mode 模式（通用表查字/查音/查泛粵表 等），可選值在 {@code EnumConst} 類定義
	 * @see cc.ecisr.jyutdict.utils.EnumConst
	 */
	void search(String chara, int mode) {
		inputEditText.setText(chara);
		switch (mode & QUERYING_MODE_MASK) { // 爲了方便以後增加不同的查詢模式，這裏 switch 不能化簡
			case QUERYING_CHARA:
			case QUERYING_PRON:
				switchQueryOpts1.setChecked(false);
				break;
			case QUERYING_SHEET:
				switchQueryOpts1.setChecked(true);
				break;
		}
		switchQueryOptsRev.setChecked(false);
		search();
	}
	
	/**
	 * 向服務器發起查詢
	 *
	 * 模式由主界面的開關指定，查詢內容由 {@code this.inputString} 指定
	 * 在等待回應時會禁用查詢按鈕
	 *
	 */
	private void search() {
		if (inputEditText.getText() == null) { return; }
		setInputString(inputEditText.getText().toString()); // 必须放在最前面
		if (isPrepared && "".equals(inputString) && !(switchQueryOpts1.isChecked() && !switchQueryOptsRev.isChecked())) {
			return;
		} // 搜索欄爲空時不檢索
		
		ResultItemAdapter.ResultInfo.clearItem();
		loadingProgressBar.setVisibility(View.VISIBLE);
		btnColoringJppPartial.setEnabled(false);
		StringBuilder url = new StringBuilder(URL_API_ROOT);
		if (switchQueryOpts1.isChecked()) { // 檢索泛粵字表
			queryingMode = QUERYING_SHEET;
			if (inputString.equals("") && !switchQueryOptsRev.isChecked()) {
				url.append("sheet?query=!&limit=10");
			} else {
				if (StringUtil.isAlphaString(inputString) && !switchQueryOptsRev.isChecked()) { // 音
					url.append(String.format("sheet?query=%s&trim", inputString));
					
				} else { // 字
					url.append(String.format("sheet?query=%s&fuzzy", inputString));
				}
				if (switchQueryOptsRev.isChecked()) { // 反查
					url.append("&b");
				}
				int selectedColumn = spinnerQueryLocation.getSelectedItemPosition();
				
				if (selectedColumn >= 2) {
					String col = FjbHeaderInfo.getCityNameByNumber(selectedColumn - 2);
					url.append("&col=").append(col);
				} else if (selectedColumn == 1) {
					if (StringUtil.isAlphaString(inputString)) { url.append("&col=").append("檢"); }
				}
				
				if (switchQueryOptsRegex.isChecked()) {
					url.append("&regex");
				}
			}
		} else { // 檢索通用字表
			if (StringUtil.isAlphaString(inputString)) { // 音
				queryingMode = QUERYING_PRON;
				url.append("detail?pron=").append(inputString);
			} else { // 字
				queryingMode = QUERYING_CHARA;
				url.append("detail?chara=").append(inputString);
			}
		}
		//query.setUrl("http://www.baidu.com/").start();
		query.setUrl(url.toString()).start(); // GET請求服務器API
		btnQueryConfirm.setEnabled(false);
		saveLayoutStatus();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ACTIVITY_REQUESTING_SETTING) {
			boolean isEnableAdvancedSearch = (resultCode&0b1) != 0;
			lyAdvancedSearch.setVisibility(isEnableAdvancedSearch ? View.VISIBLE : View.GONE);
			if (!isEnableAdvancedSearch) switchQueryOptsRegex.setChecked(false);
			
			boolean isToggleNightMode = (resultCode&0b10) != 0;
			if (isToggleNightMode) {
				applyLightDarkTheme();
				recreate();
			} else {
				resultFragment.refreshResult();
			}
		} else if (requestCode == ACTIVITY_CHECKING_INFO_PRIVACY) {
			query.start();
		}
	}

	/****************************************************************************************/
	
	private void setInputEditTextHint() {
		if (switchQueryOpts1.isChecked()) {
			if (switchQueryOptsRev.isChecked()) {
				inputEditText.setHint(R.string.search_tips_backward);
			} else {
				inputEditText.setHint(R.string.search_tips_expend);
			}
		} else {
			inputEditText.setHint(R.string.search_tips);
		}
	}
	
	/**
	 * 首次使用時顯示提示框
	 */
	private void displayTipsMessageBox() {
		new AlertDialog.Builder(this)
				.setTitle("歡迎使用本應用！")
				.setMessage("在使用之前，請務必閱覽本應用之說明。\n\n起碼把紅字看完！\n\n註意：內含隱私聲明，返回此界面則代表同意該聲明。")
				.setPositiveButton("打開「幫助」頁面",
						(dialogInterface, i) -> {
							startActivityForResult(
									new Intent(MainActivity.this, InfoActivity.class),
									ACTIVITY_CHECKING_INFO_PRIVACY
							);
							sp.edit().putBoolean("had_checked_info_activity_2", true).apply();
						})
				.setCancelable(false)
				.show();
	}
	
	/**
	 * 申請網絡等權限
	 *
	 * 在初始化 app 時調用
	 */
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
			ToastUtil.msg(this, getString(R.string.permission_requesting));
			initPermission();
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
	
	static class MainHandler extends Handler{
		//WeakReference<MainActivity> mActivity;
		IHandleMessageProcessor iHandleMessageProcessor;
		
		public MainHandler(@NonNull Looper looper, IHandleMessageProcessor processor) {
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
	
	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (resultFragment != null) {
			getSupportFragmentManager().putFragment(outState, "result_fragment", resultFragment);
		}
	}
}
