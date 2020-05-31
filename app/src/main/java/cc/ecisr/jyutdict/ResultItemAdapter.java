package cc.ecisr.jyutdict;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Vector;

import cc.ecisr.jyutdict.utils.ToastUtil;

public class ResultItemAdapter extends RecyclerView.Adapter<ResultItemAdapter.LinearViewHolder> {
	private Context mContext;
	private OnItemClickListener mListener;
	
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
		ArrayList<String> item = ResultInfo.list.get(position);
		if (!"".equals(item.get(ResultInfo.HEADER)) || !"".equals(item.get(ResultInfo.INFO))) {
			holder.lyChara.setVisibility(View.VISIBLE);
			holder.tvCharaHeader.setText(Html.fromHtml(item.get(ResultInfo.HEADER)));
			holder.tvCharaInfo.setText(Html.fromHtml(item.get(ResultInfo.INFO)));
			holder.tvCharaExtra.setText(Html.fromHtml(item.get(ResultInfo.EXTRA)));
		} else {
			holder.lyChara.setVisibility(View.GONE);
		}
		
		if (!"".equals(item.get(ResultInfo.WANSHYU))) {
			holder.tvContentWanshyu.setVisibility(View.VISIBLE);
			holder.tvContentWanshyu.setText(Html.fromHtml(item.get(ResultInfo.WANSHYU)));
		} else {
			holder.tvContentWanshyu.setVisibility(View.GONE);
		}
		
		if (!"".equals(item.get(ResultInfo.LOCATION))) {
			holder.tvContentLocation.setVisibility(View.VISIBLE);
			holder.tvContentLocation.setText(Html.fromHtml(item.get(ResultInfo.LOCATION)));
		} else {
			holder.tvContentLocation.setVisibility(View.GONE);
		}
		
		// 短按提示Toast
		holder.itemView.setOnClickListener(v -> {
			mListener.onClick(position);
			if (holder.tvCharaHeader.getText().length() != 0) { // 理應放在上面Fragment的接口
				ToastUtil.msg(mContext, "長按以複製該字");
			}
		});
		// 長按複製
		holder.itemView.setOnLongClickListener(v -> {
			mListener.onLongClick(position);
			if (holder.tvCharaHeader.getText().length() != 0) { // 理應放在上面Fragment的接口
				ClipboardManager cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData mClipData = ClipData.newPlainText("Label", holder.tvCharaHeader.getText());
				if (cm != null) {
					cm.setPrimaryClip(mClipData);
					ToastUtil.msg(mContext, "已複製："+holder.tvCharaHeader.getText());
				}
			}
			return true;
		});
		
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
		private LinearLayout lyChara, lyContent;
		private TextView tvCharaHeader, tvCharaInfo, tvCharaExtra, tvContentWanshyu, tvContentLocation;
		
		LinearViewHolder(@NonNull View itemView) {
			super(itemView);
			lyChara = itemView.findViewById(R.id.item_chara);
			lyContent = itemView.findViewById(R.id.item_content);
			
			tvCharaHeader = itemView.findViewById(R.id.chara_header);
			tvCharaInfo = itemView.findViewById(R.id.chara_info);
			tvCharaExtra = itemView.findViewById(R.id.chara_extra);
			tvContentWanshyu = itemView.findViewById(R.id.content_wanshyu);
			tvContentLocation = itemView.findViewById(R.id.content_location);
		}
	}
	
	public interface OnItemClickListener {
		void onClick(int pos);
		void onLongClick(int pos);
	}
	
	static class ResultInfo {
		private static final int HEADER = 0, INFO = 1, EXTRA = 2, WANSHYU = 3, LOCATION = 4;
		
		static Vector<ArrayList<String>> list;
		static ArrayList<String> item;
		
		ResultInfo() {
			list = new Vector<>(0);
		}
		
		static void addItem(String charaHeader, String charaInfo, String charaExtra, String contentWanshyu, String contentLocation) {
			item = new ArrayList<>(5);
			item.add(charaHeader);
			item.add(charaInfo);
			item.add(charaExtra);
			item.add(contentWanshyu);
			item.add(contentLocation);
			list.add(item);
		}
		
		static void clearItem() {
			list.clear();
			
		}
	}
}