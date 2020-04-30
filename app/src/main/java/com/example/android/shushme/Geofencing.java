package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.libraries.places.api.model.Place;

import java.util.ArrayList;
import java.util.List;

public class Geofencing {

    public static final String TAG = Geofencing.class.getSimpleName();

    public static final int GEOFENCE_RADIUS_IN_METERS = 3;
    // the Geofence will time out in 24 hours
    public static final int GEOFENCE_EXPIRATION_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

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

    /**
     * Create a Geofence for each place and add it to the Geofence ArrayList.
     *
     * @param places
     */
    public void updateGeofencesList(List<Place> places)
    {
        if (places == null || places.size() == 0)
        {
            return;
        }
        for (Place place: places) {
            // read the place info from the database Cursor
            // the place's unique ID
            String placeId = place.getId();
//            if (place.getLatLng() == null)
//            {
//                Log.e(TAG, "No latitude and longitude for " + place.getName());
//            }
            double latitude = place.getLatLng().latitude;
            double longitude = place.getLatLng().longitude;

            // build a Geofence object
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
     * Create a GeofencingRequest object using the geofenceList ArrayList
     *
     * @return the GeofencingRequest object
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // if the device is already in a Geofence at the time of registering,
        // the trigger an entry transition event immediately
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    /**
     * Create a PendingIntent object
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
}
