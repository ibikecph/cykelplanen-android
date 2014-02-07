// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.spoiledmilk.ibikecph.util.LOG;

public class SMLocationManager implements LocationListener {

	static final int UPDATE_INTERVAL = 2000; // the minimum time interval in
												// milliseconds for
												// notifications from location
												// system service

	LocationManager locationManager;
	SMLocationListener listener;

	private static SMLocationManager instance;

	Location lastValidLocation;
	boolean locationServicesEnabled;

	// Time to wait after the last gps location before using a non-gps location.
	public static final long GPS_WAIT_TIME = 20000; // 20 seconds
	private long lastGps = 0;

	private SMLocationManager() {
		lastValidLocation = null;
		locationServicesEnabled = false;
	}

	public static SMLocationManager getInstance() {
		if (instance == null)
			instance = new SMLocationManager();
		return instance;
	}

	public void init(Context context, SMLocationListener listener) {
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationServicesEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
				|| locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		if (locationServicesEnabled) {
			this.listener = listener;
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL, 0, this);
			try {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, 0, this);
			} catch (Exception e) {
				LOG.e(e.getLocalizedMessage());
			}
		}
	}

	public void removeUpdates() {
		listener = null;
		locationManager.removeUpdates(this);
	}

	public boolean hasValidLocation() {
		return lastValidLocation != null;
	}

	public Location getLastValidLocation() {
		return lastValidLocation;
	}

	public Location getLastKnownLocation() {
		if (lastValidLocation != null)
			return lastValidLocation;
		return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}

	@Override
	public void onLocationChanged(Location location) {

		// Ignore temporary non-gps fix
		if (shouldIgnore(location.getProvider(), System.currentTimeMillis())) {
			LOG.d("SMLocationManager onLocationChanged() location ignored: [" + location.getProvider() + "," + location.getLatitude() + ","
					+ location.getLongitude() + "]");
			return;
		}

		lastValidLocation = location;

		if (location != null) {
			if (listener != null)
				listener.onLocationChanged(location);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		locationServicesEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
				|| locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	@Override
	public void onProviderEnabled(String provider) {
		locationServicesEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
				|| locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		locationServicesEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
				|| locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	public boolean shouldIgnore(final String pProvider, final long pTime) {
		if (LocationManager.GPS_PROVIDER.equals(pProvider)) {
			lastGps = pTime;
		} else if (pTime < lastGps + GPS_WAIT_TIME) {
			return true;
		}
		return false;
	}

	public boolean isGPSEnabled() {
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}
}