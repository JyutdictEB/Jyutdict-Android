package cc.ecisr.jyutdict;

import android.content.Context;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import cc.ecisr.jyutdict.widget.SelectableTextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ResultItemAdapter extends RecyclerView.Adapter<ResultItemAdapter.LinearViewHolder> {
    private final Context mContext;
    private final iOnItemClickListener mListener;

    ResultItemAdapter(Context context, iOnItemClickListener listener) {
        this.mContext = context; // 主activity
        this.mListener = listener; // 提供給fragment的監聯器
    }

    @NonNull
    @Override
    public ResultItemAdapter.LinearViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == ResultInfo.TYPE_SHEET
                ? R.layout.layout_result_list_item_sheet
                : R.layout.layout_result_list_item;
        return new LinearViewHolder(LayoutInflater.from(mContext).inflate(layout, parent, false));
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
        holder.tvRightTop.setSelectableText(wanshyu);
        holder.tvRightBottom.setSelectableText(location);
        int lyCharaVisibility = (header.length()!=0 || info.length()!=0) ? View.VISIBLE : View.GONE;
        int tvContentInfoVisibility = (info.length()!=0) ? View.VISIBLE : View.GONE;
        int tvContentExtraVisibility = (extra.length()!=0) ? View.VISIBLE : View.GONE;
        int tvContentWanshyuVisibility = (wanshyu.length()!=0) ? View.VISIBLE : View.GONE;
        int tvContentLocationVisibility = (location.length()!=0) ? View.VISIBLE : View.GONE;
        int dividerVisibility = (wanshyu.length() != 0 && location.length() != 0)
                ? View.VISIBLE : View.GONE;
        holder.lyChara.setVisibility(lyCharaVisibility);
        holder.tvCharaInfo.setVisibility(tvContentInfoVisibility);
        holder.tvCharaExtra.setVisibility(tvContentExtraVisibility);
        holder.tvRightTop.setVisibility(tvContentWanshyuVisibility);
        holder.tvRightBottom.setVisibility(tvContentLocationVisibility);
        holder.contentDivider.setVisibility(dividerVisibility);

        // 短按彈出操作菜單
        holder.itemView.setOnClickListener(v -> mListener.onClick(holder));

        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
    }

    @Override
    public int getItemViewType(int position) {
        return ResultInfo.types.get(position);
    }

    @Override
    public int getItemCount() {
        return ResultInfo.list.size();
    }

    public static class LinearViewHolder extends RecyclerView.ViewHolder {
        LinearLayout lyChara;
        View contentDivider;
        TextView tvCharaHeader, tvCharaInfo, tvCharaExtra;
        SelectableTextView tvRightTop, tvRightBottom;

        LinearViewHolder(@NonNull View itemView) {
            super(itemView);
            lyChara = itemView.findViewById(R.id.item_chara);

            tvCharaHeader = itemView.findViewById(R.id.chara_header);
            tvCharaInfo = itemView.findViewById(R.id.chara_info);
            tvCharaExtra = itemView.findViewById(R.id.chara_extra);
            tvRightTop = itemView.findViewById(R.id.content_wanshyu);
            tvRightBottom = itemView.findViewById(R.id.content_location);
            contentDivider = itemView.findViewById(R.id.content_divider);
        }

        String getChara() {
            return tvCharaHeader.getText().toString();
        }
        String printContent() {
            return tvCharaHeader.getText().toString() + "\n" +
                    tvCharaInfo.getText().toString() + "\n" +
                    tvCharaExtra.getText().toString() + "\n" +
                    tvRightTop.getText().toString() + "\n" +
                    tvRightBottom.getText().toString() + "\n";
        }
    }

    public interface iOnItemClickListener {
        void onClick(@NonNull ResultItemAdapter.LinearViewHolder holder);
        void onLongClick(@NonNull ResultItemAdapter.LinearViewHolder holder);
    }

    static class ResultInfo {
        static final int TYPE_GENERAL = 0;
        static final int TYPE_SHEET = 1;
        private static final int CHARA = 0, LEFT_MIDDLE = 1, LEFT_BOTTOM = 2, RIGHT_TOP = 3, RIGHT_BOTTOM = 4;

        static ArrayList<ArrayList<Spanned>> list = new ArrayList<>(0);
        static ArrayList<Integer> types = new ArrayList<>(0);

        ResultInfo() {
        }

        /**
         * 向本類維護的字項列表中添加一項
         */
        static void addItem(Spanned chara, Spanned leftMiddle, Spanned leftBottom,
                            Spanned rightTop, Spanned rightBottom, int type) {
            ArrayList<Spanned> item = new ArrayList<>(5);
            item.add(chara);
            item.add(leftMiddle);
            item.add(leftBottom);
            item.add(rightTop);
            item.add(rightBottom);
            list.add(item);
            types.add(type);
        }

        static void clearItem() {
            list.clear();
            types.clear();
        }
    }
}
