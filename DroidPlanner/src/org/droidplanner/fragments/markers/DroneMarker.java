package org.droidplanner.fragments.markers;

import org.droidplanner.R;
import org.droidplanner.drone.Drone;
import org.droidplanner.drone.DroneInterfaces.DroneEventsType;
import org.droidplanner.drone.DroneInterfaces.OnDroneListener;
import org.droidplanner.fragments.FlightMapFragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class DroneMarker implements OnDroneListener {

	private Marker droneMarker;
	private FlightMapFragment flightMapFragment;
	private Drone drone;
	private GoogleMap mMap;

	public DroneMarker(FlightMapFragment flightMapFragment) {
		this.flightMapFragment = flightMapFragment;
		this.drone = flightMapFragment.drone;
		this.mMap = flightMapFragment.mMap;
		addMarkerToMap();
		drone.events.addDroneListener(this);
	}

	public DroneMarker(Drone vDrone, GoogleMap vMap) {
		this.mMap = vMap;
		this.drone = vDrone;
		addMarkerToMap();
		drone.events.addDroneListener(this);
	}

	private void updatePosition(float yaw, LatLng coord) {
			droneMarker.setPosition(coord);
			droneMarker.setRotation(yaw);
			droneMarker.setVisible(true);
	}

	private void addMarkerToMap() {
		droneMarker = mMap.addMarker(new MarkerOptions()
				.anchor((float) 0.5, (float) 0.5).position(new LatLng(0, 0))
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.quad)).visible(false)
				.flat(true));
	}

	@Override
	public void onDroneEvent(DroneEventsType event, Drone drone) {
		switch (event) {
		case GPS:
			updatePosition((float)drone.orientation.getYaw(),
					drone.GPS.getPosition());
			break;
		default:
			break;
		}

	}
}
