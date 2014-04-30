package org.droidplanner.fragments;

import org.droidplanner.R;
import org.droidplanner.drone.Drone;
import org.droidplanner.drone.DroneInterfaces.OnWaypointManagerListener;
import org.droidplanner.drone.variables.mission.WaypointEvent_Type;
import org.droidplanner.drone.variables.mission.commands.SetCamTriggerDist;
import org.droidplanner.drone.variables.mission.survey.Survey;
import org.droidplanner.service.MAVLinkClient.OnMavlinkTimeOutListener;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AG_WaypointProgressFragment extends DialogFragment implements OnMavlinkTimeOutListener, OnWaypointManagerListener{
	 private TextView progressTitle;
	 private TextView progressInfo;
	 private ProgressBar progressBar;
	 private Drone drone;
	 
	 private int maxWP;
	 

	    public AG_WaypointProgressFragment() {
	        // Empty constructor required for DialogFragment
	    }

	    public static AG_WaypointProgressFragment newInstance(Drone aDrone) {
	    	AG_WaypointProgressFragment frag = new AG_WaypointProgressFragment();
	        frag.drone = aDrone;
	        return frag;
	    }

	    @Override
	    public void onResume()
	    {
	        super.onResume();       
			getDialog().setCanceledOnTouchOutside(false);
	        getDialog().getWindow().setBackgroundDrawableResource(R.drawable.flight_control_button_selector);
	        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme);
	        drone.MavClient.registerTimeOutListener(this);
			drone.waypointMananger.registerWpEventListeners(this);
	    }
	    	    
		@Override
		public void onPause() {
			// TODO Auto-generated method stub
			super.onPause();
			drone.MavClient.unRegisterTimeOutListener(this);
			drone.waypointMananger.unRegisterWpEventListeners(this);
		}

		@Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	            Bundle savedInstanceState) {
			getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			
			View view = inflater.inflate(R.layout.ag_fragment_waypoint_progress, container);
			
			progressTitle = (TextView) view.findViewById(R.id.ag_wp_Title);
			progressInfo  = (TextView) view.findViewById(R.id.ag_wp_progressTxt);
			progressBar   = (ProgressBar) view.findViewById(R.id.ag_wp_progressBar);
			
	        return view;
	    }
		
		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			uploadWaypoints();
		}

		private void uploadWaypoints(){
			maxWP = drone.mission.getItems().size();
			progressBar.setMax(maxWP);
			updateProgress(0);
			updateCamTriggerDist();
			drone.mission.sendMissionToAPM();
		}

		private void updateCamTriggerDist() {
			if(drone.mission.getItems().size()<5)
				return;
			SetCamTriggerDist mItem = (SetCamTriggerDist) drone.mission.getItems().get(1);
			Survey sItem = (Survey) drone.mission.getItems().get(2);
			
			mItem.setDistance((float) sItem.surveyData.getLongitudinalPictureDistance().valueInMeters());
		}

		private void updateProgress(int aCount) {
			float prgs;
			try {
				prgs = ((float) (aCount*1.0) / (float)(maxWP*1.0)) * 100;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			
			progressBar.setProgress(aCount);
			progressInfo.setText(String.format("%d/%d - %d%%", aCount, maxWP, (int)prgs));
			
		}

		@Override
		public void notifyMavLinkTimeOut(int timeOutCount) {
			if(timeOutCount>2)
				getDialog().dismiss();
			
		}

		@Override
		public void onBeginWaypointEvent(WaypointEvent_Type wpEvent) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onWaypointEvent(WaypointEvent_Type wpEvent, int index,
				int count) {
			maxWP = count;
			progressBar.setMax(maxWP);;
			if(wpEvent!=WaypointEvent_Type.WP_RETRY && wpEvent!= WaypointEvent_Type.WP_TIMEDOUT)
				updateProgress(index);
		}

		@Override
		public void onEndWaypointEvent(WaypointEvent_Type wpEvent) {
			getDialog().dismiss();
		}
}
