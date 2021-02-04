package cc.ecisr.jyutdict.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SwitchCustomized extends SwitchMaterial {
	private OnSetCheckedListener mListener;
	private boolean isSetSetCheckedListener = false;
	
	public SwitchCustomized(@NonNull Context context) {
		super(context);
	}
	
	public SwitchCustomized(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}
	
	public SwitchCustomized(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	public void setSetCheckedListener(OnSetCheckedListener listener) {
		mListener = listener;
		isSetSetCheckedListener = true;
	}
	
	@Override
	public void setChecked(boolean checked) {
		super.setChecked(checked);
		if (isSetSetCheckedListener) mListener.onAfterSetChecked();
	}
	
	public interface OnSetCheckedListener {
		void onAfterSetChecked();
	}
}
