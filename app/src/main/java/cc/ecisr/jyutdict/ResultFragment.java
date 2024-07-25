package cc.ecisr.jyutdict;

import static cc.ecisr.jyutdict.utils.EnumConst.*;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.ecisr.jyutdict.struct.FjbCharacter;
import cc.ecisr.jyutdict.struct.EntrySetting;
import cc.ecisr.jyutdict.struct.GeneralCharacterManager;
import cc.ecisr.jyutdict.utils.ToastUtil;

public class ResultFragment extends Fragment {
	private static final String TAG = "`ResultFragment";
	
	static private RecyclerView mRvMain;  // 不加static会显示两个View // a SHITTY method

	private String rawReceivedData;
	int receivedMode = QUERYING_CHARA;

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
					new AlertDialog.Builder(getContext())
							.setItems(selections, (dialogInterface, i) -> {
								if (i == 0) {
									View view = inflater.inflate(R.layout.layout_copy_alertdialog, null);
									TextView tv = view.findViewById(R.id.dialog_box_tv);
									tv.setText(holder.printContent());
									new AlertDialog.Builder(getContext())
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
		//mRvMain.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
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
		//mRvMain.getAdapter().notifyDataSetChanged();
		super.onViewCreated(view, savedInstanceState);
	}

	public void refreshResult() {
		if (rawReceivedData==null || rawReceivedData.isEmpty()) return;
		ResultItemAdapter.ResultInfo.clearItem();
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
		rawReceivedData = jsonString;
		receivedMode = queryObjectWhat;

		SharedPreferences sp = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
		EntrySetting entrySettings = new EntrySetting(QUERYING_SHEET)
				.setAreaColoringInfo(
						sp.getBoolean("area_coloring", true),
						sp.getFloat("area_coloring_darken_ratio", 0.92f))
				.setMeaningDomainPresence(
						sp.getBoolean("phrase_meaning_domain", false))
				.setUsingNightMode(sp.getBoolean("night_mode", false))
				.setPresentIpa(sp.getBoolean("ipa_presence", true));
		switch (queryObjectWhat & QUERYING_MODE_MASK) {
			case QUERYING_CHARA:
				GeneralCharacterManager gcm = new GeneralCharacterManager();
				gcm.parse(jsonString, entrySettings); /// 為什麼要重新解析一次原始字符串？真傻
				gcm.retrieveInfo();
				gcm.coloring(queryObjectWhat & DISPLAY_CHECKING_MASK);
				for (int i = 0; i<gcm.length(); i++) {
					Spanned[] spanneds = gcm.printChara(i);
					addItem(spanneds[0], spanneds[1], spanneds[2], spanneds[3], spanneds[4]);
				}
				break;
			case QUERYING_PRON:  // 下面邏輯將棄用，改用 CharacterManager 類作輸出，類似 QUERYING_CHARA
				StringBuilder contentLocation = new StringBuilder();
				StringBuilder contentWanshyu = new StringBuilder();
				JSONObject jsonObject = new JSONObject(jsonString);
				JSONArray jsonLocationsArray = jsonObject.getJSONArray("各地");
				JSONArray jsonWanshyusArray = jsonObject.getJSONArray("韻書");
				parseJsonPron(jsonLocationsArray, contentLocation, GETTING_CONTENT_LOCATION);
				parseJsonPron(jsonWanshyusArray, contentWanshyu, GETTING_CONTENT_WANSHYU);
				addItem(Html.fromHtml(""), Html.fromHtml(""), Html.fromHtml(""),
						Html.fromHtml(contentWanshyu.toString()),
						Html.fromHtml(contentLocation.toString()));
				break;
			case QUERYING_SHEET:
				FjbCharacter character; // TODO: Use ManagerClass like QUERYING_CHARA.
				JSONArray jsonArray = new JSONArray(jsonString);
				JSONObject entry;
				if (jsonArray.length() <= 1) {
					ToastUtil.msg(getContext(), getString(R.string.tips_no_result));
				}
				for (int i = 1; i<jsonArray.length(); i++) {
					entry = jsonArray.getJSONObject(i);
					character = new FjbCharacter(entry, entrySettings, getView());

					addItem(character.printCharacter(),
							character.printUnicode(),
							character.printPronunciation(),
							character.printMeanings(),
							character.printLocations()
					);
				}
				mRvMain.getAdapter().notifyItemRangeChanged(0, jsonArray.length());
				break;
			default:
				break;
		}
		mRvMain.getAdapter().notifyDataSetChanged();
	}
	
	/**
	 * 解析查音模式下的韻書音或地方音部分 json
	 *
	 * @deprecated 不再在這方法內解析，應改用 CharacterManager 類解析並輸出 spanned
	 * @see FjbCharacter
	 *
	 * @param jsonArray 韻書音或地方音部分的 json，如韻書音部分格式：
	 *                  [{"__name":"分韻","jing":{"1":"英瑛","4":"盈楹","2":"影暎","3":"應膺"}},
	 *                  {"__name":"英華","jing":{"4":"仍侀","1":"嬰鸚","2":"影","3":"應","6":"認"}}]
	 *                  應注意原始 json 字符串這部分調號很可能是亂序的，需要排序再輸出
	 *                  可能會存在「1'」「1*」這種非純數碼的調號，本方法忽視了這種情況
	 *                  但實際應將其排序在「1」後
	 * @param stringBuilder 儲存解析得到的 HTML 文本
	 * @param type 用以區分韻書音還是地方音
	 */
	private void parseJsonPron(JSONArray jsonArray, StringBuilder stringBuilder, int type) {
		try {
			String city = "", district = "", name = "";
			for (int i = 0; i < jsonArray.length(); i++) {
				StringBuilder contentInOneLocation = new StringBuilder();
				JSONObject syllablesInCity = jsonArray.getJSONObject(i);
				Iterator<String> syllablesInCityIterator = syllablesInCity.keys();
				contentInOneLocation.delete(0, contentInOneLocation.length());
				while (syllablesInCityIterator.hasNext()) {
					String syllablesInCityKey = syllablesInCityIterator.next();
					switch (syllablesInCityKey) {
						case "__city":
							city = syllablesInCity.getString(syllablesInCityKey);
							break;
						case "__district":
							district = syllablesInCity.getString(syllablesInCityKey);
							break;
						case "__name":
							name = syllablesInCity.getString(syllablesInCityKey);
							break;
						default:
							JSONObject syllables = syllablesInCity.getJSONObject(syllablesInCityKey);
							Iterator<String> syllablesIterator = syllables.keys();
							ArrayList<String> tones = new ArrayList<>();
							while (syllablesIterator.hasNext()) {
								tones.add(syllablesIterator.next());
							}
							String[] tones_ = tones.toArray(new String[0]);
							Arrays.sort(tones_);
							for (String tone: tones_) {
								contentInOneLocation.append("<br>").append(syllablesInCityKey).append(tone).append(": ").append(syllables.getString(tone));
							}
							break;
					}
				}
				if (contentInOneLocation.length() != 0) {
					if (stringBuilder.length() != 0) stringBuilder.append("<br>");
					switch (type) {
						case GETTING_CONTENT_LOCATION:
							stringBuilder.append("<b>").append(city).append(district).append("</b>");
							break;
						case GETTING_CONTENT_WANSHYU:
							stringBuilder.append("<b>").append(name).append("</b>");
							break;
						default:
							break;
					}
					stringBuilder.append(contentInOneLocation);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
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
	private void addItem(Spanned chara, Spanned leftMiddle, Spanned leftBottom, Spanned rightTop, Spanned rightBottom) {
		if (rightTop.length()!=0 || rightBottom.length()!=0) {
			ResultItemAdapter.ResultInfo.addItem(
					chara,
					leftMiddle,
					leftBottom,
					rightTop,
					rightBottom
			);
		}
	}
	
	
}
