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
import org.droidplanner.drone.variables.mission.survey.Survey;
import org.droidplanner.drone.variables.mission.survey.SurveyData;
import org.droidplanner.fragments.EditorMapFragment;
import org.droidplanner.fragments.RectangleEditorFragment;
import org.droidplanner.fragments.RectangleEditorFragment.OnRectangleEditorEvent;
import org.droidplanner.fragments.RectangleEditorFragment.RectangleEditorAction;
import org.droidplanner.fragments.helpers.GestureMapFragment;
import org.droidplanner.fragments.helpers.GestureMapFragment.OnPathFinishedListener;
import org.droidplanner.fragments.helpers.MapProjection;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

public class EditorActivity extends SuperUI implements OnPathFinishedListener,
		OnRectangleEditorEvent, OnEditorInteraction,
		OnWayPointTypeChangeListener{

	private EditorMapFragment planningMapFragment;
	private GestureMapFragment gestureMapFragment;
	private Mission mission;
	private RectangleEditorFragment rectangleEditorFragment;
	private MissionDetailFragment itemDetailFragment;
	private FragmentManager fragmentManager;
	private TextView infoView;

	private View mContainerItemDetail;
	private Polygon rectPolygon;
	private double mBearing;
	private double mForward=20.0, mLateral=20.0;
	private LatLng mOrigin;
	private SurveyData surveyData = new SurveyData();

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
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
						| WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		OnRectValueChanged(mForward, mLateral);
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
		case MISSION_UPDATE:
			// Remove detail window if item is removed
			if (itemDetailFragment != null) {
				if (!drone.mission.hasItem(itemDetailFragment.getItem())) {
					removeItemDetail();
				}
			}
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

	private void doClearMissionConfirmation() {
		YesNoDialog ynd = YesNoDialog.newInstance(
				getString(R.string.dlg_clear_mission_title),
				getString(R.string.dlg_clear_mission_confirm),
				new YesNoDialog.Listener() {
					@Override
					public void onYes() {
						mission.clear();
					}

					@Override
					public void onNo() {
					}
				});

		ynd.show(getSupportFragmentManager(), "clearMission");
	}

	@Override
	public void OnRectValueChanged(double vForward, double vLateral) {
		mForward = vForward;
		mLateral = vLateral;
		Location loc = getLocation();
		mOrigin = new LatLng(loc.getLatitude(), loc.getLongitude());
		drawRectangle(mOrigin, mBearing, mForward, mLateral);

	}

	private void drawRectangle(LatLng vOrigin, double vBearing,
			double vForward, double vLateral) {
		GoogleMap map = planningMapFragment.mMap;
		Log.d("RECT", String.format(
				"Lat/Lng: %f/%f\n Bearing: %f\n Fwd/Lat: %f %f",
				vOrigin.latitude, vOrigin.longitude, vBearing, vForward,
				vLateral));

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

		rectPolygon = map.addPolygon(new PolygonOptions().add(l0, l1, l2, l3)
				.strokeColor(Color.RED).strokeWidth((float) 2.0)
				.fillColor(0x330000ff));
		
		if(missionHasItem(0)!=null){
			updateSurveyPolygon(rectPolygon,true);
		}
	}

	private void updateSurveyPolygon(Polygon vRectPolygon, boolean updateSurveyData) {
		Survey survey = (Survey)missionHasItem(0);
		
		if(survey!=null){
			surveyData = survey.surveyData;
			mission.clear();
		}
		
		mission.addSurveyPolygon(vRectPolygon.getPoints());
		if(survey!=null && updateSurveyData){
			survey = (Survey)missionHasItem(0);
			survey.surveyData = surveyData;
			mission.notifiyMissionUpdate();
		}
	}

	private MissionItem missionHasItem(int aIndex) {
		List <MissionItem> mItems = mission.getItems();
		if(mItems.size()<=0 && mItems.size()>=aIndex)
			return null;
		return mItems.get(aIndex);
	}

	@Override
	public void OnRectAction(RectangleEditorAction vAction, boolean isLongClick) {

		switch (vAction) {
		case CREATE:
			if (isLongClick) {
				if(rectPolygon!=null){
					updateSurveyPolygon(rectPolygon,false);
				}
			} else {
				MissionItem mItem = missionHasItem(0);
				if(mItem!=null){	
					if(getItemDetailFragment()==null)
						showItemDetail(mItem);
					else
						removeItemDetail();
				} else {
					
				}
			}
			break;
		case DELETE:
			if (isLongClick)
				doClearMissionConfirmation();
			break;
		default:
			break;
		}

	}
}
