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

    public LocationClickSpan(int locationId) {
        this.locationId = locationId;
    }

    @Override
    public void onClick(@NonNull View widget) {
        LocationInfo.Location location = LocationInfo.get(locationId);
        if (location != null) {
            LocationDetailsDialog.show(widget.getContext(), location);
        }
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        ds.setUnderlineText(false);
    }
}
