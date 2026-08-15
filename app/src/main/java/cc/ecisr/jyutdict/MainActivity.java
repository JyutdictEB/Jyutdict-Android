package cc.ecisr.jyutdict;

import static cc.ecisr.jyutdict.utils.EnumConst.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import cc.ecisr.jyutdict.auth.AuthRepository;
import cc.ecisr.jyutdict.databinding.ActivityMainBinding;
import cc.ecisr.jyutdict.databinding.DialogFilterPronBinding;
import cc.ecisr.jyutdict.databinding.DialogLocationPickerBinding;
import cc.ecisr.jyutdict.databinding.LocationPickerItemBinding;
import cc.ecisr.jyutdict.search.SearchViewModel;
import cc.ecisr.jyutdict.search.SearchViewModel.Request;
import cc.ecisr.jyutdict.struct.FjbHeaderInfo;
import cc.ecisr.jyutdict.struct.GeneralCharacterManager;
import cc.ecisr.jyutdict.struct.LocationInfo;
import cc.ecisr.jyutdict.utils.ApiUrlBuilder;
import cc.ecisr.jyutdict.utils.ColorUtil;
import cc.ecisr.jyutdict.utils.DiskTextCache;
import cc.ecisr.jyutdict.utils.ImmersiveBarUtil;
import cc.ecisr.jyutdict.utils.JyutpingUtil;
import cc.ecisr.jyutdict.utils.MotionUtil;
import cc.ecisr.jyutdict.utils.StringUtil;
import cc.ecisr.jyutdict.utils.HttpUtil;
import cc.ecisr.jyutdict.utils.ThemeUtil;
import cc.ecisr.jyutdict.utils.ToastUtil;
import androidx.appcompat.widget.AppCompatEditText;
import cc.ecisr.jyutdict.widget.LocationSpinnerAdapter;

