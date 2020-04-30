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

    public void updateGeofences(List<Place> places)
    {
        for (Place place: places) {
            geofenceList.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(place.getId())

                .setCircularRegion(
                        place.getLatLng().latitude,
                        place.getLatLng().longitude,
                        GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
        }
    }
}
