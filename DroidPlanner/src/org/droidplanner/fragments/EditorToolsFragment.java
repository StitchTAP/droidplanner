package org.droidplanner.fragments;

import org.droidplanner.R;
import org.droidplanner.widgets.button.RadioButtonCenter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;

public class EditorToolsFragment extends Fragment implements OnClickListener, OnLongClickListener {

	public enum EditorTools {
		RECT, TRASH, NONE
	}

	public interface OnEditorToolSelected {
		public void editorToolChanged(EditorTools tools);
		public void editorToolLongClicked(EditorTools tools);
	}

	private OnEditorToolSelected listener;


	private EditorTools tool = EditorTools.NONE;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.ag_fragment_editor_tools, container,
				false);

		final Button buttonRect = (Button) view.findViewById(R.id
                .ag_editor_rectangle);
		final Button buttonTrash = (Button) view.findViewById(R.id
                .ag_editor_trash);
        
		for (View vv : new View[] { buttonRect, 
				buttonTrash }) {
			vv.setOnClickListener(this);
			vv.setOnLongClickListener(this);
		}
		
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		listener = (OnEditorToolSelected) activity;
	}

	@Override
	public boolean onLongClick(View v) {
		EditorTools newTool = EditorTools.NONE;

		switch (v.getId()) {
		case R.id.ag_editor_rectangle:
			newTool = EditorTools.RECT;
			break;
		case R.id.ag_editor_trash:
			newTool = EditorTools.TRASH;
			break;
		}

		if (newTool != EditorTools.NONE) {
			listener.editorToolLongClicked(newTool);
		}

		return false;
	}
	
	@Override
	public void onClick(View v) {
		EditorTools newTool = EditorTools.NONE;
		switch (v.getId()) {
		case R.id.ag_editor_rectangle:
			newTool = EditorTools.RECT;
			break;
		case R.id.ag_editor_trash:
			newTool = EditorTools.TRASH;
			break;
		}
		if (newTool == this.tool) {
			newTool = EditorTools.NONE;
		}

		setTool(newTool);
	}

	public EditorTools getTool() {
		return tool;
	}

	public void setTool(EditorTools tool) {
		this.tool = tool;
		if (tool == EditorTools.NONE) {
		}
		listener.editorToolChanged(this.tool);
	}
	
	public void clearCheck(){
		this.tool = EditorTools.NONE;
	}
}