/**
 * app 的主頁面，包含一個查詢結果的 fragment
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "`MainActivity";
    private static final String URL_API_ROOT = "https://jyutdict.org/api/v1.0/";
    private static final String SHEET_HEADER_CACHE_KEY = "main_sheet_header_v1";
    private static final String LOCATION_HEADER_CACHE_KEY = "main_location_header_v1";
    private static final long HEADER_CACHE_MAX_AGE = 24L * 60L * 60L * 1000L;
    private static final long HEADER_REQUEST_WATCHDOG = 22_000L;
    private static final long HEADER_RETRY_BASE_DELAY = 2_000L;
    private static final long HEADER_RETRY_MAX_DELAY = 30_000L;
    private static final long HEADER_READY_STATUS_DURATION = 2_500L;
    private static final int DEFAULT_LOCATION_POSITION = 1;
    private static final String LOCATION_SELECTION_EXPLICIT_KEY =
            "location_selection_explicit_v1";
    private static final String LOCATION_SELECTION_COLUMN_KEY =
            "location_selection_column_v2";
    private static final String LEGACY_LOCATION_SELECTION_RECENT_KEY =
            "location_selection_recent_v2";
    private static final String LAST_LOCATION_COLUMN_KEY =
            "last_location_column_v2";
    private static final String EXTRA_THEME_TRANSITION = "theme_transition";

    AppCompatEditText inputEditText;
    private ActivityMainBinding binding;
    Button btnQueryConfirm, btnFilterArea, btnFilterAreaPron, btnColoringJppPartial;
    MaterialButton switchQueryOptsRev, switchQueryOptsRegex;
    RadioGroup sheetModeGroup;
    ResultFragment resultFragment;
    ProgressBar loadingProgressBar, headerLoadingSpinner;
    View headerLoadingStatus, locationPicker, locationPickerSwatch, sheetQueryOptions;
    TextView btnClearInput, headerLoadingText, locationPickerText;
    Toolbar toolbar;
    LinearLayout lyMain, lyAdvancedSearch;
    AuthRepository authRepository;
    private SearchViewModel searchViewModel;

    // 在輸入框輸入的字符串，在按下查詢按鈕時更新
    String inputString;

    // 查詢按鈕字體的顏色，僅用於功能測試
    int previousColor;

    boolean headerLoadingInitialized = false;
    boolean headerRetriesEnabled = false;
    boolean pendingSearch = false;
    int selectedLocationPosition = 0;
    Handler mainHandler;
    HeaderLoader sheetHeader, locationHeader;

    // 下拉選擇框的 Adapter，存放的是可供查詢的查詢地名
    ArrayList<LocationSpinnerAdapter.Option> locationOptions = new ArrayList<>();

    final Runnable hideHeaderReadyStatus = () -> {
        if (!sheetHeader.needsRefresh && !locationHeader.needsRefresh) {
            MotionUtil.beginLayoutTransition(lyMain);
            headerLoadingStatus.setVisibility(View.GONE);
        }
    };

    // 用於獲取用戶的設置，與存儲各開關的狀態
    SharedPreferences sp;

    // 指示搜索模式，查通用表字/查通用表音/查泛粵表
    // 在按下查詢按鈕時更新
    // 並根據這個狀態來解析JSON
    int queryingMode = QUERYING_CHARA;
    int queryingModeConfig = 0;
    private boolean inputEnterKeyDown = false;

    // 夜间模式
    //private static boolean isNightMode = false;

    /**
     * 初始化界面，獲取界面上各物件的視圖
     */
    void getView() {
        lyMain = binding.wholeMainLayout;
        inputEditText = binding.editTextInput;
        btnClearInput = binding.btnClearInput;
        btnQueryConfirm = binding.btnQuery;
        btnFilterArea = binding.btnFilterArea;
        btnFilterAreaPron = binding.btnFilterAreaPron;
        btnColoringJppPartial = binding.btnColoringJppPartial;
        locationPicker = binding.locatePicker;
        sheetQueryOptions = binding.sheetQueryOptions;
        locationPickerText = binding.locatePickerText;
        locationPickerSwatch = binding.locatePickerSwatch;
        lyAdvancedSearch = binding.inputAdvancedSwitch;
        sheetModeGroup = binding.sheetModeGroup;
        switchQueryOptsRev = binding.switchReverseSearch;
        switchQueryOptsRegex = binding.switchUseRegex;
        loadingProgressBar = binding.loadingProgress;
        headerLoadingStatus = binding.headerLoadingStatus;
        headerLoadingSpinner = binding.headerLoadingSpinner;
        headerLoadingText = binding.headerLoadingText;
        toolbar = binding.toolBar;

        setSupportActionBar(toolbar);
        locationOptions = buildLocationOptions(false);
        selectedLocationPosition = Math.max(0, Math.min(
                getInitialLocationPosition(),
                Math.max(0, locationOptions.size() - 1)));
        updateLocationPickerPresentation();
        locationPicker.setOnClickListener(view -> showLocationPickerDialog());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyLightDarkTheme();
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("settings", MODE_PRIVATE);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (savedInstanceState != null
                && getIntent().getBooleanExtra(EXTRA_THEME_TRANSITION, false)) {
            View content = findViewById(android.R.id.content);
            content.setAlpha(0f);
            content.post(() -> MotionUtil.fadeIn(content));
            getIntent().removeExtra(EXTRA_THEME_TRANSITION);
        }
        ImmersiveBarUtil.setImmersiveBar(this, false, !ThemeUtil.isNightMode(this));
        getView();
        ImmersiveBarUtil.applyToolbarInsets(toolbar);
        authRepository = AuthRepository.getInstance(this);
        authRepository.initialize((success, errorMessage) -> {});
        if (savedInstanceState == null) {
            resultFragment = new ResultFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.result_fragment, resultFragment).commit();
        } else {
            resultFragment = (ResultFragment) getSupportFragmentManager().getFragment(savedInstanceState, "result_fragment");
        }
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        binding.getRoot().post(this::observeSearchState);
        initPermission();

        mainHandler = new Handler(Looper.getMainLooper());

        // 查詢按鈕
        btnQueryConfirm.setOnClickListener(v -> search());
        btnClearInput.setOnClickListener(v -> {
            inputEditText.setText("");
            inputEditText.requestFocus();
        });

        // 監聽焦點在輸入框內的軟鍵盤的確認按鈕
        inputEditText.setOnEditorActionListener((v, actionId, event) -> {
            Log.i(TAG, "onEditorAction: " + actionId);
            boolean isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_SEND;
            if (isSearchAction) {
                submitSearchFromInput(v);
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
            }
            @Override
            public void afterTextChanged(Editable s) {
                btnClearInput.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
                boolean isJpp = StringUtil.isJyutpingInput(s.toString());

                int presentColor = isJpp ?
                        ContextCompat.getColor(MainActivity.this, R.color.colorSecondary) :
                        ContextCompat.getColor(MainActivity.this, R.color.colorPrimary);
                if (previousColor == presentColor) return;
                MotionUtil.animateTextColor(
                        (TextView) btnQueryConfirm, previousColor, presentColor);
                previousColor = presentColor;
                if (!isSheetMode()) {
                    MotionUtil.beginLayoutTransition(binding.queryLayout);
                    btnFilterArea.setVisibility(isJpp ? View.GONE : View.VISIBLE);
                    btnFilterAreaPron.setVisibility(isJpp ? View.VISIBLE : View.GONE);
                }
            }
        });
        previousColor = ContextCompat.getColor(this, R.color.colorPrimary);
        btnClearInput.setVisibility(inputEditText.length() == 0 ? View.GONE : View.VISIBLE);

        // 讀取幾個開關之前的狀態
        setSheetMode(sp.getBoolean("switch_1_is_checked", false));
        switchQueryOptsRev.setChecked(sp.getBoolean("switch_2_is_checked", false));
        switchQueryOptsRegex.setChecked(sp.getBoolean("switch_3_is_checked", false));
        lyAdvancedSearch.setVisibility(sp.getBoolean("advanced_search", false) ? View.VISIBLE : View.GONE);
        sheetModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == -1) return;
            setInputEditTextHint();
            setSearchView();
        });
        switchQueryOptsRev.addOnCheckedChangeListener((button, isChecked) -> {
            setInputEditTextHint();
            setSearchView();
        });
        setInputEditTextHint();
        setSearchView();
        //inputEditText.setOnClickListener(v -> toggleNightTheme());
        GeneralCharacterManager.cityFilter = new HashSet<>(
                sp.getStringSet("querying_filter_city", new HashSet<>()));
        if (GeneralCharacterManager.cityFilter.remove("韻書")) {
            GeneralCharacterManager.cityFilter.add(GeneralCharacterManager.FILTER_BOOK_FANWAN);
            GeneralCharacterManager.cityFilter.add(GeneralCharacterManager.FILTER_BOOK_JINGWAA);
        }
        ResultFragment.pronCityFilter = new HashSet<>(
                sp.getStringSet("querying_filter_city_pron", new HashSet<>()));
        ResultFragment.pronBookFilter = new HashSet<>(
                sp.getStringSet("querying_filter_book_pron", new HashSet<>()));
        updateFilterButtonLabels();

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

        initializeHeaderLoading();

        boolean hadCheckedInfoActivity = sp.getBoolean("had_checked_info_activity_2", false);
        if (!hadCheckedInfoActivity) {
            displayTipsMessageBox();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && inputEditText != null
                    && inputEditText.hasFocus()) {
                if (event.getRepeatCount() == 0) {
                    inputEnterKeyDown = true;
                    submitSearchFromInput(inputEditText);
                }
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_UP && inputEnterKeyDown) {
                inputEnterKeyDown = false;
                inputEditText.requestFocus();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void submitSearchFromInput(View input) {
        search();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(input.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
        input.requestFocus();
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

        DialogFilterPronBinding dialogBinding =
                DialogFilterPronBinding.inflate(getLayoutInflater());
        LinearLayout container = dialogBinding.checkboxContainer;
        AppCompatEditText filterSearch = dialogBinding.filterSearch;
        TextView selectionSummary = dialogBinding.filterSelectionSummary;

        // 臨時 filter，在確定前不直接修改原始 filter
        HashSet<String> tempFilter = new HashSet<>(filter);
        tempFilter.retainAll(itemNames);

        ArrayList<AppCompatCheckBox> checkBoxes = new ArrayList<>();
        Runnable updateSelectionSummary = () -> selectionSummary.setText(getString(
                R.string.search_filter_selection_summary,
                itemNames.size() - tempFilter.size(),
                itemNames.size()));
        for (int index = 0; index < itemNames.size(); index++) {
            String name = itemNames.get(index);
            AppCompatCheckBox cb = new AppCompatCheckBox(builder.getContext());
            cb.setText(name);
            cb.setMinHeight(getResources().getDimensionPixelSize(R.dimen.compact_touch_target));
            cb.setChecked(!tempFilter.contains(name));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    tempFilter.remove(name);
                } else {
                    tempFilter.add(name);
                }
                updateSelectionSummary.run();
            });
            container.addView(cb);
            checkBoxes.add(cb);
        }
        updateSelectionSummary.run();

        filterSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                String query = String.valueOf(text).trim().toLowerCase(Locale.ROOT);
                MotionUtil.beginLayoutTransition(container);
                for (int index = 0; index < checkBoxes.size(); index++) {
                    String item = itemNames.get(index).toLowerCase(Locale.ROOT);
                    checkBoxes.get(index).setVisibility(
                            item.contains(query) ? View.VISIBLE : View.GONE
                    );
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // 全選按鈕
        dialogBinding.btnDialogSelectAll.setOnClickListener(btn -> {
            tempFilter.clear();
            for (AppCompatCheckBox cb : checkBoxes) {
                cb.setChecked(true);
            }
            updateSelectionSummary.run();
        });

        // 清除選取
        dialogBinding.btnDialogInvert.setOnClickListener(btn -> {
            for (AppCompatCheckBox cb : checkBoxes) {
                cb.setChecked(false);
            }
            updateSelectionSummary.run();
        });

        builder.setView(dialogBinding.getRoot());
        builder.setNegativeButton(R.string.button_cancel, null);
        builder.setPositiveButton(R.string.button_confirm, (dialog, which) -> {
            onConfirm.onConfirm(tempFilter);
            updateFilterButtonLabels();
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        constrainDialogWidth(dialog, 400);
    }

    private void showLocationPickerDialog() {
        if (locationOptions.isEmpty()) return;

        DialogLocationPickerBinding dialogBinding =
                DialogLocationPickerBinding.inflate(getLayoutInflater());
        LinearLayout container = dialogBinding.locationPickerContainer;
        AppCompatEditText search = dialogBinding.locationPickerSearch;
        TextView summary = dialogBinding.locationPickerSummary;
        ArrayList<View> rows = new ArrayList<>();
        ArrayList<LocationSpinnerAdapter.Option> listedOptions = new ArrayList<>();
        LocationSpinnerAdapter.Option recentOption = buildRecentLocationOption();
        int selectedListedPosition = 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.search_choose_location)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.button_cancel, null);
        AlertDialog dialog = builder.create();

        for (int index = 0; index < locationOptions.size(); index++) {
            LocationSpinnerAdapter.Option option = locationOptions.get(index);
            if (index == selectedLocationPosition) {
                selectedListedPosition = listedOptions.size();
            }
            listedOptions.add(option);
            LocationPickerItemBinding rowBinding = LocationPickerItemBinding.inflate(
                    getLayoutInflater(), container, false);
            View row = rowBinding.getRoot();
            rowBinding.locationPickerItemSwatch.setBackground(
                    ColorUtil.locationColorDrawable(option.colors));
            rowBinding.locationPickerItemRadio.setText(option.label);
            rowBinding.locationPickerItemRadio.setChecked(index == selectedLocationPosition);
            row.setOnClickListener(view -> selectLocationOption(option, dialog));
            container.addView(row);
            rows.add(row);
        }

        if (recentOption != null) {
            LocationSpinnerAdapter.Option standaloneRecent = recentOption;
            dialogBinding.locationPickerRecentText.setText(standaloneRecent.label);
            dialogBinding.locationPickerRecentSwatch.setBackground(
                    ColorUtil.locationColorDrawable(standaloneRecent.colors));
            int recentBackground = ContextCompat.getColor(this,
                    ThemeUtil.isNightMode(this) ? R.color.colorHoverDark : R.color.colorHover);
            dialogBinding.locationPickerRecent.setBackgroundColor(recentBackground);
            dialogBinding.locationPickerRecent.setOnClickListener(view ->
                    selectLocationOption(standaloneRecent, dialog));
        } else {
            dialogBinding.locationPickerRecent.setVisibility(View.GONE);
        }

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                String query = String.valueOf(text).trim().toLowerCase(Locale.ROOT);
                int visibleCount = 0;
                MotionUtil.beginLayoutTransition(container);
                for (int index = 0; index < listedOptions.size(); index++) {
                    boolean visible = listedOptions.get(index).label
                            .toLowerCase(Locale.ROOT).contains(query);
                    rows.get(index).setVisibility(visible ? View.VISIBLE : View.GONE);
                    if (visible) visibleCount++;
                }
                summary.setText(getString(R.string.search_location_picker_summary,
                        visibleCount, listedOptions.size()));
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        summary.setText(getString(R.string.search_location_picker_summary,
                listedOptions.size(), listedOptions.size()));

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        constrainDialogWidth(dialog, 340);
        int initialScrollPosition = selectedListedPosition;
        dialogBinding.locationPickerScroll.post(() ->
                dialogBinding.locationPickerScroll.scrollTo(0,
                initialScrollPosition * Math.round(
                        34 * getResources().getDisplayMetrics().density)));
    }

    private void selectLocationOption(LocationSpinnerAdapter.Option selected,
                                      AlertDialog dialog) {
        LocationSpinnerAdapter.Option previous = getSelectedLocationOption();
        if (previous != null && !previous.queryColumn.equals(selected.queryColumn)) {
            sp.edit().putString(
                    LAST_LOCATION_COLUMN_KEY, previous.queryColumn).apply();
        }

        locationOptions = buildLocationOptions(FjbHeaderInfo.isLoaded);
        selectedLocationPosition = findLocationOption(
                selected.queryColumn, locationOptions);
        sp.edit().putBoolean(LOCATION_SELECTION_EXPLICIT_KEY, true).apply();
        updateLocationPickerPresentation();
        saveLayoutStatus();
        dialog.dismiss();
    }

    private void constrainDialogWidth(AlertDialog dialog, int maxWidthDp) {
        Window window = dialog.getWindow();
        if (window == null) return;
        int horizontalMargin = getResources().getDimensionPixelSize(R.dimen.space_xl);
        int maxWidth = Math.round(maxWidthDp * getResources().getDisplayMetrics().density);
        int availableWidth = getResources().getDisplayMetrics().widthPixels - horizontalMargin * 2;
        window.setLayout(Math.min(maxWidth, availableWidth),
                WindowManager.LayoutParams.WRAP_CONTENT);
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
        if (resultFragment != null) resultFragment.finishLoading();
    }

    private void beginSearchUi() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        if (resultFragment != null) resultFragment.beginLoading();
        btnColoringJppPartial.setEnabled(false);
        btnQueryConfirm.setEnabled(false);
    }

    private void observeSearchState() {
        searchViewModel.getUiState().observe(this, state -> {
            switch (state.status) {
                case LOADING:
                    beginSearchUi();
                    break;
                case SUCCESS:
                    if (state.request == null || state.responseBody == null) return;
                    binding.resultFragment.post(() -> {
                        try {
                            resultFragment.parseJson(
                                    state.responseBody,
                                    state.request.responseMode);
                        } catch (JSONException | RuntimeException exception) {
                            Log.e(TAG, "Unable to parse search response", exception);
                            ToastUtil.msg(this, getString(R.string.error_tips_data));
                        }
                        finishSearchUi();
                    });
                    break;
                case ERROR:
                    showRequestError(state.error);
                    finishSearchUi();
                    break;
                case IDLE:
                default:
                    break;
            }
        });
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

    private void initializeHeaderLoading() {
        sheetHeader = new HeaderLoader(true,
                ApiUrlBuilder.from(URL_API_ROOT, "sheet").build(), SHEET_HEADER_CACHE_KEY);
        locationHeader = new HeaderLoader(false,
                ApiUrlBuilder.from(URL_API_ROOT, "detail").add("chara", "").build(),
                LOCATION_HEADER_CACHE_KEY);
        sheetHeader.needsRefresh = !sheetHeader.restoreCache();
        locationHeader.needsRefresh = !locationHeader.restoreCache();
        headerLoadingInitialized = true;
        headerLoadingStatus.setOnClickListener(view -> retryHeadersNow());
        boolean usingFreshCache = !sheetHeader.needsRefresh && !locationHeader.needsRefresh;
        updateHeaderLoadingStatus(usingFreshCache, usingFreshCache);
    }

    private boolean applyHeader(boolean sheet, String raw, boolean cacheResponse) {
        try {
            if (sheet) {
                JSONObject headerObject = new JSONObject(raw);
                JSONArray headerArray = headerObject.optJSONArray("columns");
                if (!hasValidSheetHeader(headerArray)) return false;
                FjbHeaderInfo.load(headerArray);
                updateLocationsAdapter();
            } else {
                JSONArray locationArray = new JSONArray(raw);
                if (!hasValidLocationHeader(locationArray)) return false;
                LocationInfo.load(locationArray);
                rebuildGeneralLocationList();
            }
            if (cacheResponse) {
                DiskTextCache.write(this,
                        sheet ? SHEET_HEADER_CACHE_KEY : LOCATION_HEADER_CACHE_KEY,
                        raw);
            }
            return true;
        } catch (JSONException | RuntimeException exception) {
            Log.e(TAG, "Unable to parse " + (sheet ? "sheet" : "location") + " header", exception);
            return false;
        }
    }

    private boolean hasValidSheetHeader(JSONArray array) {
        if (array == null || array.length() == 0) return false;
        boolean hasCharacterColumn = false;
        boolean hasPronunciationColumn = false;
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item != null && item.optInt("index", -1) >= 0
                    && !item.optString("col", "").isEmpty()) {
                String column = item.optString("col", "");
                hasCharacterColumn |= FjbHeaderInfo.COLUMN_NAME_CHARACTER.equals(column);
                hasPronunciationColumn |= FjbHeaderInfo.COLUMN_NAME_PRONUNCIATION.equals(column);
            }
        }
        return hasCharacterColumn && hasPronunciationColumn;
    }

    private boolean hasValidLocationHeader(JSONArray array) {
        if (array == null || array.length() == 0) return false;
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.optJSONObject(index);
            if (item != null && item.optInt("id", -1) >= 0) return true;
        }
        return false;
    }

    private void updateLocationsAdapter() {
        if (!FjbHeaderInfo.isLoaded) return;
        boolean hasSemanticSelection = sp.contains(LOCATION_SELECTION_COLUMN_KEY);
        String selectedColumn = sp.getString(
                LOCATION_SELECTION_COLUMN_KEY, FjbHeaderInfo.COLUMN_NAME_RETRIEVAL);
        locationOptions = buildLocationOptions(true);
        if (hasSemanticSelection) {
            selectedLocationPosition = findLocationOption(
                    selectedColumn, locationOptions);
        } else {
            selectedLocationPosition = getInitialLocationPosition();
        }
        updateLocationPickerPresentation();
    }

    private int getInitialLocationPosition() {
        if (sp.contains(LOCATION_SELECTION_COLUMN_KEY)) {
            return findLocationOption(
                    sp.getString(LOCATION_SELECTION_COLUMN_KEY,
                            FjbHeaderInfo.COLUMN_NAME_RETRIEVAL),
                    locationOptions);
        }
        int savedPosition = sp.getInt(
                "spinner_selected_position", DEFAULT_LOCATION_POSITION);
        if (savedPosition >= 1) {
            return Math.min(savedPosition, locationOptions.size() - 1);
        }
        if (savedPosition == 0
                && sp.getBoolean(LOCATION_SELECTION_EXPLICIT_KEY, false)) {
            return 0;
        }
        return DEFAULT_LOCATION_POSITION;
    }

    private LocationSpinnerAdapter.Option getSelectedLocationOption() {
        if (locationOptions.isEmpty()) return null;
        int position = Math.max(0, Math.min(
                selectedLocationPosition, locationOptions.size() - 1));
        return locationOptions.get(position);
    }

    private int findLocationOption(String queryColumn,
                                   ArrayList<LocationSpinnerAdapter.Option> options) {
        for (int index = 0; index < options.size(); index++) {
            LocationSpinnerAdapter.Option option = options.get(index);
            if (option.queryColumn.equals(queryColumn)) {
                return index;
            }
        }
        return Math.min(DEFAULT_LOCATION_POSITION, Math.max(0, options.size() - 1));
    }

    private void updateLocationPickerPresentation() {
        if (locationPickerText == null || locationPickerSwatch == null
                || locationOptions.isEmpty()) return;
        selectedLocationPosition = Math.max(0, Math.min(
                selectedLocationPosition, locationOptions.size() - 1));
        LocationSpinnerAdapter.Option option = locationOptions.get(selectedLocationPosition);
        MotionUtil.setText(locationPickerText, option.label);
        locationPickerSwatch.setBackground(ColorUtil.locationColorDrawable(option.colors));
        locationPicker.setContentDescription(
                getString(R.string.search_choose_location) + "：" + option.label);
    }

    private void rebuildGeneralLocationList() {
        GeneralCharacterManager.cityList = new ArrayList<>();
        GeneralCharacterManager.cityList.add(GeneralCharacterManager.FILTER_BOOK_FANWAN);
        GeneralCharacterManager.cityList.add(GeneralCharacterManager.FILTER_BOOK_JINGWAA);
        for (LocationInfo.Location location : LocationInfo.getAll()) {
            GeneralCharacterManager.cityList.add(location.displayName());
        }
        updateFilterButtonLabels();
    }

    /** One independent cached header request, including retry and watchdog state. */
    private final class HeaderLoader {
        final boolean sheet;
        final String url, cacheKey;
        final HttpUtil request = new HttpUtil(HttpUtil.GET).setTimeouts(8_000, 12_000);
        boolean needsRefresh = true, inFlight, retryScheduled;
        int retryAttempt;
        final Runnable retry = () -> {
            retryScheduled = false;
            start(false);
        };
        final Runnable watchdog = () -> {
            if (!inFlight) return;
            request.cancel();
            fail("timeout");
        };

        HeaderLoader(boolean sheet, String url, String cacheKey) {
            this.sheet = sheet;
            this.url = url;
            this.cacheKey = cacheKey;
        }

        boolean restoreCache() {
            String cached = DiskTextCache.readFresh(
                    MainActivity.this, cacheKey, HEADER_CACHE_MAX_AGE);
            boolean fresh = cached != null;
            if (cached == null) cached = DiskTextCache.readAny(MainActivity.this, cacheKey);
            if (cached != null && !applyHeader(sheet, cached, false)) {
                Log.w(TAG, "Ignoring invalid cached "
                        + (sheet ? "sheet" : "location") + " header");
                fresh = false;
            }
            return fresh;
        }

        void start(boolean resetBackoff) {
            if (!headerLoadingInitialized || !headerRetriesEnabled
                    || !needsRefresh || inFlight) return;
            clearRetry();
            if (resetBackoff) retryAttempt = 0;
            inFlight = true;
            updateHeaderLoadingStatus();
            request.enqueue(url, new HttpUtil.Callback() {
                @Override
                public void onSuccess(String body) {
                    finish(body);
                }

                @Override
                public void onFailure(HttpUtil.RequestError error) {
                    fail(error);
                }
            });
            mainHandler.postDelayed(watchdog, HEADER_REQUEST_WATCHDOG);
        }

        void finish(String raw) {
            clearWatchdog();
            inFlight = false;
            if (!applyHeader(sheet, raw, true)) {
                fail(null);
                return;
            }
            needsRefresh = false;
            retryAttempt = 0;
            clearRetry();
            updateHeaderLoadingStatus(true);
            maybeRunPendingSearch();
        }

        void fail(Object error) {
            clearWatchdog();
            inFlight = false;
            needsRefresh = true;
            if (error != null) Log.w(TAG, (sheet ? "Sheet" : "Location")
                    + " header request failed: " + error);
            scheduleRetry();
            updateHeaderLoadingStatus();
        }

        void scheduleRetry() {
            if (!headerRetriesEnabled || !needsRefresh) return;
            clearRetry();
            long multiplier = 1L << Math.min(retryAttempt++, 4);
            retryScheduled = true;
            mainHandler.postDelayed(retry, Math.min(HEADER_RETRY_MAX_DELAY,
                    HEADER_RETRY_BASE_DELAY * multiplier));
        }

        void clearRetry() {
            mainHandler.removeCallbacks(retry);
            retryScheduled = false;
        }

        void clearWatchdog() {
            mainHandler.removeCallbacks(watchdog);
        }

        void stop() {
            clearRetry();
            clearWatchdog();
            if (inFlight) request.cancel();
            inFlight = false;
        }
    }

    private void retryHeadersNow() {
        sheetHeader.start(true);
        locationHeader.start(true);
    }

    private void requestNeededHeaders() {
        sheetHeader.start(false);
        locationHeader.start(false);
    }

    private void updateHeaderLoadingStatus() {
        updateHeaderLoadingStatus(false);
    }

    private void updateHeaderLoadingStatus(boolean announceReady) {
        updateHeaderLoadingStatus(announceReady, false);
    }

    private void updateHeaderLoadingStatus(boolean announceReady, boolean usingFreshCache) {
        MotionUtil.beginLayoutTransition(lyMain);
        if (!headerLoadingInitialized) {
            headerLoadingStatus.setVisibility(View.GONE);
            return;
        }

        mainHandler.removeCallbacks(hideHeaderReadyStatus);
        if (!sheetHeader.needsRefresh && !locationHeader.needsRefresh) {
            headerLoadingSpinner.setVisibility(View.GONE);
            MotionUtil.setText(headerLoadingText, getString(usingFreshCache
                    ? R.string.header_sync_cached
                    : R.string.header_sync_ready));
            headerLoadingText.setAlpha(usingFreshCache ? 0.52f : 1f);
            headerLoadingStatus.setVisibility(announceReady ? View.VISIBLE : View.GONE);
            if (announceReady) {
                mainHandler.postDelayed(hideHeaderReadyStatus, HEADER_READY_STATUS_DURATION);
            }
            return;
        }

        int readyCount = (FjbHeaderInfo.isLoaded ? 1 : 0) + (LocationInfo.isLoaded ? 1 : 0);
        boolean loading = sheetHeader.inFlight || locationHeader.inFlight;
        boolean waitingToRetry = sheetHeader.retryScheduled || locationHeader.retryScheduled;
        headerLoadingText.setAlpha(1f);
        headerLoadingSpinner.setVisibility(loading || !waitingToRetry
                ? View.VISIBLE : View.INVISIBLE);
        if (loading || !waitingToRetry) {
            MotionUtil.setText(headerLoadingText, readyCount == 2
                    ? getString(R.string.header_sync_updating)
                    : getString(R.string.header_sync_preparing, readyCount));
        } else {
            MotionUtil.setText(headerLoadingText, readyCount == 2
                    ? getString(R.string.header_sync_stale)
                    : getString(R.string.header_sync_retrying));
        }
        headerLoadingStatus.setVisibility(View.VISIBLE);
    }

    private void maybeRunPendingSearch() {
        if (!pendingSearch || !isRequiredHeaderReady()) return;
        pendingSearch = false;
        search();
    }

    private boolean isRequiredHeaderReady() {
        return isSheetMode() ? FjbHeaderInfo.isLoaded : LocationInfo.isLoaded;
    }

    private void requestRequiredHeaderNow() {
        HeaderLoader required = isSheetMode() ? sheetHeader : locationHeader;
        required.needsRefresh = true;
        required.start(true);
        updateHeaderLoadingStatus();
    }

    private ArrayList<LocationSpinnerAdapter.Option> buildLocationOptions(boolean includeCities) {
        ArrayList<LocationSpinnerAdapter.Option> options = new ArrayList<>();
        options.add(new LocationSpinnerAdapter.Option(
                getString(R.string.select_drop_down_standard),
                FjbHeaderInfo.getColumnColors(FjbHeaderInfo.COLUMN_NAME_PRONUNCIATION),
                FjbHeaderInfo.COLUMN_NAME_PRONUNCIATION
        ));
        options.add(new LocationSpinnerAdapter.Option(
                getString(R.string.select_drop_down_convenience),
                FjbHeaderInfo.getColumnColors(FjbHeaderInfo.COLUMN_NAME_RETRIEVAL),
                FjbHeaderInfo.COLUMN_NAME_RETRIEVAL
        ));
        if (includeCities) {
            String[] cityNames = FjbHeaderInfo.getCityList();
            for (int i = 0; i < cityNames.length; i++) {
                String column = FjbHeaderInfo.getCityNameByNumber(i);
                LocationSpinnerAdapter.Option cityOption = new LocationSpinnerAdapter.Option(
                        cityNames[i],
                        FjbHeaderInfo.getColumnColors(column),
                        column
                );
                options.add(cityOption);
            }
        }
        return options;
    }

    private LocationSpinnerAdapter.Option buildRecentLocationOption() {
        if (locationOptions.isEmpty()) return null;
        String recentColumn = sp.getString(
                LAST_LOCATION_COLUMN_KEY, FjbHeaderInfo.COLUMN_NAME_RETRIEVAL);
        LocationSpinnerAdapter.Option recentSource = null;
        for (LocationSpinnerAdapter.Option option : locationOptions) {
            if (option.queryColumn.equals(recentColumn)) {
                recentSource = option;
                break;
            }
        }
        if (recentSource == null) {
            recentSource = locationOptions.get(Math.min(
                    DEFAULT_LOCATION_POSITION, locationOptions.size() - 1));
        }
        return new LocationSpinnerAdapter.Option(
                getString(R.string.select_drop_down_recent, recentSource.label),
                recentSource.colors,
                recentSource.queryColumn
        );
    }

    private void updateFilterButtonLabels() {
        if (btnFilterArea == null || btnFilterAreaPron == null) return;

        int generalTotal = GeneralCharacterManager.cityList.size();
        int generalHidden = 0;
        for (String name : GeneralCharacterManager.cityList) {
            if (GeneralCharacterManager.cityFilter.contains(name)) generalHidden++;
        }
        int generalSelected = generalTotal - generalHidden;
        btnFilterArea.setText(getString(R.string.search_filtering_area_summary,
                selectionCountLabel(generalSelected, generalTotal)));

        int pronunciationTotal = LocationInfo.getAll().size() + 2;
        int pronunciationHidden = Math.min(pronunciationTotal,
                ResultFragment.pronCityFilter.size()
                        + ResultFragment.pronBookFilter.size());
        int pronunciationSelected = Math.max(0,
                pronunciationTotal - pronunciationHidden);
        btnFilterAreaPron.setText(getString(R.string.search_filtering_area_pron_summary,
                selectionCountLabel(pronunciationSelected, pronunciationTotal)));
    }

    private String selectionCountLabel(int selected, int total) {
        return total == 0 || selected == total
                ? getString(R.string.search_filter_all)
                : getString(R.string.search_filter_count, selected, total);
    }

    /**
     * 設置幾個開關的顯示與隱藏
     */
    private void setSearchView() {
        MotionUtil.beginLayoutTransition(binding.queryLayout);
        boolean is1Checked = isSheetMode();
        boolean is2Checked = switchQueryOptsRev.isChecked();
        boolean advancedSearchVisible = lyAdvancedSearch.getVisibility() == View.VISIBLE;
        if (is1Checked) {
            switchQueryOptsRev.setVisibility(View.VISIBLE);
            locationPicker.setVisibility(is2Checked ? View.GONE : View.VISIBLE);
            sheetQueryOptions.setVisibility(!is2Checked || advancedSearchVisible
                    ? View.VISIBLE : View.GONE);
            btnFilterArea.setVisibility(View.GONE);
            btnFilterAreaPron.setVisibility(View.GONE);
            btnColoringJppPartial.setVisibility(View.GONE);
        } else {
            switchQueryOptsRev.setVisibility(View.GONE);
            locationPicker.setVisibility(View.GONE);
            sheetQueryOptions.setVisibility(View.GONE);
            boolean isJpp = inputEditText.getText() != null && StringUtil.isJyutpingInput(inputEditText.getText().toString());
            btnFilterArea.setVisibility(isJpp ? View.GONE : View.VISIBLE);
            btnFilterAreaPron.setVisibility(isJpp ? View.VISIBLE : View.GONE);
            btnColoringJppPartial.setVisibility(View.VISIBLE);
        }
        switchQueryOptsRegex.setEnabled(is1Checked);
    }

    private boolean isSheetMode() {
        return sheetModeGroup.getCheckedRadioButtonId() == R.id.btn_jyut_sheet;
    }

    private void setSheetMode(boolean sheetMode) {
        sheetModeGroup.check(sheetMode ? R.id.btn_jyut_sheet : R.id.btn_common_sheet);
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
        editor.putBoolean("switch_1_is_checked", isSheetMode());
        editor.putBoolean("switch_2_is_checked", switchQueryOptsRev.isChecked());
        editor.putBoolean("switch_3_is_checked", switchQueryOptsRegex.isChecked());
        editor.putInt("spinner_selected_position", selectedLocationPosition);
        LocationSpinnerAdapter.Option selectedLocation = getSelectedLocationOption();
        if (selectedLocation != null) {
            editor.putString(LOCATION_SELECTION_COLUMN_KEY, selectedLocation.queryColumn);
        }
        editor.remove(LEGACY_LOCATION_SELECTION_RECENT_KEY);
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
                setSheetMode(false);
                break;
            case QUERYING_SHEET:
                setSheetMode(true);
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
        if (!isRequiredHeaderReady()) {
            pendingSearch = true;
            requestRequiredHeaderNow();
            ToastUtil.msg(this, getString(R.string.header_sync_required));
            return;
        }
        pendingSearch = false;
        setInputString(inputEditText.getText().toString()); // 必须放在最前面
        if ("".equals(inputString) && !(isSheetMode() && !switchQueryOptsRev.isChecked())) {
            return;
        } // 搜索欄爲空時不檢索

        ApiUrlBuilder url;
        int modeSnapshot;
        if (isSheetMode()) { // 檢索泛粵字表
            modeSnapshot = QUERYING_SHEET;
            url = ApiUrlBuilder.from(URL_API_ROOT, "sheet");
            if (inputString.isEmpty() && !switchQueryOptsRev.isChecked()) {
                url.add("random", 10);
            } else {
                url.add("q", inputString);
                boolean pronunciationInput = StringUtil.isSheetPronunciationInput(inputString)
                        && !switchQueryOptsRev.isChecked();
                String sheetMode;
                if (pronunciationInput) {
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

                LocationSpinnerAdapter.Option selectedLocation = getSelectedLocationOption();
                String selectedColumn = selectedLocation == null
                        ? FjbHeaderInfo.COLUMN_NAME_RETRIEVAL
                        : selectedLocation.queryColumn;

                // 漢字必須交由 API 自動選擇字頭列；只有查音纔傳讀音列。
                if (pronunciationInput
                        && !FjbHeaderInfo.COLUMN_NAME_PRONUNCIATION.equals(selectedColumn)) {
                    url.add("col", selectedColumn);
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
        searchViewModel.search(new Request(url.build(), responseMode));
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
                MotionUtil.beginLayoutTransition(binding.queryLayout);
                lyAdvancedSearch.setVisibility(isEnableAdvancedSearch ? View.VISIBLE : View.GONE);
                if (!isEnableAdvancedSearch) switchQueryOptsRegex.setChecked(false);
                setSearchView();

                boolean isToggleNightMode = (resultCode&0b10) != 0;
                if (isToggleNightMode) {
                    applyLightDarkTheme();
                    getIntent().putExtra(EXTRA_THEME_TRANSITION, true);
                    int surface = ContextCompat.getColor(this,
                            ThemeUtil.isNightMode(this)
                                    ? R.color.colorBackgroundDark
                                    : R.color.colorBackground);
                    MotionUtil.fadeThroughColor(
                            (ViewGroup) findViewById(android.R.id.content),
                            surface,
                            this::recreate);
                } else {
                    resultFragment.refreshResult();
                }
            });

    /****************************************************************************************/

    private void setInputEditTextHint() {
        if (isSheetMode()) {
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

    @Override
    protected void onStart() {
        super.onStart();
        headerRetriesEnabled = true;
        requestNeededHeaders();
    }

    @Override
    protected void onStop() {
        headerRetriesEnabled = false;
        sheetHeader.stop();
        locationHeader.stop();
        updateHeaderLoadingStatus();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (sheetHeader != null) sheetHeader.request.cancel();
        if (locationHeader != null) locationHeader.request.cancel();
        if (mainHandler != null) mainHandler.removeCallbacksAndMessages(null);
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
