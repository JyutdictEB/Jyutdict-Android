package cc.ecisr.jyutdict.widget;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

import cc.ecisr.jyutdict.LocationDetailsDialog;
import cc.ecisr.jyutdict.struct.LocationInfo;

/** 地名短按入口；長按仍由 SelectableTextView 接管文字選取。 */
public final class LocationClickSpan extends ClickableSpan {
    private final int locationId;
    private final String locationName;

    public LocationClickSpan(int locationId) {
        this.locationId = locationId;
        this.locationName = null;
    }

    public LocationClickSpan(String locationName) {
        this.locationId = -1;
        this.locationName = locationName;
    }

    @Override
    public void onClick(@NonNull View widget) {
        LocationInfo.Location location = LocationInfo.get(locationId);
        if (location != null) {
            LocationDetailsDialog.show(widget.getContext(), location);
        } else if (locationName != null && !locationName.trim().isEmpty()) {
            LocationDetailsDialog.show(widget.getContext(), locationName);
        }
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        ds.setUnderlineText(false);
    }
}
