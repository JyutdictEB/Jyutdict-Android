package cc.ecisr.jyutdict;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cc.ecisr.jyutdict.struct.LocationInfo;

/** 網站端地名懸浮卡在 Android 上的觸控版。 */
public final class LocationDetailsDialog {
    private LocationDetailsDialog() {
    }

    public static void show(Context context, LocationInfo.Location location) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_location_info, null);
        TextView title = view.findViewById(R.id.location_title);
        TextView hierarchy = view.findViewById(R.id.location_hierarchy);
        TextView metadata = view.findViewById(R.id.location_metadata);
        Button article = view.findViewById(R.id.location_article);
        Button phonology = view.findViewById(R.id.location_phonology);

        title.setText(location.displayTitle());
        hierarchy.setText(location.hierarchy());
        hierarchy.setVisibility(location.hierarchy().isEmpty() ? View.GONE : View.VISIBLE);
        metadata.setText(location.sheetInfo.isEmpty()
                ? context.getString(R.string.location_no_metadata)
                : location.sheetInfo);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setNegativeButton(R.string.location_close, null)
                .create();

        article.setOnClickListener(v -> {
            context.startActivity(LocationReaderActivity.articleIntent(
                    context,
                    location.displayName()
            ));
            dialog.dismiss();
        });

        phonology.setEnabled(location.hasPhonology);
        if (!location.hasPhonology) {
            phonology.setText(R.string.location_phonology_unavailable);
        }
        phonology.setOnClickListener(v -> {
            context.startActivity(LocationReaderActivity.phonologyIntent(
                    context,
                    location.id,
                    location.displayName()
            ));
            dialog.dismiss();
        });

        dialog.show();
    }
}
