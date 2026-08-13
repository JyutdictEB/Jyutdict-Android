package cc.ecisr.jyutdict;

import static cc.ecisr.jyutdict.utils.EnumConst.*;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.ecisr.jyutdict.struct.LocationInfo;
import android.text.SpannableStringBuilder;

import cc.ecisr.jyutdict.struct.FjbCharacter;
import cc.ecisr.jyutdict.struct.FjbHeaderInfo;
import cc.ecisr.jyutdict.struct.EntrySetting;
import cc.ecisr.jyutdict.struct.GeneralCharacterManager;
import cc.ecisr.jyutdict.utils.ColorUtil;
import cc.ecisr.jyutdict.utils.ThemeUtil;
import cc.ecisr.jyutdict.utils.ToastUtil;
import cc.ecisr.jyutdict.widget.LocationClickSpan;
import cc.ecisr.jyutdict.widget.LocationLabelSpan;

public class ResultFragment extends Fragment {
    private static final String TAG = "`ResultFragment";

    private RecyclerView mRvMain;

    private String rawReceivedData;
    int receivedMode = QUERYING_CHARA;
    public static HashSet<String> pronCityFilter = new HashSet<>();
    public static HashSet<String> pronBookFilter = new HashSet<>();

    // TODO 不 parse JSON in Fragment

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View selfView = inflater.inflate(R.layout.fragment_result, container, false);
        mRvMain = selfView.findViewById(R.id.result_list);
        ResultItemAdapter marketItemAdapter = new ResultItemAdapter(getActivity(), new ResultItemAdapter.iOnItemClickListener() {
            @Override
            public void onClick(@NonNull ResultItemAdapter.LinearViewHolder holder) {
                ArrayList<String> selectionList = new ArrayList<>();
                ArrayList<String> charaInWordsList = new ArrayList<>();
                selectionList.add(getString(R.string.entry_menu_copy_chara));

                final Pattern pt= Pattern.compile("((?<=〔[～~])[^～~]+?(?=〕))|((?<=〔)[^～~]+?(?=[～~]+?〕))");
                Matcher mt=pt.matcher(holder.tvRightTop.getText().toString());
                while (mt.find()){
                    charaInWordsList.add(mt.group(0));
                    selectionList.add(getString(R.string.entry_menu_search_common, mt.group(0)));
                    selectionList.add(getString(R.string.entry_menu_search_special, mt.group(0)));
                }

                if (!selectionList.isEmpty() && null != getActivity()) {
                    final String[] selections = selectionList.toArray(new String[0]);
                    new MaterialAlertDialogBuilder(getContext())
                            .setItems(selections, (dialogInterface, i) -> {
                                if (i == 0) {
                                    View view = inflater.inflate(R.layout.layout_copy_alertdialog, null);
                                    TextView tv = view.findViewById(R.id.dialog_box_tv);
                                    tv.setText(holder.printContent());
                                    new MaterialAlertDialogBuilder(getContext())
                                            .setView(view).setPositiveButton(R.string.button_confirm, null).show();
                                } else {
                                    int elseItemAddedCount = 1;
                                    int mode = (i % 2 == 1) ?
                                            QUERYING_CHARA :
                                            QUERYING_SHEET;
                                    ((MainActivity) getActivity()).search(
                                            charaInWordsList.get((i - 1) >> elseItemAddedCount),
                                            mode);
                                }
                            }).create().show();
                }
            }

            @Override
            public void onLongClick(@NonNull ResultItemAdapter.LinearViewHolder holder) {
                if (!holder.getChara().isEmpty() && getActivity()!=null) {
                    copy(holder.getChara());
                }
            }
        });
        mRvMain.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRvMain.setItemAnimator(new DefaultItemAnimator());
        mRvMain.setAdapter(marketItemAdapter);
        if (savedInstanceState!= null) {
            rawReceivedData = savedInstanceState.getString("received_data");
            receivedMode = savedInstanceState.getInt("received_mode");
            if (!"".equals(rawReceivedData)) { refreshResult(); }
        }
        return selfView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG, "onSaveInstanceState: " + System.identityHashCode(this));
        super.onSaveInstanceState(outState);
        outState.putString("received_data", rawReceivedData);
        outState.putInt("received_mode", receivedMode);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated: " + System.identityHashCode(this));
        super.onViewCreated(view, savedInstanceState);
    }

    public void refreshResult() {
        if (rawReceivedData==null || rawReceivedData.isEmpty()) return;
        try {
            parseJson(rawReceivedData, receivedMode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void refreshResult(int receivedModeConfig) {
        receivedMode = (receivedMode&QUERYING_MODE_MASK) | receivedModeConfig;
        refreshResult();
    }

    private void copy(String chara) {
        if (getActivity() == null) return;
        ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData mClipData = ClipData.newPlainText("jyut_chara", chara);
        if (cm != null) {
            cm.setPrimaryClip(mClipData);
            ToastUtil.msg(getContext(), getString(R.string.tips_chara_copied, chara));
        }
    }

    /**
     * 將服務器返回的 JSON 字符串處理成可閱讀的樣式，並顯示出來
     * 在 {@code MainActivity} 成功收到查詢回應（JSON 字符串）時調用
     * 在本方法解析 JSON 字符串生成每個條目的五個 layout 的 spanned，再交由 {@code ResultItemAdapter} 顯示
     * 查詢通用字表的兩個模式（查字&查音）用的是內嵌的解析，且生成的是 HTML 格式。應棄用
     * TODO 好像會發生內存洩露？
     *
     * @param jsonString 服務器返回的 JSON 字符串
     * @param queryObjectWhat 查詢模式(通用表查字/查音/查泛粵表 等)，值在 {@code EnumConst} 類中定義
     * @see cc.ecisr.jyutdict.utils.EnumConst
     */
    void parseJson(String jsonString, int queryObjectWhat) throws JSONException {
        if (getActivity()==null) return;
        if (mRvMain.getAdapter() == null) return;

        int mode = queryObjectWhat & QUERYING_MODE_MASK;
        if (mode == QUERYING_PRON) {
            new JSONObject(jsonString);
        } else {
            new JSONArray(jsonString);
        }

        ArrayList<ArrayList<Spanned>> previousItems =
                new ArrayList<>(ResultItemAdapter.ResultInfo.list);
        ArrayList<Integer> previousTypes =
                new ArrayList<>(ResultItemAdapter.ResultInfo.types);
        ResultItemAdapter.ResultInfo.clearItem();

        try {
            parseJsonValidated(jsonString, queryObjectWhat);
            rawReceivedData = jsonString;
            receivedMode = queryObjectWhat;
        } catch (JSONException | RuntimeException e) {
            ResultItemAdapter.ResultInfo.list.clear();
            ResultItemAdapter.ResultInfo.list.addAll(previousItems);
            ResultItemAdapter.ResultInfo.types.clear();
            ResultItemAdapter.ResultInfo.types.addAll(previousTypes);
            publishResultViews();
            throw e;
        }
    }

    private void parseJsonValidated(String jsonString, int queryObjectWhat) throws JSONException {

        SharedPreferences sp = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        EntrySetting entrySettings = new EntrySetting(QUERYING_SHEET)
                .setAreaColoringInfo(
                        sp.getBoolean("area_coloring", true),
                        sp.getFloat("area_coloring_darken_ratio", 0.92f))
                .setMeaningDomainPresence(
                        sp.getBoolean("phrase_meaning_domain", false))
                .setUsingNightMode(ThemeUtil.isNightMode(getActivity()))
                .setPresentIpa(sp.getBoolean("ipa_presence", true));
        switch (queryObjectWhat & QUERYING_MODE_MASK) {
            case QUERYING_CHARA:
                GeneralCharacterManager gcm = new GeneralCharacterManager();
                gcm.parse(jsonString, entrySettings); /// 為什麼要重新解析一次原始字符串？真傻
                gcm.retrieveInfo();
                gcm.coloring(queryObjectWhat & DISPLAY_CHECKING_MASK);
                for (int i = 0; i<gcm.length(); i++) {
                    Spanned[] spanneds = gcm.printChara(i);
                    addItem(spanneds[0], spanneds[1], spanneds[2], spanneds[3], spanneds[4],
                            ResultItemAdapter.ResultInfo.TYPE_GENERAL);
                }
                break;
            case QUERYING_PRON:
                parseJsonPron(jsonString, entrySettings);
                break;
            case QUERYING_SHEET:
                FjbCharacter character; // TODO: Use ManagerClass like QUERYING_CHARA.
                JSONArray jsonArray = new JSONArray(jsonString);
                ArrayList<JSONObject> sortedEntries = new ArrayList<>(jsonArray.length());
                if (jsonArray.length() == 0) {
                    ToastUtil.msg(getContext(), getString(R.string.tips_no_result));
                }
                for (int i = 0; i < jsonArray.length(); i++) {
                    sortedEntries.add(jsonArray.getJSONObject(i));
                }
                // 與網頁端一致：地方讀音覆蓋度越高的條目越靠前；TimSort 保持同分原序。
                Collections.sort(sortedEntries, (left, right) -> Integer.compare(
                        FjbHeaderInfo.densityScore(right),
                        FjbHeaderInfo.densityScore(left)
                ));
                for (JSONObject entry : sortedEntries) {
                    character = new FjbCharacter(entry, entrySettings, mRvMain);

                    addItem(character.printCharacter(),
                            character.printUnicode(),
                            character.printPronunciation(),
                            character.printMeanings(),
                            character.printLocations(),
                            ResultItemAdapter.ResultInfo.TYPE_SHEET
                    );
                }
                break;
            default:
                break;
        }
        publishResultViews();
    }

    /**
     * Selectable TextView 的 Editor/ActionMode 狀態會跟隨 ViewHolder 留在回收池中。
     * 同模式再次查詢若只 notifyDataSetChanged，重綁後的文字可能再也無法長按選取；
     * 發佈一批新結果時丟棄舊 holder，確保每次查詢都使用全新的選取狀態。
     */
    private void publishResultViews() {
        RecyclerView.Adapter<?> adapter = mRvMain.getAdapter();
        if (adapter == null) return;
        mRvMain.stopScroll();
        mRvMain.setAdapter(null);
        mRvMain.getRecycledViewPool().clear();
        mRvMain.setAdapter(adapter);
        if (adapter.getItemCount() > 0) {
            mRvMain.scrollToPosition(0);
        }
    }



    /**
     * 向 ResultItemAdapter 添加一項條文
     *
     * @param chara layout 中的左上部分
     *              僅用於顯示字頭
     * @param leftMiddle layout 中的左中部分
     *                   顯示廣韻音（通語表）或統一碼（泛粵表）
     * @param leftBottom layout 中的左下部分
     *                   顯示綜合音
     * @param rightTop layout 中的右上部分
     *                 顯示韻書（通用表）或釋義（泛粵表）
     * @param rightBottom layout 中的右下部分
     *                    顯示地方音
     */
    private void addItem(Spanned chara, Spanned leftMiddle, Spanned leftBottom,
                         Spanned rightTop, Spanned rightBottom, int type) {
        if (rightTop.length()!=0 || rightBottom.length()!=0) {
            ResultItemAdapter.ResultInfo.addItem(
                    chara,
                    leftMiddle,
                    leftBottom,
                    rightTop,
                    rightBottom,
                    type
            );
        }
    }

    /**
     * 解析並顯示 v1.0 檢音 API 返回的結果
     *
     * @param jsonString 服務器返回的 JSON 字符串
     */
    private void parseJsonPron(String jsonString, EntrySetting entrySettings) throws JSONException {
        JSONObject root = new JSONObject(jsonString);
        JSONArray wanshyuArray = root.optJSONArray("韻書");
        JSONArray areasArray = root.optJSONArray("各地");

        // 解析韻書
        if (wanshyuArray != null) {
            for (int i = 0; i < wanshyuArray.length(); i++) {
                JSONObject obj = wanshyuArray.optJSONObject(i);
                if (obj == null) continue;
                String bookName = obj.optString("__name", "韻書");
                if (pronBookFilter.contains(bookName)) continue;
                SpannableStringBuilder bookNameSsb = new SpannableStringBuilder(bookName);
                SpannableStringBuilder pronListSsb = new SpannableStringBuilder();

                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String syllable = keys.next();
                    if ("__name".equals(syllable)) continue;
                    JSONObject tones = obj.optJSONObject(syllable);
                    if (tones == null) continue;

                    Iterator<String> toneKeys = tones.keys();
                    while (toneKeys.hasNext()) {
                        String tone = toneKeys.next();
                        String chars = tones.optString(tone, "");
                        if (!chars.isEmpty()) {
                            if (pronListSsb.length() > 0) pronListSsb.append("\n");
                            pronListSsb.append(syllable).append(tone).append(": ").append(chars);
                        }
                    }
                }
                if (pronListSsb.length() > 0) {
                    addItem(new SpannableStringBuilder(), new SpannableStringBuilder(),
                            new SpannableStringBuilder(), bookNameSsb, pronListSsb,
                            ResultItemAdapter.ResultInfo.TYPE_GENERAL);
                }
            }
        }

        // 解析各地
        if (areasArray != null) {
            for (int i = 0; i < areasArray.length(); i++) {
                JSONObject obj = areasArray.optJSONObject(i);
                if (obj == null) continue;

                int locId = obj.optInt("__id", -1);
                if (locId != -1 && pronCityFilter.contains(String.valueOf(locId))) {
                    continue; // 被篩選掉
                }

                LocationInfo.Location loc = LocationInfo.get(locId);
                String cityName;
                if (loc != null) {
                    cityName = loc.displayName();
                } else {
                    cityName = "id=" + locId;
                }

                SpannableStringBuilder cityNameSsb = new SpannableStringBuilder(cityName);
                if (loc != null && cityNameSsb.length() > 0) {
                    int[] locationColors = new int[entrySettings.isAreaColoring()
                            ? loc.colors.size()
                            : 0];
                    double ratio = entrySettings.isUsingNightMode()
                            ? 2 - entrySettings.getAreaColoringDarkenRatio()
                            : entrySettings.getAreaColoringDarkenRatio();
                    for (int colorIndex = 0; colorIndex < locationColors.length; colorIndex++) {
                        locationColors[colorIndex] = ColorUtil.darken(
                                loc.colors.get(colorIndex),
                                ratio
                        );
                    }
                    cityNameSsb.setSpan(
                            new LocationLabelSpan(cityName, locationColors),
                            0,
                            cityNameSsb.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    cityNameSsb.setSpan(
                            new LocationClickSpan(loc.id),
                            0,
                            cityNameSsb.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
                SpannableStringBuilder pronListSsb = new SpannableStringBuilder();

                Iterator<String> keys = obj.keys();
                boolean hasData = false;
                while (keys.hasNext()) {
                    String syllable = keys.next();
                    if ("__id".equals(syllable)) continue;
                    JSONObject tones = obj.optJSONObject(syllable);
                    if (tones == null) continue;

                    Iterator<String> toneKeys = tones.keys();
                    while (toneKeys.hasNext()) {
                        String tone = toneKeys.next();
                        String chars = tones.optString(tone, "");
                        if (!chars.isEmpty()) {
                            if (pronListSsb.length() > 0) pronListSsb.append("\n");
                            pronListSsb.append(syllable).append(tone).append(": ").append(chars);
                            hasData = true;
                        }
                    }
                }
                
                if (hasData) {
                    addItem(new SpannableStringBuilder(), new SpannableStringBuilder(),
                            new SpannableStringBuilder(), cityNameSsb, pronListSsb,
                            ResultItemAdapter.ResultInfo.TYPE_GENERAL);
                }
            }
        }
        
        if (wanshyuArray == null && areasArray == null) {
            ToastUtil.msg(getContext(), getString(R.string.tips_no_result));
        }
    }


}
