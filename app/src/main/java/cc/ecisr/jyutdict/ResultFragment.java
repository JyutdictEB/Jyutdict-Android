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
import cc.ecisr.jyutdict.utils.EnumConst;
import cc.ecisr.jyutdict.utils.ToastUtil;

public class ResultFragment extends Fragment {
	private static final String TAG = "`ResultFragment";
	
	static private RecyclerView mRvMain;  //  不加static会显示两个View
	private View selfView;
	
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
					final boolean isCopiable = chara.length() != 0 && !"？".equals(chara);
					if (isCopiable) {
						selectionList.add("複製：" + chara);
					}
					final Pattern pt= Pattern.compile("((?<=（[～~])[^～~](?=）))|((?<=（)[^～~](?=[～~]）))");
					Matcher mt=pt.matcher(holder.tvRightTop.getText().toString());
					while(mt.find()){
						charaInWordsList.add(mt.group(0));
						selectionList.add("在通語字表檢索：" + mt.group(0));
						selectionList.add("在泛粵字表檢索：" + mt.group(0));
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
		Log.i(TAG, "onCreateView: " + System.identityHashCode(this));
		return selfView;
	}
	
	private void copy(String chara) {
		ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData mClipData = ClipData.newPlainText("Label", chara);
		if (cm != null) {
			cm.setPrimaryClip(mClipData);
			ToastUtil.msg(getContext(), "已複製："+chara);
		}
	}
	
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		Log.i(TAG, "onSaveInstanceState: ");
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onDestroyView() {
		Log.i(TAG, "onDestroyView: " + System.identityHashCode(this));
		super.onDestroyView();
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy: ");
		super.onDestroy();
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		Log.i(TAG, "onViewCreated: ");
		super.onViewCreated(view, savedInstanceState);
	}
	
	void parseJson(String jsonString, int queryObjectWhat) {
		try {
			if (mRvMain.getAdapter() == null) return;
			mRvMain.getAdapter().notifyDataSetChanged();
			ResultItemAdapter.ResultInfo.clearItem();
			StringBuilder contentLocation = new StringBuilder();
			StringBuilder contentWanshyu = new StringBuilder();
			StringBuilder contentCharaInfo = new StringBuilder();
			String chara;
			JSONArray jsonArray;
			switch (queryObjectWhat) {
				case EnumConst.QUERYING_CHARA: // 將棄用，改用Character類
					jsonArray = new JSONArray(jsonString);
					for (int i = 0; i<jsonArray.length(); i++) {
						JSONObject jsonObject = jsonArray.getJSONObject(i);
						JSONArray jsonLocationsArray = jsonObject.getJSONArray("各地");
						chara = jsonObject.getString("字");
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
				case EnumConst.QUERYING_PRON: // 將棄用，改用Character類
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
					Character character;
					if (getActivity()==null) break;
					SharedPreferences sp = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
					int areaColoring = (sp.getBoolean("area_coloring", true)) ? 4 : 0;
					for (int i = 1; i<jsonArray.length(); i++) {
						entry = jsonArray.getJSONObject(i);
						character = new Character(entry, EnumConst.QUERYING_SHEET | areaColoring);
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
	
	private void parseJsonPron(JSONArray jsonArray, StringBuilder stringBuilder, int type) { // TODO 改变量名
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
							// syllablesKey == "haa" "ki" "ge"...
							syllables = syllablesInCity.getJSONObject(syllablesInCityKey);
							syllablesIterator = syllables.keys();
							stringForSortToneMask = 0L;
							
							// 對聲調排序，但對1'這種非數字調號會出問題
							while (syllablesIterator.hasNext()) {
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
