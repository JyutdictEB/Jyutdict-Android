package cc.ecisr.jyutdict;

import static cc.ecisr.jyutdict.utils.EnumConst.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import cc.ecisr.jyutdict.struct.FjbHeaderInfo;
import cc.ecisr.jyutdict.struct.GeneralCharacterManager;
import cc.ecisr.jyutdict.struct.LocationInfo;
import cc.ecisr.jyutdict.utils.ApiUrlBuilder;
import cc.ecisr.jyutdict.utils.ImmersiveBarUtil;
import cc.ecisr.jyutdict.utils.JyutpingUtil;
import cc.ecisr.jyutdict.utils.StringUtil;
import cc.ecisr.jyutdict.utils.HttpUtil;
import cc.ecisr.jyutdict.utils.ThemeUtil;
import cc.ecisr.jyutdict.utils.ToastUtil;
import androidx.appcompat.widget.AppCompatEditText;
import cc.ecisr.jyutdict.widget.SwitchCustomized;

/**
 * app 的主頁面，包含一個查詢結果的 fragment
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "`MainActivity";
    private static final String URL_API_ROOT = "https://jyutdict.org/api/v1.0/";
    private static final int INITIALIZE_LOCATIONS_FAIL = 3288;
    private static final int INITIALIZE_DETAIL_LOCATIONS_FAIL = 3289;
    private static final int SEARCH_SUCCESS_BASE = 0x5000;
    private static final int SEARCH_SUCCESS_MASK = 0xFF00;
    private static final int SEARCH_MODE_MASK = 0x00FF;
    private static final int SEARCH_FAIL = 0x5100;

    AppCompatEditText inputEditText;
    Button btnQueryConfirm, btnQueryClear, btnFilterArea, btnFilterAreaPron, btnColoringJppPartial;
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
    final HttpUtil headerQuery = new HttpUtil(HttpUtil.GET);
    final HttpUtil searchQuery = new HttpUtil(HttpUtil.GET);
    final HttpUtil locationQuery = new HttpUtil(HttpUtil.GET);

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
        btnFilterAreaPron = findViewById(R.id.btn_filter_area_pron);
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
        ImmersiveBarUtil.setImmersiveBar(this, true, false);
        getView();
        if (savedInstanceState == null) {
            resultFragment = new ResultFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.result_fragment, resultFragment).commit();
        } else {
            resultFragment = (ResultFragment) getSupportFragmentManager().getFragment(savedInstanceState, "result_fragment");
        }
        initPermission();

        mainHandler = new MainHandler(Looper.getMainLooper(), msg -> {
            if ((msg.what & SEARCH_SUCCESS_MASK) == SEARCH_SUCCESS_BASE) {
                int responseMode = msg.what & SEARCH_MODE_MASK;
                try {
                    resultFragment.parseJson(msg.obj.toString(), responseMode);
                } catch (JSONException | RuntimeException e) {
                    Log.e(TAG, "Unable to parse search response", e);
                    ToastUtil.msg(this, getString(R.string.error_tips_data));
                }
                finishSearchUi();
                return;
            }

            switch (msg.what) {
                case INITIALIZE_LOCATIONS: // 初始化泛粵字表表頭
                    try {
                        // v1.0: 返回 {"columns": [...]}，v0.9 返回 {"__valid_options": [...]}
                        JSONObject headerObj = new JSONObject(msg.obj.toString());
                        JSONArray headerArray = headerObj.getJSONArray("columns");
                        FjbHeaderInfo.load(headerArray);
                        setLocationsAdapter();
                        if (!isJustInitialized  && inputEditText.getText()!=null && inputEditText.getText().length() != 0) search();
                    } catch (JSONException | RuntimeException e) {
                        Log.e(TAG, "Unable to parse sheet header", e);
                        ToastUtil.msg(this, getString(R.string.error_tips_data));
                    }
                    break;
                case INITIALIZE_DETAIL_LOCATIONS: // 初始化通用字表地點列表
                    try {
                        JSONArray locationArray = new JSONArray(msg.obj.toString());
                        LocationInfo.load(locationArray);
                        // 用地點列表構建篩選城市列表（替代原來從結果動態構建的方式）
                        GeneralCharacterManager.cityList = new ArrayList<>();
                        GeneralCharacterManager.cityList.add("韻書");  // 保留韻書作為可篩選項
                        for (LocationInfo.Location loc : LocationInfo.getAll()) {
                            GeneralCharacterManager.cityList.add(loc.displayName());
                        }
                    } catch (JSONException | RuntimeException e) {
                        Log.e(TAG, "Unable to parse location metadata", e);
                        ToastUtil.msg(this, getString(R.string.error_tips_data));
                    }
                    break;
                case INITIALIZE_LOCATIONS_FAIL:
                    isPrepared = false;
                    showRequestError(msg.obj);
                    btnQueryConfirm.setEnabled(true);
                    break;
                case INITIALIZE_DETAIL_LOCATIONS_FAIL:
                    showRequestError(msg.obj);
                    break;
                case SEARCH_FAIL:
                    showRequestError(msg.obj);
                    finishSearchUi();
                    break;
                default:
                    break;
            }
        });

        // 查詢按鈕
        btnQueryConfirm.setOnClickListener(v -> {
            isJustInitialized = false;
            if (!isPrepared) {
                ToastUtil.msg(this, "正在獲取地方信息，請稍候");
                headerQuery.start(); // 重新向服務器發送請求
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
                if (s.toString().isEmpty()) {
                    btnQueryClear.setVisibility(View.GONE);
                } else {
                    btnQueryClear.setVisibility(View.VISIBLE);
                }

            }
            @Override
            public void afterTextChanged(Editable s) {
                boolean isJpp = StringUtil.isJyutpingInput(s.toString());

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
                if (!switchQueryOpts1.isChecked()) {
                    btnFilterArea.setVisibility(isJpp ? View.GONE : View.VISIBLE);
                    btnFilterAreaPron.setVisibility(isJpp ? View.VISIBLE : View.GONE);
                }
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
        GeneralCharacterManager.cityFilter = new HashSet<>(
                sp.getStringSet("querying_filter_city", new HashSet<>()));
        ResultFragment.pronCityFilter = new HashSet<>(
                sp.getStringSet("querying_filter_city_pron", new HashSet<>()));
        ResultFragment.pronBookFilter = new HashSet<>(
                sp.getStringSet("querying_filter_book_pron", new HashSet<>()));

        queryingModeConfig = sp.getInt("querying_mode_config", 0);
        btnColoringJppPartial.setOnClickListener(view -> {
            AlertDialog.Builder dialog = getDialogForColoringJpp();
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
        btnFilterArea.setOnClickListener(v -> showFilterDialog(
                R.string.search_filtering_area,
                GeneralCharacterManager.cityList,
                GeneralCharacterManager.cityFilter,
                newFilter -> {
                    GeneralCharacterManager.cityFilter = newFilter;
                    resultFragment.refreshResult();
                    saveLayoutStatus();
                }
        ));
        btnFilterAreaPron.setOnClickListener(v -> {
            ArrayList<LocationInfo.Location> locList = LocationInfo.getAll();
            if (locList.isEmpty()) {
                ToastUtil.msg(this, "正在獲取地方信息，請稍候");
                return;
            }
            ArrayList<String> names = new ArrayList<>();
            String fanwanLabel = getString(R.string.search_filter_book_item, "分韻");
            String jingwaaLabel = getString(R.string.search_filter_book_item, "英華");
            names.add(fanwanLabel);
            names.add(jingwaaLabel);
            for (LocationInfo.Location loc : locList) {
                names.add(loc.displayName());
            }
            // 對話框顯示名稱；內部仍分別保存韻書名與地點 id。
            HashSet<String> nameFilter = new HashSet<>();
            if (ResultFragment.pronBookFilter.contains("分韻")) {
                nameFilter.add(fanwanLabel);
            }
            if (ResultFragment.pronBookFilter.contains("英華")) {
                nameFilter.add(jingwaaLabel);
            }
            for (LocationInfo.Location loc : locList) {
                if (ResultFragment.pronCityFilter.contains(String.valueOf(loc.id))) {
                    nameFilter.add(loc.displayName());
                }
            }
            showFilterDialog(
                    R.string.search_filtering_area_pron,
                    names,
                    nameFilter,
                    newFilter -> {
                        HashSet<String> bookFilter = new HashSet<>();
                        if (newFilter.contains(fanwanLabel)) bookFilter.add("分韻");
                        if (newFilter.contains(jingwaaLabel)) bookFilter.add("英華");
                        ResultFragment.pronBookFilter = bookFilter;

                        HashSet<String> idFilter = new HashSet<>();
                        for (LocationInfo.Location loc : locList) {
                            if (newFilter.contains(loc.displayName())) {
                                idFilter.add(String.valueOf(loc.id));
                            }
                        }
                        ResultFragment.pronCityFilter = idFilter;
                        resultFragment.refreshResult();
                        saveLayoutStatus();
                    }
            );
        });
        setSearchView();

        // 獲取泛粵字表的表頭
        setLocationsAdapter();

        boolean hadCheckedInfoActivity = sp.getBoolean("had_checked_info_activity_2", false);
        if (hadCheckedInfoActivity) {
            if (!isPrepared) { headerQuery.start(); }
        } else {
            displayTipsMessageBox();
        }
    }

    /**
     * 通用的篩選對話框：自定義佈局，全選/反選按鈕在對話框內部，不會關閉對話框
     *
     * @param titleRes   對話框標題資源 ID
     * @param itemNames  可選項列表
     * @param filter     當前被過濾掉（不顯示）的項目名稱集合
     * @param onConfirm  確定時回調，傳入最終的 filter 集合
     */
    private void showFilterDialog(int titleRes, ArrayList<String> itemNames,
                                  HashSet<String> filter, FilterResultListener onConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(titleRes);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_pron, null);
        LinearLayout container = dialogView.findViewById(R.id.checkbox_container);

        // 臨時 filter，在確定前不直接修改原始 filter
        HashSet<String> tempFilter = new HashSet<>(filter);

        ArrayList<android.widget.CheckBox> checkBoxes = new ArrayList<>();
        for (int index = 0; index < itemNames.size(); index++) {
            String name = itemNames.get(index);
            android.widget.CheckBox cb = new android.widget.CheckBox(builder.getContext());
            cb.setText(name);
            cb.setChecked(!tempFilter.contains(name));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    tempFilter.remove(name);
                } else {
                    tempFilter.add(name);
                }
            });
            container.addView(cb);
            checkBoxes.add(cb);
        }

        // 全選按鈕
        dialogView.findViewById(R.id.btn_dialog_select_all).setOnClickListener(btn -> {
            tempFilter.clear();
            for (android.widget.CheckBox cb : checkBoxes) {
                cb.setChecked(true);
            }
        });

        // 反選按鈕
        dialogView.findViewById(R.id.btn_dialog_invert).setOnClickListener(btn -> {
            for (android.widget.CheckBox cb : checkBoxes) {
                cb.setChecked(!cb.isChecked());
            }
        });

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.button_confirm, (dialog, which) -> onConfirm.onConfirm(tempFilter));
        builder.create().show();
    }

    private interface FilterResultListener {
        void onConfirm(HashSet<String> filter);
    }

    private AlertDialog.Builder getDialogForColoringJpp() {
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
        return dialog;
    }

    /**
     * 根據設置應用主題
     */
    private void applyLightDarkTheme() {
        if (ThemeUtil.isNightMode(this)) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    private void finishSearchUi() {
        loadingProgressBar.setVisibility(View.GONE);
        btnColoringJppPartial.setEnabled(true);
        btnQueryConfirm.setEnabled(true);
    }

    private void showRequestError(Object errorObject) {
        if (errorObject instanceof HttpUtil.RequestError
                && ((HttpUtil.RequestError) errorObject).kind == HttpUtil.ErrorKind.TIMEOUT) {
            ToastUtil.msg(this, getString(R.string.error_tips_network_out_of_time));
            return;
        }
        String errorCode = errorObject == null ? "network" : errorObject.toString();
        ToastUtil.msg(this, getString(R.string.error_tips_network, errorCode));
    }

    private void setLocationsAdapter() {
        if (isPrepared) return;
        if (FjbHeaderInfo.isLoaded) {
            locationsAdapter.clear();
            locationsAdapter.add(getString(R.string.select_drop_down_standard));
            locationsAdapter.add(getString(R.string.select_drop_down_convenience));
            locationsAdapter.addAll(FjbHeaderInfo.getCityList());
            int savedLocation = sp.getInt("spinner_selected_position", 0);
            int lastLocation = Math.max(0, locationsAdapter.getCount() - 1);
            spinnerQueryLocation.setSelection(
                    Math.max(0, Math.min(savedLocation, lastLocation)));
            isPrepared = true;
            // 在泛粵字表表頭初始化完成後，啟動地點列表請求（如果尚未加載）
            if (!LocationInfo.isLoaded) {
                locationQuery.setUrl(ApiUrlBuilder.from(URL_API_ROOT, "detail")
                                .add("chara", "")
                                .build())
                        .setHandler(
                                mainHandler,
                                INITIALIZE_DETAIL_LOCATIONS,
                                INITIALIZE_DETAIL_LOCATIONS_FAIL
                        )
                        .start();
            }
        } else {
            headerQuery.setUrl(ApiUrlBuilder.from(URL_API_ROOT, "sheet").build())
                    .setHandler(
                            mainHandler,
                            INITIALIZE_LOCATIONS,
                            INITIALIZE_LOCATIONS_FAIL
                    );
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
            btnFilterAreaPron.setVisibility(View.GONE);
            btnColoringJppPartial.setVisibility(View.GONE);
        } else {
            switch1Text = getString(R.string.search_common_sheet);
            switchQueryOptsRev.setVisibility(View.GONE);
            spinnerQueryLocation.setVisibility(View.GONE);
            boolean isJpp = inputEditText.getText() != null && StringUtil.isJyutpingInput(inputEditText.getText().toString());
            btnFilterArea.setVisibility(isJpp ? View.GONE : View.VISIBLE);
            btnFilterAreaPron.setVisibility(isJpp ? View.VISIBLE : View.GONE);
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
     * REQUESTING_SETTING 表示打開設置界面的 request code
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        int itemId = item.getItemId();
        if (itemId == R.id.menu_setting) {
            startActivitySetting.launch(
                    new Intent(MainActivity.this, SettingsActivity.class)
            );
        } else if (itemId == R.id.menu_info) {
            intent = new Intent(MainActivity.this, InfoActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * saveLayoutStatus()
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
        editor.putStringSet("querying_filter_city_pron", ResultFragment.pronCityFilter);
        editor.putStringSet("querying_filter_book_pron", ResultFragment.pronBookFilter);
        editor.apply();
    }


    /**
     * 更新輸入框中的字符串到 {@code this.inputString} 中
     * 在將發起查詢時調用
     *
     * @param string 輸入框中的字符串
     */
    private void setInputString(String string) {
        inputString = string;
    }

    /**
     * 用指定字符串以指定模式發起查詢
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
     * 模式由主界面的開關指定，查詢內容由 {@code this.inputString} 指定
     * 在等待回應時會禁用查詢按鈕
     *
     */
    private void search() {
        if (inputEditText.getText() == null) { return; }
        if (!isPrepared) {
            ToastUtil.msg(this, "正在獲取地方信息，請稍候");
            headerQuery.start();
            return;
        }
        setInputString(inputEditText.getText().toString()); // 必须放在最前面
        if (isPrepared && "".equals(inputString) && !(switchQueryOpts1.isChecked() && !switchQueryOptsRev.isChecked())) {
            return;
        } // 搜索欄爲空時不檢索

        loadingProgressBar.setVisibility(View.VISIBLE);
        btnColoringJppPartial.setEnabled(false);
        ApiUrlBuilder url;
        int modeSnapshot;
        if (switchQueryOpts1.isChecked()) { // 檢索泛粵字表
            modeSnapshot = QUERYING_SHEET;
            url = ApiUrlBuilder.from(URL_API_ROOT, "sheet");
            if (inputString.isEmpty() && !switchQueryOptsRev.isChecked()) {
                url.add("random", 10);
            } else {
                url.add("q", inputString);
                String sheetMode;
                if (StringUtil.isAlphaString(inputString) && !switchQueryOptsRev.isChecked()) {
                    sheetMode = "trim";
                } else if (switchQueryOptsRev.isChecked()) {
                    sheetMode = "meaning";
                } else {
                    sheetMode = "fuzzy";
                }
                if (switchQueryOptsRegex.isChecked()) {
                    sheetMode = "regex";
                }
                url.add("mode", sheetMode);

                int selectedColumn = spinnerQueryLocation.getSelectedItemPosition();

                if (selectedColumn >= 2) {
                    String col = FjbHeaderInfo.getCityNameByNumber(selectedColumn - 2);
                    url.add("col", col);
                } else if (selectedColumn == 1) {
                    if (StringUtil.isAlphaString(inputString)) {
                        url.add("col", "檢");
                    }
                }
            }
        } else { // 檢索通用字表
            url = ApiUrlBuilder.from(URL_API_ROOT, "detail");
            if (StringUtil.isJyutpingInput(inputString)) { // 音（允許空格作模糊佔位）
                String[] parts = JyutpingUtil.parseJyutpingQuery(inputString);
                if (parts != null) {
                    modeSnapshot = QUERYING_PRON;
                    url.add("in", parts[0])
                            .add("nu", parts[1])
                            .add("co", parts[2]);
                    if (parts[3] != null && !parts[3].isEmpty()) {
                        url.add("to", parts[3]);
                    }
                    addPronunciationBookFilter(url);
                } else {
                    // 解析失敗，回退為查字模式
                    modeSnapshot = QUERYING_CHARA;
                    url.add("chara", inputString);
                }
            } else { // 字
                modeSnapshot = QUERYING_CHARA;
                url.add("chara", inputString);
            }
        }
        queryingMode = modeSnapshot;
        int responseMode = modeSnapshot | queryingModeConfig;
        searchQuery.setUrl(url.build())
                .setHandler(
                        mainHandler,
                        SEARCH_SUCCESS_BASE | responseMode,
                        SEARCH_FAIL
                )
                .start();
        btnQueryConfirm.setEnabled(false);
        saveLayoutStatus();
    }

    private void addPronunciationBookFilter(ApiUrlBuilder url) {
        boolean hidesFanwan = ResultFragment.pronBookFilter.contains("分韻");
        boolean hidesJingwaa = ResultFragment.pronBookFilter.contains("英華");
        if (hidesFanwan && hidesJingwaa) {
            url.add("wanshyu", "none");
        } else if (hidesFanwan) {
            url.add("wanshyu", "jingwaa");
        } else if (hidesJingwaa) {
            url.add("wanshyu", "fanwan");
        }
    }

    private final ActivityResultLauncher<Intent> startActivitySetting = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                int resultCode = result.getResultCode();
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
            });

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
                            startActivity(new Intent(MainActivity.this, InfoActivity.class));
                            sp.edit().putBoolean("had_checked_info_activity_2", true).apply();
                        })
                .setCancelable(false)
                .show();
    }

    /**
     * 申請網絡等權限
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
    protected void onDestroy() {
        headerQuery.cancel();
        locationQuery.cancel();
        searchQuery.cancel();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (resultFragment != null) {
            getSupportFragmentManager().putFragment(outState, "result_fragment", resultFragment);
        }
    }
}
