package cc.ecisr.jyutdict.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.ecisr.jyutdict.R;
import cc.ecisr.jyutdict.utils.ColorUtil;

/** 泛粵字表查詢列選單；每一項均在左側顯示其完整地點色。 */
public final class LocationSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {
    public static final class Option {
        public final String label;
        public final List<String> colors;
        public final String queryColumn;

        public Option(String label, List<String> colors, String queryColumn) {
            this.label = label;
            this.colors = colors == null
                    ? Collections.emptyList()
                    : new ArrayList<>(colors);
            this.queryColumn = queryColumn == null ? "" : queryColumn;
        }
    }

    private final LayoutInflater inflater;
    private final ArrayList<Option> options = new ArrayList<>();

    public LocationSpinnerAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void setOptions(List<Option> newOptions) {
        options.clear();
        if (newOptions != null) options.addAll(newOptions);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return options.size();
    }

    @Override
    public Option getItem(int position) {
        return options.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return bind(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return bind(position, convertView, parent);
    }

    private View bind(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null || row.findViewById(R.id.location_color_swatch) == null) {
            row = inflater.inflate(R.layout.spinner_drop_down_item, parent, false);
        }

        Option option = getItem(position);
        TextView label = row.findViewById(android.R.id.text1);
        View swatch = row.findViewById(R.id.location_color_swatch);
        label.setText(option.label);
        swatch.setBackground(ColorUtil.locationColorDrawable(option.colors));
        return row;
    }
}
