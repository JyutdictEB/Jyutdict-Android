package cc.ecisr.jyutdict;

import static cc.ecisr.jyutdict.utils.EnumConst.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.ecisr.jyutdict.comments.CommentDialogFragment;
import cc.ecisr.jyutdict.comments.CommentRepository;
import cc.ecisr.jyutdict.databinding.FragmentResultBinding;
import cc.ecisr.jyutdict.databinding.LayoutCopyAlertdialogBinding;
import cc.ecisr.jyutdict.struct.LocationInfo;
import android.text.SpannableStringBuilder;

import cc.ecisr.jyutdict.struct.FjbCharacter;
import cc.ecisr.jyutdict.struct.FjbHeaderInfo;
import cc.ecisr.jyutdict.struct.EntrySetting;
import cc.ecisr.jyutdict.struct.GeneralCharacterManager;
import cc.ecisr.jyutdict.utils.ColorUtil;
import cc.ecisr.jyutdict.utils.ImmersiveBarUtil;
import cc.ecisr.jyutdict.utils.MotionUtil;
import cc.ecisr.jyutdict.utils.ThemeUtil;
import cc.ecisr.jyutdict.utils.ToastUtil;
import cc.ecisr.jyutdict.widget.LocationClickSpan;
import cc.ecisr.jyutdict.widget.LocationLabelSpan;

public class ResultFragment extends Fragment {
    private static final String TAG = "`ResultFragment";
    private static final Pattern WORD_PATTERN = Pattern.compile(
            "((?<=〔[～~])[^～~]+?(?=〕))|((?<=〔)[^～~]+?(?=[～~]+?〕))");

    private RecyclerView mRvMain;
    private FragmentResultBinding binding;
    private ResultItemAdapter resultAdapter;
    private boolean resultRevealRunning;
    private CommentRepository commentRepository;

    private String rawReceivedData;
    int receivedMode = QUERYING_CHARA;
    public static HashSet<String> pronCityFilter = new HashSet<>();
    public static HashSet<String> pronBookFilter = new HashSet<>();

