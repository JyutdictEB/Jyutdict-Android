package cc.ecisr.jyutdict;

import android.app.AlertDialog;
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
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.ecisr.jyutdict.struct.Character;
import cc.ecisr.jyutdict.struct.EntrySetting;
import cc.ecisr.jyutdict.utils.EnumConst;
import cc.ecisr.jyutdict.utils.ToastUtil;

public class ResultFragment extends Fragment {
	private static final String TAG = "`ResultFragment";
	
	static private RecyclerView mRvMain;  // 不加static会显示两个View // a SHITTY method
	private View selfView;
	
	// TODO 不 parse JSON in Fragment
	// TODO 高度不會變動的滾動條 // 需要自定義滾動條類
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if (selfView != null) { // 避免重新設置view
			ViewGroup parent = (ViewGroup) selfView.getParent();
			if (parent != null) parent.removeView(selfView);
			return selfView;
		}
		
		if (mRvMain == null) {
			selfView = inflater.inflate(R.layout.fragment_result, container, false);
			mRvMain = selfView.findViewById(R.id.result_list);
			ResultItemAdapter marketItemAdapter = new ResultItemAdapter(getActivity(), new ResultItemAdapter.OnItemClickListener() {
				@Override
				public void onClick(@NonNull ResultItemAdapter.LinearViewHolder holder) {
					ArrayList<String> selectionList = new ArrayList<>();
					ArrayList<String> charaInWordsList = new ArrayList<>();
					
					final String chara = holder.getChara();
					final boolean isCopiable = chara.length() != 0 && !"□".equals(chara);
					if (isCopiable) {
						selectionList.add(getString(R.string.entry_menu_copy_chara, chara));
					}
					
					final Pattern pt= Pattern.compile("((?<=（[～~])[^～~]+?(?=）))|((?<=（)[^～~]+?(?=[～~]+?）))");
					Matcher mt=pt.matcher(holder.tvRightTop.getText().toString());
					while (mt.find()){
						charaInWordsList.add(mt.group(0));
						selectionList.add(getString(R.string.entry_menu_search_common, mt.group(0)));
						selectionList.add(getString(R.string.entry_menu_search_special, mt.group(0)));
					}
					
					if (selectionList.size()!=0) {
						final String[] selections = selectionList.toArray(new String[0]);
						new AlertDialog.Builder(getContext())
								.setItems(selections, (dialogInterface, i) -> {
									if (i == 0 && isCopiable) {
										copy(chara);
									} else {
										int elseItemAddedCount = (isCopiable ? 1 : 0);
										int mode = (i % 2 == elseItemAddedCount) ?
												EnumConst.QUERYING_CHARA :
												EnumConst.QUERYING_SHEET;
										((MainActivity) getActivity()).search(
												charaInWordsList.get((i - 1) >> elseItemAddedCount),
												mode);
									}
								}).create().show();
					}
				}
				
				@Override
				public void onLongClick(@NonNull ResultItemAdapter.LinearViewHolder holder) {
					if (holder.getChara().length()!=0 && getActivity()!=null) {
						copy(holder.getChara());
					}
				}
			});
			mRvMain.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
			mRvMain.setItemAnimator(new DefaultItemAnimator());
			mRvMain.setAdapter(marketItemAdapter);
//			if (getActivity() != null) {
//				mRvMain.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
//			}
//			Log.i(TAG, "onCreateView: 1");
		} else if (savedInstanceState!=null) {
//			mRvMain = (RecyclerView) savedInstanceState.get("rv_main");
//			Log.i(TAG, "onCreateView: 2");
		} else {
//			Log.i(TAG, "onCreateView: 3");
		}
		Log.d(TAG, "onCreateView: " + System.identityHashCode(this));
		return selfView;
	}
	
	private void copy(String chara) {
		ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData mClipData = ClipData.newPlainText("jyut_chara", chara);
		if (cm != null) {
			cm.setPrimaryClip(mClipData);
			ToastUtil.msg(getContext(), getString(R.string.tips_chara_copied, chara));
		}
	}
	
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		Log.d(TAG, "onSaveInstanceState: ");
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onDestroyView() {
		Log.d(TAG, "onDestroyView: " + System.identityHashCode(this));
		super.onDestroyView();
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy: ");
		super.onDestroy();
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		Log.d(TAG, "onViewCreated: ");
		super.onViewCreated(view, savedInstanceState);
	}
	
	/**
	 * 將服務器返回的 JSON 字符串處理成可閱讀的樣式，並顯示出來
	 *
	 * 在 {@code MainActivity} 成功收到查詢回應（JSON 字符串）時調用
	 * 在本方法解析 JSON 字符串生成每個條目的五個 layout 的 spanned，再交由 {@code ResultItemAdapter} 顯示
	 * 查詢通用字表的兩個模式（查字&查音）用的是內嵌的解析，且生成的是 HTML 格式。應棄用
	 *
	 * TODO 全部使用 Character 類作來
	 * TODO 好像會發生內存洩露？
	 *
	 * @param jsonString 服務器返回的 JSON 字符串
	 * @param queryObjectWhat 查詢模式(通用表查字/查音/查泛粵表 等)，值在 {@code EnumConst} 類中定義
	 * @see EnumConst
	 */
	void parseJson(String jsonString, int queryObjectWhat) {
		if (getActivity()==null) return;
		try {
			if (mRvMain.getAdapter() == null) return;
			mRvMain.getAdapter().notifyDataSetChanged();
			ResultItemAdapter.ResultInfo.clearItem();
			StringBuilder contentLocation = new StringBuilder();
			StringBuilder contentWanshyu = new StringBuilder();
			StringBuilder contentCharaInfo = new StringBuilder();
			SharedPreferences sp = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
			Character character;
			JSONArray jsonArray;
			switch (queryObjectWhat) {
				case EnumConst.QUERYING_CHARA: // 下面邏輯將棄用，改用 Character 類作輸出，類似 QUERYING_SHEET
					jsonArray = new JSONArray(jsonString);
					for (int i = 0; i<jsonArray.length(); i++) {
						JSONObject jsonObject = jsonArray.getJSONObject(i);
						JSONArray jsonLocationsArray = jsonObject.getJSONArray("各地");
						String chara = jsonObject.getString("字");
						for (int j = 0; j<jsonLocationsArray.length(); j++) {
							JSONArray jsonLocationArray = jsonLocationsArray.getJSONArray(j);
							String city, district, jyutping, ipa, note;
							for (int k = 0; k<jsonLocationArray.length(); k++) {
								JSONObject jsonLocationInfo = jsonLocationArray.getJSONObject(k);
								city = jsonLocationInfo.getString("市");
								district = jsonLocationInfo.getString("管區");
								jyutping = jsonLocationInfo.getString("聲母") +
										jsonLocationInfo.getString("韻核") +
										jsonLocationInfo.getString("韻尾") +
										jsonLocationInfo.getString("聲調");
								ipa = jsonLocationInfo.getString("IPA");
								note = jsonLocationInfo.getString("註");
								if (contentLocation.length()!=0) contentLocation.append("<br>");
								if (k==0) contentLocation.append(city).append(district).append("\t");
								else contentLocation.append("　　\t");
								contentLocation.append(jyutping).append("\t/").append(ipa).append("/\t").append(note);
								
							}
						}
						
						JSONArray jsonWanshyusArray = jsonObject.getJSONArray("韻書");
						for (int j = 0; j<jsonWanshyusArray.length(); j++) {
							JSONArray jsonWanshyuArray = jsonWanshyusArray.getJSONArray(j);
							String name, initial, rime, rimeClass, division, rounding, tone, trans;
							String coda, finalCh, initialCh, meaning, nuclei, siuwan, toneCh, yunbu;
							String pron;
							for (int k = 0; k<jsonWanshyuArray.length(); k++) {
								JSONObject jsonWanshyuInfo = jsonWanshyuArray.getJSONObject(k);
								name = jsonWanshyuInfo.getString("書名");
								switch (name) {
									case "廣韻":
										initial = jsonWanshyuInfo.getString("聲母");
										rimeClass = jsonWanshyuInfo.getString("攝");
										rime = jsonWanshyuInfo.getString("韻");
										division = jsonWanshyuInfo.getString("等");
										rounding = jsonWanshyuInfo.getString("呼");
										tone = jsonWanshyuInfo.getString("聲調");
										trans = jsonWanshyuInfo.getString("轉寫");
										contentCharaInfo.append(initial).append(rimeClass).append(rime).append(division).append(rounding).append(tone).append(trans);
										if (k<jsonWanshyuArray.length()-1) contentCharaInfo.append("<br>");
										
										break;
									case "分韻":
										yunbu = jsonWanshyuInfo.getString("韻部");
										siuwan = jsonWanshyuInfo.getString("小韻");
										initialCh = jsonWanshyuInfo.getString("聲字");
										finalCh = jsonWanshyuInfo.getString("韻字");
										toneCh = jsonWanshyuInfo.getString("調類");
										initial = jsonWanshyuInfo.getString("聲母");
										nuclei = jsonWanshyuInfo.getString("韻核");
										coda = jsonWanshyuInfo.getString("韻尾");
										tone = jsonWanshyuInfo.getString("聲調");
										meaning = jsonWanshyuInfo.getString("義");
										if (k==0) contentWanshyu.append("<b>").append(name).append("</b>\t");
										contentWanshyu.append(yunbu).append("-").append(siuwan).append(", ").append(initialCh).append(finalCh).append(toneCh);
										contentWanshyu.append("(").append(initial).append(nuclei).append(coda).append(tone).append("), ").append(meaning);
										if (k<jsonWanshyuArray.length()-1) contentWanshyu.append(" | ");
										
										break;
									case "英華":
										pron = jsonWanshyuInfo.getString("音");
										initial = jsonWanshyuInfo.getString("聲母");
										nuclei = jsonWanshyuInfo.getString("韻核");
										coda = jsonWanshyuInfo.getString("韻尾");
										tone = jsonWanshyuInfo.getString("聲調");
										if (k==0) contentWanshyu.append("<br><b>").append(name).append("</b>\t");
										contentWanshyu.append(pron).append("(").append(initial).append(nuclei).append(coda).append(tone).append(")");
										if (k<jsonWanshyuArray.length()-1) contentWanshyu.append(" | ");
										
										break;
									default:
										Log.w(TAG, "parseJson: 未知韻書");
								}
							}
						}
						addItem(chara, "", contentCharaInfo, contentWanshyu, contentLocation);
						
					}
					break;
				case EnumConst.QUERYING_PRON:  // 下面邏輯將棄用，改用 Character 類作輸出，類似 QUERYING_SHEET
					JSONObject jsonObject = new JSONObject(jsonString);
					JSONArray jsonLocationsArray = jsonObject.getJSONArray("各地");
					JSONArray jsonWanshyusArray = jsonObject.getJSONArray("韻書");
					parseJsonPron(jsonLocationsArray, contentLocation, EnumConst.GETTING_CONTENT_LOCATION);
					parseJsonPron(jsonWanshyusArray, contentWanshyu, EnumConst.GETTING_CONTENT_WANSHYU);
					addItem("", "", contentCharaInfo, contentWanshyu, contentLocation);
					break;
				case EnumConst.QUERYING_SHEET:
					jsonArray = new JSONArray(jsonString);
					JSONObject entry;
					
					EntrySetting entrySettings = new EntrySetting(EnumConst.QUERYING_SHEET)
							.setAreaColoringInfo(
									sp.getBoolean("area_coloring", true),
									sp.getFloat("area_coloring_darken_ratio", 0.92f))
							.setMeaningDomainPresence(
									sp.getBoolean("phrase_meaning_domain", false)
							);
					if (jsonArray.length() <= 1) {
						ToastUtil.msg(getContext(), getString(R.string.tips_no_result));
					}
					for (int i = 1; i<jsonArray.length(); i++) {
						entry = jsonArray.getJSONObject(i);
						character = new Character(entry, entrySettings);
						
						addItem(character.printCharacter(),
								character.printUnicode(),
								character.printPronunciation(),
								character.printMeanings(),
								character.printLocations()
						);
					}
					break;
				default:
					break;
			}
			mRvMain.getAdapter().notifyDataSetChanged();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 解析查音模式下的韻書音或地方音部分 json
	 *
	 * @deprecated 不再在這方法內解析，應改用 Character 類解析並輸出 spanned
	 * @see Character
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
			String[] stringForSortTone = new String[15];
			long stringForSortToneMask;
			for (int i = 0; i < jsonArray.length(); i++) {
				StringBuilder contentInOneLocation = new StringBuilder();
				JSONObject syllablesInCity = jsonArray.getJSONObject(i);
				JSONObject syllables;
				Iterator<String> syllablesInCityIterator = syllablesInCity.keys();
				Iterator<String> syllablesIterator;
				String syllablesInCityKey;
				String syllablesKey;
				contentInOneLocation.delete(0, contentInOneLocation.length());
				while (syllablesInCityIterator.hasNext()) {
					syllablesInCityKey = syllablesInCityIterator.next();
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
							syllables = syllablesInCity.getJSONObject(syllablesInCityKey);
							syllablesIterator = syllables.keys();
							stringForSortToneMask = 0L;
							
							// 對聲調排序，但對1'這種非數字調號會出問題
							while (syllablesIterator.hasNext()) {
								// syllablesKey == "haa" "ki" "ge"...
								syllablesKey = syllablesIterator.next();
								int keyInt = ("".equals(syllablesKey)) ? 0 : Integer.parseInt(syllablesKey)-1;
								stringForSortToneMask |= 1 << keyInt;
								stringForSortTone[keyInt] = syllablesInCityKey + syllablesKey + ": " + syllables.getString(syllablesKey);
							}
							for (int j = 0; j <= 15 && stringForSortToneMask != 0L; j++) {
								if ((stringForSortToneMask & (1 << j)) != 0L) {
									contentInOneLocation.append("<br>").append(stringForSortTone[j]);
									stringForSortToneMask ^= 1 << j;
								}
							}
							break;
					}
				}
				if (contentInOneLocation.length() != 0) {
					if (stringBuilder.length() != 0) stringBuilder.append("<br>");
					switch (type) {
						case EnumConst.GETTING_CONTENT_LOCATION:
							stringBuilder.append("<b>").append(city).append(district).append("</b>");
							break;
						case EnumConst.GETTING_CONTENT_WANSHYU:
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
	 * @deprecated 使用參數全爲 spanned 的同名方法
	 * @see #addItem(Spanned, Spanned, Spanned, Spanned, Spanned)
	 *
	 * @param chara layout 中的左上部分
	 *              僅用於顯示字頭
	 * @param extra layout 中的左中部分
	 *              顯示廣韻音（通語表）或統一碼（泛粵表）
	 * @param sbs layout 中的左下、右上下部分
	 *            左下用於泛粵表顯示綜合音
	 *            右上顯示韻書（通用表）或釋義（泛粵表）
	 *            右下顯示地方音
	 */
	private void addItem(String chara, String extra, StringBuilder... sbs) { // 將棄用，改用下面這個
		if (sbs.length != 3) return;
		if (sbs[0].length()!=0 || sbs[1].length()!=0 || sbs[2].length()!=0) {
			ResultItemAdapter.ResultInfo.addItem(
					chara,
					sbs[0].toString(),
					extra,
					sbs[1].toString(),
					sbs[2].toString()
			);
		}
		sbs[0].delete(0,sbs[0].length());
		sbs[1].delete(0,sbs[1].length());
		sbs[2].delete(0,sbs[2].length());
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
