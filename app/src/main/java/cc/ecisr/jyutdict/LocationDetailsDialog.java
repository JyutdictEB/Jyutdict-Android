package cc.ecisr.jyutdict;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import cc.ecisr.jyutdict.struct.LocationInfo;
import cc.ecisr.jyutdict.utils.ColorUtil;
import cc.ecisr.jyutdict.utils.LocationArticleRepository;

/** 網站端地名懸浮卡在 Android 上的觸控版。 */
public final class LocationDetailsDialog {
    private LocationDetailsDialog() {
    }

    public static void show(Context context, LocationInfo.Location location) {
        showInternal(context, location.displayName(), location);
    }

    public static void show(Context context, String locationName) {
        showInternal(context, locationName, LocationInfo.findByName(locationName));
    }

    private static void showInternal(Context context, String requestedName,
                                     LocationInfo.Location initialLocation) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_location_info, null);
        TextView title = view.findViewById(R.id.location_title);
        TextView redirect = view.findViewById(R.id.location_redirect);
        TextView metadata = view.findViewById(R.id.location_metadata);
        View metadataAccent = view.findViewById(R.id.location_metadata_accent);
        ProgressBar checking = view.findViewById(R.id.location_article_checking);
        Button article = view.findViewById(R.id.location_article);
        Button phonology = view.findViewById(R.id.location_phonology);

        renderLocation(context, requestedName, initialLocation, title, metadata,
                metadataAccent, phonology);
        article.setEnabled(false);
        article.setText(R.string.location_article_checking);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();
        view.findViewById(R.id.location_close).setOnClickListener(v -> dialog.dismiss());
        phonology.setOnClickListener(v -> {
            LocationInfo.Location target = (LocationInfo.Location) phonology.getTag();
            if (target == null || !target.hasPhonology) return;
            context.startActivity(LocationReaderActivity.phonologyIntent(
                    context,
                    target.id,
                    target.displayName(),
                    target.sheetStatistic
            ));
            dialog.dismiss();
        });

        LocationArticleRepository.lookup(context, requestedName, result -> {
            Runnable update = () -> {
                checking.setVisibility(View.INVISIBLE);
                LocationInfo.Location resolvedLocation = result.location != null
                        ? result.location
                        : initialLocation;
                renderLocation(
                        context,
                        result.resolvedName,
                        resolvedLocation,
                        title,
                        metadata,
                        metadataAccent,
                        phonology
                );
                if (result.redirected()) {
                    redirect.setText(context.getString(
                            R.string.location_article_redirected,
                            result.resolvedName
                    ));
                    redirect.setVisibility(View.VISIBLE);
                } else {
                    redirect.setVisibility(View.INVISIBLE);
                }
                article.setEnabled(result.articleAvailable);
                article.setText(result.articleAvailable
                        ? R.string.location_article
                        : R.string.location_article_unavailable_button);
                article.setOnClickListener(v -> {
                    if (!result.articleAvailable) return;
                    context.startActivity(LocationReaderActivity.articleIntent(
                            context,
                            result.resolvedName
                    ));
                    dialog.dismiss();
                });
            };
            if (Looper.myLooper() == Looper.getMainLooper()) {
                update.run();
            } else {
                view.post(update);
            }
        });

        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window == null) return;
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92f);
            window.setLayout(
                    Math.min(width, dp(context, 460)),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        });
        dialog.show();
    }

    private static void renderLocation(Context context, String fallbackName,
                                       LocationInfo.Location location,
                                       TextView title, TextView metadata,
                                       View metadataAccent,
                                       Button phonology) {
        if (location == null) {
            title.setText(fallbackName);
            metadata.setText(R.string.location_no_metadata);
            metadataAccent.setBackground(ColorUtil.locationColorDrawable(
                    null,
                    GradientDrawable.Orientation.TOP_BOTTOM
            ));
            phonology.setEnabled(false);
            phonology.setText(R.string.location_phonology_unavailable);
            phonology.setTag(null);
            return;
        }
        title.setText(location.displayTitle());
        metadataAccent.setBackground(ColorUtil.locationColorDrawable(
                location.colors,
                GradientDrawable.Orientation.TOP_BOTTOM
        ));
        metadata.setText(location.sheetInfo.isEmpty()
                ? context.getString(R.string.location_no_metadata)
                : location.sheetInfo);
        phonology.setEnabled(location.hasPhonology);
        phonology.setText(location.hasPhonology
                ? R.string.location_phonology
                : R.string.location_phonology_unavailable);
        phonology.setTag(location);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