    // TODO 不 parse JSON in Fragment

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentResultBinding.inflate(inflater, container, false);
        mRvMain = binding.resultList;
        ImmersiveBarUtil.applyBottomInsets(mRvMain);
        commentRepository = new CommentRepository(requireContext());
        resultAdapter = new ResultItemAdapter(this::showItemMenu);
        mRvMain.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRvMain.setItemAnimator(null);
        mRvMain.setAdapter(resultAdapter);
        if (savedInstanceState!= null) {
            rawReceivedData = savedInstanceState.getString("received_data");
            receivedMode = savedInstanceState.getInt("received_mode");
        }
        if (rawReceivedData != null && !rawReceivedData.isEmpty()) refreshResult();
        return binding.getRoot();
    }

    private void showItemMenu(ResultItemAdapter.LinearViewHolder holder) {
        ArrayList<String> choices = new ArrayList<>();
        ArrayList<String> characters = new ArrayList<>();
        choices.add(getString(R.string.entry_menu_copy_chara));
        Matcher matcher = WORD_PATTERN.matcher(holder.getWanshyuText());
        while (matcher.find()) {
            String character = matcher.group();
            characters.add(character);
            choices.add(getString(R.string.entry_menu_search_common, character));
            choices.add(getString(R.string.entry_menu_search_special, character));
        }

        ResultItemAdapter.ResultInfo.CommentTarget comment = holder.getCommentTarget();
        int commentIndex = -1;
        if (comment != null && !comment.target.isEmpty()) {
            commentIndex = choices.size();
            choices.add(comment.countLoaded
                    ? getString(R.string.comment_button_count, comment.count)
                    : getString(R.string.comment_button));
        }
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        int finalCommentIndex = commentIndex;
        new AlertDialog.Builder(activity).setItems(choices.toArray(new String[0]), (dialog, index) -> {
            if (index == 0) {
                LayoutCopyAlertdialogBinding copy =
                        LayoutCopyAlertdialogBinding.inflate(getLayoutInflater());
                copy.dialogBoxTv.setText(holder.printContent());
                new AlertDialog.Builder(activity).setView(copy.getRoot())
                        .setPositiveButton(R.string.button_confirm, null).show();
            } else if (index == finalCommentIndex) {
                CommentDialogFragment.newInstance(comment.type, comment.target, holder.getChara())
                        .show(getParentFragmentManager(), "comments");
            } else {
                activity.search(characters.get((index - 1) >> 1),
                        index % 2 == 1 ? QUERYING_CHARA : QUERYING_SHEET);
            }
        }).show();
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
        getParentFragmentManager().setFragmentResultListener(
                CommentDialogFragment.RESULT_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> updateCommentCount(
                        result.getString(CommentDialogFragment.RESULT_TYPE, ""),
                        result.getString(CommentDialogFragment.RESULT_TARGET, ""),
                        result.getInt(CommentDialogFragment.RESULT_COUNT, 0)));
    }

    public void refreshResult() {
        if (rawReceivedData==null || rawReceivedData.isEmpty()) return;
        try {
            parseJson(rawReceivedData, receivedMode, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void refreshResult(int receivedModeConfig) {
        receivedMode = (receivedMode&QUERYING_MODE_MASK) | receivedModeConfig;
        refreshResult();
    }

    public void beginLoading() {
        if (mRvMain == null) return;
        resultRevealRunning = false;
        MotionUtil.fadeTo(mRvMain, 0.55f);
    }

    public void finishLoading() {
        if (mRvMain == null) return;
        if (resultRevealRunning) {
            resultRevealRunning = false;
            return;
        }
        mRvMain.setTranslationY(0f);
        MotionUtil.fadeTo(mRvMain, 1f);
    }

    @Override
    public void onDestroyView() {
        if (mRvMain != null) {
            mRvMain.animate().cancel();
            mRvMain.setAdapter(null);
        }
        resultAdapter = null;
        mRvMain = null;
        binding = null;
        super.onDestroyView();
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
        parseJson(jsonString, queryObjectWhat, true);
    }

    private void parseJson(String jsonString, int queryObjectWhat, boolean revealNewResult)
            throws JSONException {
        if (getActivity()==null) return;
        if (resultAdapter == null) return;

        ArrayList<ResultItemAdapter.ResultInfo> parsedItems =
                parseJsonValidated(jsonString, queryObjectWhat);
        resultAdapter.replaceItems(parsedItems);
        rawReceivedData = jsonString;
        receivedMode = queryObjectWhat;
        publishResultViews(revealNewResult);
        loadCommentCounts();
    }

    private ArrayList<ResultItemAdapter.ResultInfo> parseJsonValidated(
            String jsonString, int queryObjectWhat) throws JSONException {

        SharedPreferences sp = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        ArrayList<ResultItemAdapter.ResultInfo> parsedItems = new ArrayList<>();
        EntrySetting entrySettings = new EntrySetting()
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
                    addItem(parsedItems, spanneds[0], spanneds[1], spanneds[2],
                            spanneds[3], spanneds[4],
                            ResultItemAdapter.ResultInfo.TYPE_GENERAL,
                            CommentRepository.TYPE_CHAR, spanneds[0].toString());
                }
                break;
            case QUERYING_PRON:
                parseJsonPron(jsonString, entrySettings, parsedItems);
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
                    character = new FjbCharacter(entry, entrySettings);

                    addItem(parsedItems, character.printCharacter(),
                            character.printUnicode(),
                            character.printPronunciation(),
                            character.printMeanings(),
                            character.printLocations(),
                            ResultItemAdapter.ResultInfo.TYPE_SHEET,
                            CommentRepository.TYPE_SHEET,
                            entry.optString("鍵", "")
                    );
                }
                break;
            default:
                break;
        }
        return parsedItems;
    }

    /**
     * Selectable TextView 的 Editor/ActionMode 狀態會跟隨 ViewHolder 留在回收池中。
     * 同模式再次查詢若只 notifyDataSetChanged，重綁後的文字可能再也無法長按選取；
     * 發佈一批新結果時丟棄舊 holder，確保每次查詢都使用全新的選取狀態。
     */
    @SuppressLint("NotifyDataSetChanged")
    private void publishResultViews(boolean revealNewResult) {
        if (mRvMain == null || resultAdapter == null) return;
        mRvMain.stopScroll();
        if (mRvMain.getItemAnimator() != null) {
            mRvMain.getItemAnimator().endAnimations();
        }
        resultAdapter.notifyDataSetChanged();
        if (!revealNewResult) {
            resultRevealRunning = false;
            mRvMain.animate().cancel();
            mRvMain.setAlpha(1f);
            mRvMain.setTranslationY(0f);
            return;
        }
        if (resultAdapter.getItemCount() > 0) {
            mRvMain.scrollToPosition(0);
        }
        resultRevealRunning = true;
        MotionUtil.reveal(mRvMain);
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
    private void addItem(ArrayList<ResultItemAdapter.ResultInfo> items,
                         Spanned chara, Spanned leftMiddle, Spanned leftBottom,
                         Spanned rightTop, Spanned rightBottom, int type) {
        addItem(items, chara, leftMiddle, leftBottom, rightTop, rightBottom,
                type, null, null);
    }

    private void addItem(ArrayList<ResultItemAdapter.ResultInfo> items,
                         Spanned chara, Spanned leftMiddle, Spanned leftBottom,
                         Spanned rightTop, Spanned rightBottom, int type,
                         String commentType, String commentTarget) {
        if (rightTop.length()!=0 || rightBottom.length()!=0) {
            items.add(new ResultItemAdapter.ResultInfo(
                    chara,
                    leftMiddle,
                    leftBottom,
                    rightTop,
                    rightBottom,
                    type,
                    commentType,
                    commentTarget
            ));
        }
    }

    private void loadCommentCounts() {
        LinkedHashSet<String> charTargets = new LinkedHashSet<>();
        LinkedHashSet<String> sheetTargets = new LinkedHashSet<>();
        if (resultAdapter == null) return;
        for (int index = 0; index < resultAdapter.getItemCount(); index++) {
            ResultItemAdapter.ResultInfo.CommentTarget metadata =
                    resultAdapter.getItem(index).commentTarget;
            if (metadata == null || metadata.target.isEmpty()) continue;
            if (CommentRepository.TYPE_CHAR.equals(metadata.type)) {
                charTargets.add(metadata.target);
            } else if (CommentRepository.TYPE_SHEET.equals(metadata.type)) {
                sheetTargets.add(metadata.target);
            }
        }
        requestCommentCounts(CommentRepository.TYPE_CHAR, new ArrayList<>(charTargets));
        requestCommentCounts(CommentRepository.TYPE_SHEET, new ArrayList<>(sheetTargets));
    }

    private void requestCommentCounts(String type, List<String> targets) {
        if (targets.isEmpty()) return;
        commentRepository.getCounts(type, targets, (counts, errorMessage) -> {
            if (!isAdded() || counts == null || mRvMain == null
                    || resultAdapter == null) return;
            for (int index = 0; index < resultAdapter.getItemCount(); index++) {
                ResultItemAdapter.ResultInfo.CommentTarget metadata =
                        resultAdapter.getItem(index).commentTarget;
                if (metadata == null || !type.equals(metadata.type)) continue;
                Integer count = counts.get(metadata.target);
                if (count == null) continue;
                metadata.count = count;
                metadata.countLoaded = true;
                resultAdapter.notifyItemChanged(index);
            }
        });
    }

    private void updateCommentCount(String type, String target, int count) {
        if (resultAdapter == null) return;
        for (int index = 0; index < resultAdapter.getItemCount(); index++) {
            ResultItemAdapter.ResultInfo.CommentTarget metadata =
                    resultAdapter.getItem(index).commentTarget;
            if (metadata == null || !type.equals(metadata.type) || !target.equals(metadata.target)) {
                continue;
            }
            metadata.count = count;
            metadata.countLoaded = true;
            resultAdapter.notifyItemChanged(index);
        }
    }

    private void parseJsonPron(String jsonString, EntrySetting entrySettings,
                               ArrayList<ResultItemAdapter.ResultInfo> parsedItems)
            throws JSONException {
        JSONObject root = new JSONObject(jsonString);
        JSONArray books = root.optJSONArray("韻書"), areas = root.optJSONArray("各地");
        if (books != null) {
            for (int index = 0; index < books.length(); index++) {
                JSONObject book = books.optJSONObject(index);
                if (book == null) continue;
                String name = book.optString("__name", "韻書");
                if (!pronBookFilter.contains(name)) {
                    addPronunciationItem(parsedItems, new SpannableStringBuilder(name),
                            formatPronunciations(book, "__name"));
                }
            }
        }
        if (areas != null) {
            for (int index = 0; index < areas.length(); index++) {
                JSONObject area = areas.optJSONObject(index);
                if (area == null) continue;
                int locationId = area.optInt("__id", -1);
                if (locationId != -1 && pronCityFilter.contains(String.valueOf(locationId))) {
                    continue;
                }
                addPronunciationItem(parsedItems,
                        locationLabel(LocationInfo.get(locationId), locationId, entrySettings),
                        formatPronunciations(area, "__id"));
            }
        }
        if (books == null && areas == null) {
            ToastUtil.msg(getContext(), getString(R.string.tips_no_result));
        }
    }

    private SpannableStringBuilder formatPronunciations(JSONObject source, String metadataKey) {
        SpannableStringBuilder result = new SpannableStringBuilder();
        Iterator<String> syllables = source.keys();
        while (syllables.hasNext()) {
            String syllable = syllables.next();
            if (metadataKey.equals(syllable)) continue;
            JSONObject tones = source.optJSONObject(syllable);
            if (tones == null) continue;
            Iterator<String> toneKeys = tones.keys();
            while (toneKeys.hasNext()) {
                String tone = toneKeys.next();
                String characters = tones.optString(tone, "");
                if (characters.isEmpty()) continue;
                if (result.length() > 0) result.append("\n");
                result.append(syllable).append(tone).append(": ").append(characters);
            }
        }
        return result;
    }

    private SpannableStringBuilder locationLabel(LocationInfo.Location location, int id,
                                                  EntrySetting settings) {
        String name = location == null ? "id=" + id : location.displayName();
        SpannableStringBuilder result = new SpannableStringBuilder(name);
        if (location == null || name.isEmpty()) return result;
        double ratio = settings.isUsingNightMode()
                ? 2 - settings.getAreaColoringDarkenRatio()
                : settings.getAreaColoringDarkenRatio();
        int[] colors = settings.isAreaColoring()
                ? ColorUtil.locationColorInts(location.colors, ratio) : new int[0];
        result.setSpan(new LocationLabelSpan(name, colors), 0, result.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        result.setSpan(new LocationClickSpan(location.id), 0, result.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return result;
    }

    private void addPronunciationItem(ArrayList<ResultItemAdapter.ResultInfo> items,
                                      Spanned label, Spanned pronunciations) {
        if (pronunciations.length() == 0) return;
        addItem(items, new SpannableStringBuilder(), new SpannableStringBuilder(),
                new SpannableStringBuilder(), label, pronunciations,
                ResultItemAdapter.ResultInfo.TYPE_GENERAL);
    }

}
