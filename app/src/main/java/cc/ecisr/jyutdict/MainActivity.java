package cc.ecisr.jyutdict;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;

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
	
	String inputString;
	int previousColor;
	ArrayAdapter<String> locationsAdapter;
	
	Handler mainHandler;
	HttpUtil query = new HttpUtil(HttpUtil.GET); // 封裝的HttpUtil，用於請求API
	
	int queryObjectWhat = EnumConst.QUERYING_CHARA; // 指示蒐索對象，根據這個狀態來解析JSON
	
	void getView() {
		inputEditText = findViewById(R.id.edit_text_input);
		btnQueryConfirm = findViewById(R.id.btn_query);
		spinnerQueryLocation = findViewById(R.id.locate_spinner);
		switchQueryOpts1 = findViewById(R.id.switch1);
		switchQueryOpts2 = findViewById(R.id.switch2);
		loadingProgressBar = findViewById(R.id.loading_progress);
		
		resultFragment = new ResultFragment();
		locationsAdapter = new ArrayAdapter<>(this, R.layout.spinner_drop_down_item);
		spinnerQueryLocation.setAdapter(locationsAdapter);
		locationsAdapter.add("字/音");
		getSupportFragmentManager().beginTransaction().replace(R.id.result_fragment, resultFragment).commit();
	}
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		getView();
		
		mainHandler = new Handler() {
			@Override
			public void handleMessage(@NonNull Message msg) {
				switch (msg.what) {
					case EnumConst.INITIALIZE_LOCATIONS:
						try {
							JSONObject jsonObject = new JSONObject(
									msg.obj.toString()
							).getJSONObject("__valid_options");
							Iterator<String> keysIterator = jsonObject.keys(); // 獲取地名鍵
							while (keysIterator.hasNext()) {
								String key = keysIterator.next();
								switch (key) {
									case "繁":
									case "綜":
									case "總點數":
									case "釋義":
										break;
									default:
										locationsAdapter.add(key); // 下拉列表新增項
										break;
								}
							}
						} catch (JSONException ignored) {}
						break;
					case HttpUtil.REQUEST_CONTENT_SUCCESSFULLY:
						resultFragment.parseJson(msg.obj.toString(), queryObjectWhat); // 解析json
						loadingProgressBar.setVisibility(View.INVISIBLE);
						break;
					case HttpUtil.REQUEST_CONTENT_FAIL:
						ToastUtil.msg(MainActivity.this, "網路錯誤或伺服器錯誤，錯誤碼：" + msg.obj.toString());
						break;
					default:
						break;
				}
				btnQueryConfirm.setEnabled(true);
			}
		};
		
		// 查詢按鈕
		btnQueryConfirm.setOnClickListener(v -> {
			search();
		});
		
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
		
		// 監聽輸入框的輸入
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
		
		switchQueryOpts1.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (isChecked) {
				switchQueryOpts1.setText("泛粵字表");
				switchQueryOpts2.setVisibility(View.VISIBLE);
				spinnerQueryLocation.setVisibility(View.VISIBLE);
			} else {
				switchQueryOpts1.setText("通用字表");
				switchQueryOpts2.setVisibility(View.INVISIBLE);
				switchQueryOpts2.setChecked(false);
				spinnerQueryLocation.setVisibility(View.INVISIBLE);
			}
		});
		switchQueryOpts2.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (isChecked) {
				spinnerQueryLocation.setVisibility(View.INVISIBLE);
			} else {
				spinnerQueryLocation.setVisibility(View.VISIBLE);
			}
		});
		
		query.setUrl("http://jyutdict.org/api/v0.9/sheet?query="); // 獲取地名列表
		query.setHandler(mainHandler, EnumConst.INITIALIZE_LOCATIONS);
		query.start();
	}
	
	
	private void setInputString(String string) {
		try {
			inputString = new String(string.getBytes("UTF-8"), "UTF-8");
		} catch (UnsupportedEncodingException e) {e.printStackTrace();}
	}
	
	private void search() {
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
			String col = spinnerQueryLocation.getSelectedItem().toString();
			if (!"字/音".equals(col)) { // 指定地點
				url.append("&col=").append(col);
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
		Log.i(TAG, url.toString());
		
		query.setUrl(url.toString()); // GET請求API
		query.setHandler(mainHandler);
		query.start();
		btnQueryConfirm.setEnabled(false);
	}
}
