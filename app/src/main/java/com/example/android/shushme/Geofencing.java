package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.libraries.places.api.model.Place;

import java.util.ArrayList;
import java.util.List;

public class Geofencing {

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
}
