package cc.ecisr.jyutdict;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import cc.ecisr.jyutdict.utils.EnumConst;
import cc.ecisr.jyutdict.utils.StringUtil;

public class ResultFragment extends Fragment {
	private static final String TAG = "`ResultFragment";
	
	private RecyclerView mRvMain;
	private View selfView;
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if (selfView != null) { // 避免重新設置view
			ViewGroup parent = (ViewGroup) selfView.getParent();
			if (parent != null) parent.removeView(selfView);
			return selfView;
		}
		
		selfView = inflater.inflate(R.layout.fragment_result, container, false);
		mRvMain = selfView.findViewById(R.id.result_list);
		ResultItemAdapter marketItemAdapter = new ResultItemAdapter(getActivity(), new ResultItemAdapter.OnItemClickListener() {
			@Override
			public void onClick(int pos) {
				//ToastUtil.msg(getActivity(), "Click: " + pos);
			}
			
			@Override
			public void onLongClick(int pos) {
				//ToastUtil.msg(getActivity(), "Long: " + pos);
			}
		});
		
		mRvMain.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
		mRvMain.setItemAnimator(new DefaultItemAnimator());
		mRvMain.setAdapter(marketItemAdapter);
		if (getActivity() != null) {
			mRvMain.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
		}
		Log.i(TAG, "onCreateView: ");
		return selfView;
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		Log.i(TAG, "onViewCreated: ");
		super.onViewCreated(view, savedInstanceState);
	}
	
	void parseJson(String jsonString, int queryObjectWhat) {
		mRvMain.getAdapter().notifyDataSetChanged(); // 不能放在下面那句==null後
		try {
			if (mRvMain.getAdapter() == null) return;
			ResultItemAdapter.ResultInfo.clearItem();
			StringBuilder contentLocation = new StringBuilder();
			StringBuilder contentWanshyu = new StringBuilder();
			StringBuilder contentCharaInfo = new StringBuilder();
			String chara = "";
			JSONArray jsonArray;
			switch (queryObjectWhat) {
				case EnumConst.QUERYING_CHARA:
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
				case EnumConst.QUERYING_PRON:
					JSONObject jsonObject = new JSONObject(jsonString);
					JSONArray jsonLocationsArray = jsonObject.getJSONArray("各地");
					JSONArray jsonWanshyusArray = jsonObject.getJSONArray("韻書");
					parseJsonPron(jsonLocationsArray, contentLocation, EnumConst.GETTING_CONTENT_LOCATION);
					parseJsonPron(jsonWanshyusArray, contentWanshyu, EnumConst.GETTING_CONTENT_WANSHYU);
					
					addItem("", "", contentCharaInfo, contentWanshyu, contentLocation);
					break;
				case EnumConst.QUERYING_SHEET:
					jsonArray = new JSONArray(jsonString);
					JSONObject header = jsonArray.getJSONObject(0);
					JSONObject entry;
					Iterator<String> headerColsIterator = header.keys();
					int length = header.length();
					String[] headerColsString = new String[length];
					String retrievedString;
					String key;
					while (headerColsIterator.hasNext()) {
						key = headerColsIterator.next();
						headerColsString[header.getInt(key)] = key;
					}
					
					for (int i = 1; i<jsonArray.length(); i++) {
						entry = jsonArray.getJSONObject(i);
						for (int j=0; j<length; j++) {
							retrievedString = entry.getString(headerColsString[j]);
							switch (headerColsString[j]) {
								case "繁":
									chara = retrievedString;
									break;
								case "綜":
									if (retrievedString.contains("?")) {
										contentCharaInfo.append("<i>").append(retrievedString).append("</i>");
									} else {
										contentCharaInfo.append(retrievedString);
									}
									break;
								case "釋義":
									contentWanshyu.append(retrievedString);
									break;
								case "總點數":
									break;
								default:
									if (!"".equals(retrievedString)) {
										if ("_".equals(retrievedString)) contentLocation.append("<font color=\"#B9BAA3\">");
										if (retrievedString.contains("?")) contentLocation.append("<i>");
										contentLocation.append(headerColsString[j]).append(": ")
												.append(retrievedString).append("\t  ");
										if (retrievedString.contains("?")) contentLocation.append("</i>");
										if ("_".equals(retrievedString)) contentLocation.append("</font>");
										
									}
									break;
							}
						}
						String unicode = "";
						String processedChara = chara;
						if (!"!".equals(chara) && !"！".equals(chara) && !"?".equals(chara) && !"？".equals(chara)) {
							boolean questionable = chara.contains("?") || chara.contains("？");
							boolean duplicate = chara.contains("見");
							processedChara = chara.replaceAll("[?/!？！見 ]", "");
							unicode = StringUtil.charaToUnicode(processedChara);
							if ("".equals(processedChara)) processedChara = "　";
							if (questionable) processedChara = "<font color=\"#B9BAA3\">" + processedChara + "</font>";
							if (duplicate) processedChara = "<font color=\"#3d3b4f\">" + processedChara + "</font>";
						}
						addItem(processedChara, unicode, contentCharaInfo, contentWanshyu, contentLocation);
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
	
	private void addItem(String chara, String extra, StringBuilder... sbs) {
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
}
