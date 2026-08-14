package cc.ecisr.jyutdict;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cc.ecisr.jyutdict.databinding.DialogLocationInfoBinding;
import cc.ecisr.jyutdict.struct.LocationInfo;
import cc.ecisr.jyutdict.utils.ColorUtil;
import cc.ecisr.jyutdict.utils.LocationArticleRepository;
import cc.ecisr.jyutdict.utils.MotionUtil;

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
        DialogLocationInfoBinding binding = DialogLocationInfoBinding.inflate(
                android.view.LayoutInflater.from(context));

        renderLocation(context, requestedName, initialLocation, binding.locationTitle,
                binding.locationMetadata, binding.locationMetadataAccent,
                binding.locationPhonology);
        binding.locationArticle.setEnabled(false);
        binding.locationArticle.setText(R.string.location_article_checking);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(binding.getRoot())
                .create();
        binding.locationClose.setOnClickListener(v -> dialog.dismiss());
        binding.locationPhonology.setOnClickListener(v -> {
            LocationInfo.Location target =
                    (LocationInfo.Location) binding.locationPhonology.getTag();
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
                MotionUtil.beginLayoutTransition((ViewGroup) binding.getRoot());
                binding.locationArticleChecking.setVisibility(View.INVISIBLE);
                LocationInfo.Location resolvedLocation = result.location != null
                        ? result.location
                        : initialLocation;
                renderLocation(
                        context,
                        result.resolvedName,
                        resolvedLocation,
                        binding.locationTitle,
                        binding.locationMetadata,
                        binding.locationMetadataAccent,
                        binding.locationPhonology
                );
                if (result.redirected()) {
                    MotionUtil.setText(binding.locationRedirect, context.getString(
                            R.string.location_article_redirected,
                            result.resolvedName
                    ));
                    binding.locationRedirect.setVisibility(View.VISIBLE);
                } else {
                    binding.locationRedirect.setVisibility(View.INVISIBLE);
                }
                binding.locationArticle.setEnabled(result.articleAvailable);
                MotionUtil.setText(binding.locationArticle, context.getString(
                        result.articleAvailable
                                ? R.string.location_article
                                : R.string.location_article_unavailable_button));
                binding.locationArticle.setOnClickListener(v -> {
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
                binding.getRoot().post(update);
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
            MotionUtil.setText(title, fallbackName);
            MotionUtil.setText(metadata, context.getString(R.string.location_no_metadata));
            metadataAccent.setBackground(ColorUtil.locationColorDrawable(
                    null,
                    GradientDrawable.Orientation.TOP_BOTTOM
            ));
            phonology.setEnabled(false);
            MotionUtil.setText((TextView) phonology,
                    context.getString(R.string.location_phonology_unavailable));
            phonology.setTag(null);
            return;
        }
        MotionUtil.setText(title, location.displayTitle());
        metadataAccent.setBackground(ColorUtil.locationColorDrawable(
                location.colors,
                GradientDrawable.Orientation.TOP_BOTTOM
        ));
        MotionUtil.setText(metadata, location.sheetInfo.isEmpty()
                ? context.getString(R.string.location_no_metadata)
                : location.sheetInfo);
        phonology.setEnabled(location.hasPhonology);
        MotionUtil.setText((TextView) phonology, context.getString(location.hasPhonology
                ? R.string.location_phonology
                : R.string.location_phonology_unavailable));
        phonology.setTag(location);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
