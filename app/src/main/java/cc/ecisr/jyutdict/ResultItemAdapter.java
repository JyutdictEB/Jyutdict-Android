package cc.ecisr.jyutdict;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ResultItemAdapter extends RecyclerView.Adapter<ResultItemAdapter.LinearViewHolder> {
	private final Context mContext;
	private final OnItemClickListener mListener;
	
	ResultItemAdapter(Context context, OnItemClickListener listener) {
		this.mContext = context; // 主activity
		this.mListener = listener; // 提供給fragment的監聽器
		new ResultInfo(); // 初始化類內靜態列表
	}
	
	@NonNull
	@Override
	public ResultItemAdapter.LinearViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new LinearViewHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_result_list_item, parent, false));
	}
	
	
	@Override
	public void onBindViewHolder(@NonNull ResultItemAdapter.LinearViewHolder holder, final int position) {
		ArrayList<Spanned> item = ResultInfo.list.get(position);
		Spanned header = item.get(ResultInfo.CHARA);
		Spanned info = item.get(ResultInfo.LEFT_MIDDLE);
		Spanned extra = item.get(ResultInfo.LEFT_BOTTOM);
		Spanned wanshyu = item.get(ResultInfo.RIGHT_TOP);
		Spanned location = item.get(ResultInfo.RIGHT_BOTTOM);
		
		holder.tvCharaHeader.setText(header);
		holder.tvCharaInfo.setText(info);
		holder.tvCharaExtra.setText(extra);
		holder.tvRightTop.setText(wanshyu);
		holder.tvRightBottom.setText(location);
		holder.tvRightBottom.setMovementMethod(LinkMovementMethod.getInstance());
		int lyCharaVisibility = (header.length()!=0 || info.length()!=0) ? View.VISIBLE : View.GONE;
		int tvContentInfoVisibility = (info.length()!=0) ? View.VISIBLE : View.GONE;
		int tvContentExtraVisibility = (extra.length()!=0) ? View.VISIBLE : View.GONE;
		int tvContentWanshyuVisibility = (wanshyu.length()!=0) ? View.VISIBLE : View.GONE;
		int tvContentLocationVisibility = (location.length()!=0) ? View.VISIBLE : View.GONE;
		holder.lyChara.setVisibility(lyCharaVisibility);
		holder.tvCharaInfo.setVisibility(tvContentInfoVisibility);
		holder.tvCharaExtra.setVisibility(tvContentExtraVisibility);
		holder.tvRightTop.setVisibility(tvContentWanshyuVisibility);
		holder.tvRightBottom.setVisibility(tvContentLocationVisibility);
		
		// 短按彈出操作菜單
		holder.itemView.setOnClickListener(v -> mListener.onClick(holder));
		// 長按複製
//		holder.itemView.setOnLongClickListener(v -> {
//			mListener.onLongClick(holder);
//			return true;
//		});
		
		ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
		layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
	}
	
	@Override
	public int getItemViewType(int position) {
		return super.getItemViewType(position);
	}
	
	@Override
	public int getItemCount() {
		return ResultInfo.list.size();
	}
	
	static class LinearViewHolder extends RecyclerView.ViewHolder {
		LinearLayout lyChara;//, lyContent;
		TextView tvCharaHeader, tvCharaInfo, tvCharaExtra, tvRightTop, tvRightBottom;
		
		LinearViewHolder(@NonNull View itemView) {
			super(itemView);
			lyChara = itemView.findViewById(R.id.item_chara);
			//lyContent = itemView.findViewById(R.id.item_content);
			
			tvCharaHeader = itemView.findViewById(R.id.chara_header);
			tvCharaInfo = itemView.findViewById(R.id.chara_info);
			tvCharaExtra = itemView.findViewById(R.id.chara_extra);
			tvRightTop = itemView.findViewById(R.id.content_wanshyu);
			tvRightBottom = itemView.findViewById(R.id.content_location);
		}
		
		String getChara() {
			return tvCharaHeader.getText().toString();
		}
	}
	
	public interface OnItemClickListener {
		void onClick(@NonNull ResultItemAdapter.LinearViewHolder holder);
		void onLongClick(@NonNull ResultItemAdapter.LinearViewHolder holder);
	}
	
	static class ResultInfo {
		private static final int CHARA = 0, LEFT_MIDDLE = 1, LEFT_BOTTOM = 2, RIGHT_TOP = 3, RIGHT_BOTTOM = 4;
		
		static ArrayList<ArrayList<Spanned>> list;
		
		ResultInfo() {
			list = new ArrayList<>(0);
		}
		
		/**
		 * 向本類維護的字項列表中添加一項
		 *
		 * @deprecated 使用參數全爲 spanned 的同名方法
		 * @see #addItem(Spanned, Spanned, Spanned, Spanned, Spanned)
		 */
		static void addItem(String charaHeader, String charaInfo, String charaExtra, String contentWanshyu, String contentLocation) {
			ArrayList<Spanned> item = new ArrayList<>(5);
			item.add(Html.fromHtml(charaHeader));
			item.add(Html.fromHtml(charaInfo));
			item.add(Html.fromHtml(charaExtra));
			item.add(Html.fromHtml(contentWanshyu));
			item.add(Html.fromHtml(contentLocation));
			list.add(item);
		}
		
		/**
		 * 向本類維護的字項列表中添加一項
		 */
		static void addItem(Spanned chara, Spanned leftMiddle, Spanned leftBottom, Spanned rightTop, Spanned rightBottom) {
			ArrayList<Spanned> item = new ArrayList<>(5);
			item.add(chara);
			item.add(leftMiddle);
			item.add(leftBottom);
			item.add(rightTop);
			item.add(rightBottom);
			list.add(item);
		}
		
		static void clearItem() {
			list.clear();
			
		}
	}
}