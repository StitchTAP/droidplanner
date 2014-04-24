package org.droidplanner.activities;

import java.util.List;

import org.droidplanner.R;
import org.droidplanner.activities.helpers.OnEditorInteraction;
import org.droidplanner.activities.helpers.SuperUI;
import org.droidplanner.dialogs.YesNoDialog;
import org.droidplanner.drone.Drone;
import org.droidplanner.drone.DroneInterfaces.DroneEventsType;
import org.droidplanner.drone.variables.mission.Mission;
import org.droidplanner.drone.variables.mission.MissionItem;
import org.droidplanner.drone.variables.mission.commands.ReturnToHome;
import org.droidplanner.drone.variables.mission.commands.SetCamTriggerDist;
import org.droidplanner.drone.variables.mission.survey.Survey;
import org.droidplanner.drone.variables.mission.survey.SurveyData;
import org.droidplanner.drone.variables.mission.waypoints.Takeoff;
import org.droidplanner.fragments.EditorMapFragment;
import org.droidplanner.fragments.RectangleEditorFragment;
import org.droidplanner.fragments.RectangleEditorFragment.OnRectangleEditorEvent;
import org.droidplanner.fragments.RectangleEditorFragment.RectangleEditorAction;
import org.droidplanner.fragments.helpers.GestureMapFragment;
import org.droidplanner.fragments.helpers.GestureMapFragment.OnPathFinishedListener;
import org.droidplanner.fragments.helpers.MapProjection;
import org.droidplanner.fragments.markers.DroneMarker;
import org.droidplanner.fragments.mission.MissionDetailFragment;
import org.droidplanner.fragments.mission.MissionDetailFragment.OnWayPointTypeChangeListener;
import org.droidplanner.helpers.geoTools.GeoTools;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

