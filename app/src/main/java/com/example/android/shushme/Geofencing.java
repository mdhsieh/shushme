package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.model.Place;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class Geofencing implements ResultCallback {

    // Constants
    public static final String TAG = Geofencing.class.getSimpleName();

    private static final int GEOFENCE_RADIUS_IN_METERS = 50;
    // the Geofence will time out 24 hours after being registered
    private static final int GEOFENCE_EXPIRATION_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

    // default latitude and longitude is of Sydney Opera House in Australia
    // these are used if one of the places has no specified longitude and latitude
    public static final double DEFAULT_LATITUDE = -33.856159;
    public static final double DEFAULT_LONGITUDE = 151.215256;

    private Context context;
    private GoogleApiClient googleApiClient;
    private PendingIntent geofencePendingIntent;
    private List<Geofence> geofenceList;

    public Geofencing(Context context, GoogleApiClient googleApiClient)
    {
        this.context = context;
        this.googleApiClient = googleApiClient;
        geofencePendingIntent = null;
        geofenceList = new ArrayList<>();
    }

    /***
     * Registers the list of Geofences specified in geofenceList with Google Place Services
     * Uses {@code #mGoogleApiClient} to connect to Google Place Services
     * Uses {@link #getGeofencingRequest} to get the list of Geofences to be registered
     * Uses {@link #getGeofencePendingIntent} to get the pending intent to launch the IntentService
     * when the Geofence is triggered
     * Triggers {@link #onResult} when the geofences have been registered successfully
     */
    public void registerAllGeofences()
    {
        // Check that the API client is connected and that the list has Geofences in it
        if (googleApiClient == null || !googleApiClient.isConnected() ||
            geofenceList == null || geofenceList.size() == 0)
        {
            return;
        }
        try {
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            Log.e(TAG, securityException.getMessage());
        }
    }

    /***
     * Unregisters all the Geofences created by this app from Google Place Services
     * Uses {@code #mGoogleApiClient} to connect to Google Place Services
     * Uses {@link #getGeofencePendingIntent} to get the pending intent passed when
     * registering the Geofences in the first place
     * Triggers {@link #onResult} when the geofences have been unregistered successfully
     */
    public void unregisterAllGeofences()
    {
        if (googleApiClient == null || !googleApiClient.isConnected())
        {
            return;
        }
        try {
            LocationServices.GeofencingApi.removeGeofences(
                    googleApiClient,
                    // This is the same pending intent that was used in registerAllGeofences
                    getGeofencePendingIntent()
            );
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            Log.e(TAG, securityException.getMessage());
        }
    }

    /**
     * Updates the local ArrayList of Geofences using data from the passed in list
     * Uses the Place ID defined by the API as the Geofence object ID
     *
     * @param places the List result of the getPlaceById call
     */
    public void updateGeofencesList(List<Place> places)
    {
        geofenceList = new ArrayList<>();
        if (places == null || places.size() == 0)
        {
            return;
        }
        for (Place place: places) {
            // Read the place information from the database Cursor

            // the place's unique ID
            String placeId = place.getId();

            double latitude;
            double longitude;
            if (place.getLatLng() == null)
            {
                Log.i(TAG, "No latitude and longitude for " + place.getName());
                latitude = DEFAULT_LATITUDE;
                longitude = DEFAULT_LONGITUDE;
            }
            else
            {
                latitude = place.getLatLng().latitude;
                longitude = place.getLatLng().longitude;
                Log.d(TAG, place.getName() + " latitude: " + latitude);
                Log.d(TAG, place.getName() + " longitude: " + longitude);
            }

            // Build a Geofence object
            Geofence geofence = new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(placeId)
                .setCircularRegion(
                        latitude,
                        longitude,
                        GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

            // add the Geofence to the list
            geofenceList.add(geofence);
        }
    }

    /**
     * Create a GeofencingRequest object using the geofenceList ArrayList of Geofences
     * Used by {@code #registerGeofences}
     *
     * @return the GeofencingRequest object
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // if the device is already in a Geofence at the time of registering,
        // then trigger an entry transition event immediately
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    /**
     * Creates a PendingIntent object using the GeofenceBroadcastReceiver class
     * Used by {@code #registerGeofences}
     *
     * @return the PendingIntent object
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.e(TAG, String.format("Error adding/removing geofence: %s",
                result.getStatus().toString()));
    }
}
