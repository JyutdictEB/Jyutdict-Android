package cc.ecisr.jyutdict.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;

public class EditTextWithClear extends androidx.appcompat.widget.AppCompatEditText implements View.OnFocusChangeListener, TextWatcher {
	
	
	public EditTextWithClear(Context context) {
		super(context);
	}
	
	public EditTextWithClear(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public EditTextWithClear(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	
	@Override
	public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
	
	}
	
	@Override
	public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
	
	}
	
	@Override
	public void afterTextChanged(Editable editable) {
	
	}
	
	@Override
	public void onFocusChange(View view, boolean b) {
	
	}
}