public class EditorActivity extends SuperUI implements OnPathFinishedListener,
		OnRectangleEditorEvent, OnEditorInteraction,
		OnWayPointTypeChangeListener {

	private EditorMapFragment planningMapFragment;
	private GestureMapFragment gestureMapFragment;
	private Mission mission;
	private RectangleEditorFragment rectangleEditorFragment;
	private MissionDetailFragment itemDetailFragment;
	private FragmentManager fragmentManager;
	private TextView infoView;

	private View mContainerItemDetail;
	private Polygon rectPolygon, surveyPolygon;
	private double mBearing;
	private double mForward = 20.0, mLateral = 20.0;
	private LatLng mOrigin;
	private SurveyData surveyData = new SurveyData();
	private DroneMarker droneMarker;
	static int SURVEYINDEX = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ag_activity_editor);

		ActionBar actionBar = getActionBar();
		if (actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled(true);

		fragmentManager = getSupportFragmentManager();

		planningMapFragment = ((EditorMapFragment) fragmentManager
				.findFragmentById(R.id.mapFragment));
		gestureMapFragment = ((GestureMapFragment) fragmentManager
				.findFragmentById(R.id.gestureMapFragment));
		rectangleEditorFragment = (RectangleEditorFragment) fragmentManager
				.findFragmentById(R.id.rectangleEditorFragment);
		infoView = (TextView) findViewById(R.id.editorInfoWindow);

		/*
		 * On phone, this view will be null causing the item detail to be shown
		 * as a dialog.
		 */
		mContainerItemDetail = findViewById(R.id.containerItemDetail);

		mission = drone.mission;
		gestureMapFragment.setOnPathFinishedListener(this);
		gestureMapFragment.disableGestureDetection();

		mBearing = 0;
		droneMarker = new DroneMarker(drone, planningMapFragment.mMap);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
						| WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		updateRectangleState();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		updateMapPadding();
	}

	private void updateMapPadding() {
		int topPadding = infoView.getBottom();
		int rightPadding = 0, bottomPadding = 0;
		if (mission.getItems().size() > 0) {
			bottomPadding = rectangleEditorFragment.getView().getHeight();
		}
		planningMapFragment.mMap.setPadding(rightPadding, topPadding, 0,
				bottomPadding);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		planningMapFragment.saveCameraPosition();
	}

	@Override
	public void onDroneEvent(DroneEventsType event, Drone drone) {
		super.onDroneEvent(event, drone);
		switch (event) {
		case GPS:
			updateRectangle(mForward, mLateral);
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			planningMapFragment.saveCameraPosition();
			NavUtils.navigateUpFromSameTask(this);
			Log.d("CAL", "Home");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onMapClick(LatLng point) {
		removeItemDetail();
		notifySelectionChanged();
	}

	@Override
	public void onPathFinished(List<Point> path) {
		List<LatLng> points = MapProjection.projectPathIntoMap(path,
				planningMapFragment.mMap);
		if (path.size() > 2) {
			drone.mission.addSurveyPolygon(points);
		}
	}

	@Override
	public void onWaypointTypeChanged(MissionItem newItem, MissionItem oldItem) {
		mission.replace(oldItem, newItem);
		showItemDetail(newItem);
	}

	@Override
	public boolean onItemLongClick(MissionItem item) {
		return true;
	}

	@Override
	public void onItemClick(MissionItem item) {
	}

	private void notifySelectionChanged() {
		planningMapFragment.update();
	}

	@Override
	public void onListVisibilityChanged() {
		updateMapPadding();
	}

	@Override
	public void OnRectValueChanged(double vForward, double vLateral) {
		if (getSurveyItem() != null && surveyPolygon == null) {

		}
		updateRectangle(vForward, vLateral);

	}

	private void updateRectangle(double vForward, double vLateral) {
		mForward = vForward;
		mLateral = vLateral;

		if (drone.GPS.getSatCount() <= 0) {
			Location loc = getLocation();

			if (loc == null)
				return;

			mOrigin = new LatLng(loc.getLatitude(), loc.getLongitude());
		} else {
			mOrigin = drone.GPS.getPosition();
		}

		mBearing = drone.navigation.getNavBearing();

		updateRectPolygon(mOrigin, mBearing, mForward, mLateral);
	}

	@Override
	public void OnRectAction(RectangleEditorAction vAction, boolean isLongClick) {

		switch (vAction) {
		case CREATE:
			if (isLongClick) {
				if (rectPolygon != null) {
					if (getSurveyItem() != null)
						doUploadSurveyConfirmation();
					else
						updateSurveyPoints(rectPolygon, false);
				}
			} else {
				MissionItem mItem = getSurveyItem();
				if (mItem != null) {
					if (getItemDetailFragment() == null)
						showItemDetail(mItem);
					else
						removeItemDetail();
				} else {
					Toast.makeText(this, R.string.ag_editor_rect_info,
							Toast.LENGTH_SHORT).show();
				}
			}
			break;
		case DELETE:
			if (isLongClick)
				doClearMissionConfirmation();
			else {
				Toast.makeText(this, R.string.ag_editor_trash_info,
						Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}

	}

	// Local Methods : Mission
	// Detail--------------------------------------------------------------------
	private void showItemDetail(MissionItem item) {
		if (itemDetailFragment == null) {
			addItemDetail(item);
		} else {
			switchItemDetail(item);
		}
	}

	private void addItemDetail(MissionItem item) {
		itemDetailFragment = item.getDetailFragment();

		if (mContainerItemDetail == null) {
			itemDetailFragment.show(fragmentManager, "Item detail dialog");
		} else {
			fragmentManager.beginTransaction()
					.add(R.id.containerItemDetail, itemDetailFragment).commit();
		}
	}

	public MissionDetailFragment getItemDetailFragment() {
		return itemDetailFragment;
	}

	public void switchItemDetail(MissionItem item) {
		removeItemDetail();
		addItemDetail(item);
	}

	private void removeItemDetail() {
		if (itemDetailFragment != null) {
			if (mContainerItemDetail == null) {
				itemDetailFragment.dismiss();
			} else {
				fragmentManager.beginTransaction().remove(itemDetailFragment)
						.commit();
			}
			itemDetailFragment = null;
		}
	}

	// Local Methods : Location
	// related--------------------------------------------------------------------
	private Location getLocation() {
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();

		Location location = locationManager
				.getLastKnownLocation(locationManager.getBestProvider(criteria,
						false));

		return location;
	}

	private void zoomToMyLocation(Location location) {
		if (location != null) {
			GoogleMap map = planningMapFragment.mMap;

			map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
					location.getLatitude(), location.getLongitude()), 13));

			CameraPosition cameraPosition = new CameraPosition.Builder()
					.target(new LatLng(location.getLatitude(), location
							.getLongitude())) // Sets the center of the map to
												// location user
					.zoom(map.getCameraPosition().zoom) // Sets the zoom
					.bearing(0) // Sets the orientation of the camera to east
					.tilt(0) // Sets the tilt of the camera to 30 degrees
					.build(); // Creates a CameraPosition from the builder
			map.animateCamera(CameraUpdateFactory
					.newCameraPosition(cameraPosition));
		}
	}

	// Local Methods : Rectangle
	// Drawing--------------------------------------------------------------------
	private void updateRectangleState() {
		Survey s = getSurveyItem();
		if(s!=null)
			try {
				updateSurveyPolygon(s.polygon.getLatLngList());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		OnRectValueChanged(mForward, mLateral);
	}

	private void updateRectPolygon(LatLng vOrigin, double vBearing,
			double vForward, double vLateral) {

		LatLng l0 = GeoTools.newCoordFromBearingAndDistance(vOrigin,
				vBearing - 90, vLateral / 2);
		LatLng l1 = GeoTools.newCoordFromBearingAndDistance(l0, vBearing + 90,
				vLateral);
		LatLng l2 = GeoTools.newCoordFromBearingAndDistance(l1, vBearing,
				vForward);
		LatLng l3 = GeoTools.newCoordFromBearingAndDistance(l0, vBearing,
				vForward);

		if (rectPolygon != null)
			rectPolygon.remove();

		rectPolygon = doDrawRectangle(l0, l1, l2, l3, Color.RED, 0x330000ff);
	}

	private void updateSurveyPolygon(List<LatLng> polygonPoints) {
		if (polygonPoints == null || polygonPoints.size() != 5)
			return;

		if (surveyPolygon != null)
			surveyPolygon.remove();

		surveyPolygon = doDrawRectangle(polygonPoints.get(0),
				polygonPoints.get(1), polygonPoints.get(2),
				polygonPoints.get(3), Color.YELLOW, 0x3300ff00);
	}

	private Polygon doDrawRectangle(LatLng l0, LatLng l1, LatLng l2, LatLng l3,
			int cStroke, int cFill) {
		GoogleMap map = planningMapFragment.mMap;
		return map
				.addPolygon(new PolygonOptions().add(l0, l1, l2, l3)
						.strokeColor(cStroke).strokeWidth((float) 2.0)
						.fillColor(cFill));
	}

	// Local Methods : Survey Mission items
	// helpers--------------------------------------------------------------------
	private void updateSurveyPoints(Polygon vRectPolygon,
			boolean updateSurveyData) {
		Survey survey = getSurveyItem();

		if (survey != null) {
			surveyData = survey.surveyData;
			mission.clear();
		}
		// Add a waypoint for takeoff
		mission.addWaypoint(mOrigin);

		// Add another waypoint for CAMTriggerDist = wpDistance
		mission.addWaypoint(mOrigin);

		// Add the survey waypoints
		mission.addSurveyPolygon(vRectPolygon.getPoints());
		if (survey != null && updateSurveyData) {
			survey = getSurveyItem();
			survey.surveyData = surveyData;
			mission.notifiyMissionUpdate();
		}

		// Add another waypoint for CAMTriggerDist = 0
		mission.addWaypoint(mOrigin);

		// Add another waypoint for RTL
		mission.addWaypoint(mOrigin);


		MissionItem cItem;
		MissionItem mItem;

		// Get the second waypoint and change to setCamTriggerDist
		try {
			cItem = mission.getItems().get(1);
			mItem = new SetCamTriggerDist(cItem);
			mission.replace(cItem, mItem);
		} catch (Exception e) {
			Log.d("EDITOR", "Failed to create SetCamTriggerDist waypoint");
		}

		// Get the second last waypoint and change to setCamTriggerDist
		try {
			cItem = mission.getItems().get(mission.getItems().size()-2);
			mItem = new SetCamTriggerDist(cItem);
			mission.replace(cItem, mItem);
		} catch (Exception e) {
			Log.d("EDITOR", "Failed to create SetCamTriggerDist waypoint");
		}

		// Get the first waypoint and change to Takeoff
		try {
			cItem = mission.getItems().get(0);
			mItem = new Takeoff(cItem);
			mission.replace(cItem, mItem);
		} catch (Exception e) {
			Log.d("EDITOR", "Failed to create Takeoff waypoint");
		}

		// Get the last waypoint and change to RTL
		try {
			cItem = mission.getItems().get(mission.getItems().size() - 1);
			mItem = new ReturnToHome(cItem);
			mission.replace(cItem, mItem);
		} catch (Exception e) {
			Log.d("EDITOR", "Failed to create RTL waypoint");
		}

		if (!updateSurveyData) {
			updateSurveyPolygon(rectPolygon.getPoints());
		}
	}

	private Survey getSurveyItem() {
		List<MissionItem> mItems = mission.getItems();
		
		if(mItems.size()>0 && SURVEYINDEX < mItems.size()){
			if(mItems.get(SURVEYINDEX).getClass().getName().contains("Survey"))
				return (Survey) mItems.get(SURVEYINDEX);
		}
		return null;
	}
	
	
	private void updateCamTriggerDist(){
		Survey survey = getSurveyItem();
		SetCamTriggerDist camTrigg = (SetCamTriggerDist) mission.getItems().get(1);
		
		float Dist = (float) survey.surveyData.getLateralPictureDistance().valueInMeters();
		camTrigg.setDistance(Dist);			
		Log.d("EDITOR", "Tiegger Dist: " + String.valueOf(camTrigg.getDistance()));

	}
	// Local Methods : Confirmation
	// dialogs--------------------------------------------------------------------
	private void doClearMissionConfirmation() {
		YesNoDialog ynd = YesNoDialog.newInstance(
				getString(R.string.dlg_clear_mission_title),
				getString(R.string.dlg_clear_mission_confirm),
				new YesNoDialog.Listener() {
					@Override
					public void onYes() {
						mission.clear();
						if (surveyPolygon != null)
							surveyPolygon.remove();
					}

					@Override
					public void onNo() {
					}
				});

		ynd.show(getSupportFragmentManager(), "clearMission");
	}

	private void doUploadSurveyConfirmation() {
		YesNoDialog ynd = YesNoDialog.newInstance(
				getString(R.string.ag_editor_rect_dlg_upload_title),
				getString(R.string.ag_editor_rect_dlg_upload),
				new YesNoDialog.Listener() {
					@Override
					public void onYes() {
						updateCamTriggerDist();
						drone.mission.sendMissionToAPM();
					}

					@Override
					public void onNo() {
					}
				});

		ynd.show(getSupportFragmentManager(), "createSurvey");
	}

}
