package org.droidplanner.fragments;

import org.droidplanner.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class RectangleEditorFragment extends Fragment implements OnClickListener, OnLongClickListener, OnEditorActionListener, OnFocusChangeListener {
	
	public enum RectangleEditorAction {
		CREATE, DELETE, NONE
	}

	public interface OnRectangleEditorEvent {
		public void OnRectValueChanged(double vForward, double vLateral);
		public void OnRectAction(RectangleEditorAction vAction, boolean isLongClick);
	}

	private OnRectangleEditorEvent listener;

	private double fwdValue, latValue;
	private Activity activity;
	private EditText edtFwd, edtLat;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.ag_fragment_rectangle_editor, container,
				false);

		final Button buttonRect = (Button) view
				.findViewById(R.id.ag_editor_rectangle);
		
		final Button buttonTrash = (Button) view
				.findViewById(R.id.ag_editor_trash);

		final ImageButton btnFwdPlus = (ImageButton) view
				.findViewById(R.id.ag_fwd_btn_plus);

		final ImageButton btnFwdMinus = (ImageButton) view
				.findViewById(R.id.ag_fwd_btn_minus);

		final ImageButton btnLatPlus = (ImageButton) view
				.findViewById(R.id.ag_lat_btn_plus);

		final ImageButton btnLatMinus = (ImageButton) view
				.findViewById(R.id.ag_lat_btn_minus);

		edtFwd = (EditText) view
				.findViewById(R.id.ag_fwd_textedit);
		
		edtLat = (EditText) view
				.findViewById(R.id.ag_lat_textedit);

		for (View vv : new View[] { buttonRect, buttonTrash, btnFwdPlus,
				btnFwdMinus, btnLatPlus, btnLatMinus }) {
			vv.setOnClickListener(this);
			vv.setOnLongClickListener(this);
		}

		for (EditText vv : new EditText[] { edtFwd, edtLat }) {
			vv.setOnEditorActionListener(this);
			vv.setOnFocusChangeListener(this);
		}

		fwdValue = 20.0;
		latValue = 20.0;		
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);	
		this.activity = activity;
		listener = (OnRectangleEditorEvent) activity;
	}

	@Override
	public boolean onLongClick(View v) {
		
		RectangleEditorAction action = RectangleEditorAction.NONE;
		
		switch (v.getId()) {
		case R.id.ag_editor_rectangle:
				action = RectangleEditorAction.CREATE;
			break;
		case R.id.ag_editor_trash:
				action = RectangleEditorAction.DELETE;
			break;
		case R.id.ag_fwd_btn_plus:
			fwdValue+=5;
			break;
		case R.id.ag_lat_btn_plus:
			latValue+=5;
			break;
		case R.id.ag_fwd_btn_minus:
			fwdValue-=5;
			break;
		case R.id.ag_lat_btn_minus:
			latValue-=5;
			break;
		default:
			break;
		}
		
		if(listener!=null){
			if(action!=RectangleEditorAction.NONE)
				listener.OnRectAction(action, true);
			else{
				edtFwd.setText(String.valueOf(fwdValue));
				edtLat.setText(String.valueOf(latValue));
				updateListener();
			}
			return true;
		}
		
		return false;
	}

	@Override
	public void onClick(View v) {
		
		RectangleEditorAction action = RectangleEditorAction.NONE;
		
		switch (v.getId()) {
		case R.id.ag_editor_rectangle:
				action = RectangleEditorAction.CREATE;
			break;
		case R.id.ag_editor_trash:
				action = RectangleEditorAction.DELETE;
			break;
		case R.id.ag_fwd_btn_plus:
			fwdValue++;
			break;
		case R.id.ag_lat_btn_plus:
			latValue++;
			break;
		case R.id.ag_fwd_btn_minus:
			fwdValue--;
			break;
		case R.id.ag_lat_btn_minus:
			latValue--;
			break;
		default:
			action = RectangleEditorAction.NONE;
			break;
		}
		
		
		if(listener!=null){
			if(action!=RectangleEditorAction.NONE)
				listener.OnRectAction(action, false);
			else{
				edtFwd.setText(String.valueOf(fwdValue));
				edtLat.setText(String.valueOf(latValue));
				updateListener();
			}
		}
	}

	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
        	closeVirtualKeyboard(view);
        	updateValues(view);
         	updateListener();
            return true;
        }		
        
        return false;
	}

	@Override
	public void onFocusChange(View arg0, boolean arg1) {
		if(!arg1){
			updateValues((TextView)arg0);
			updateListener();
		}
	}

	private void updateValues(TextView view) {
       	switch(view.getId()){
    	case R.id.ag_fwd_textedit:
    		fwdValue =  Double.parseDouble(view.getEditableText().toString());
    		break;
    	case R.id.ag_lat_textedit:
    		latValue =  Double.parseDouble(view.getEditableText().toString());
    		break;
    	}
	}

	private void updateListener() {
    	if(listener!=null){
    		listener.OnRectValueChanged(fwdValue, latValue);
    	}
	}

	private void closeVirtualKeyboard(TextView view) {
		if(activity==null)
			return;
		
		InputMethodManager imm = (InputMethodManager) activity.getSystemService( Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);		
	}

}
