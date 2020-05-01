package com.example.android.shushme;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    /***
     * Handles the Broadcast message sent when the Geofence Transition is triggered
     * Careful here though, this is running on the main thread so make sure you start an AsyncTask for
     * anything that takes longer than say 10 seconds to run
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive called.");

        // Get the Geofence Event from the Intent sent through
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        // Get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        // Check which transition type has triggered this event
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER)
        {
            Log.d(TAG, "entered a geofence");
            Toast.makeText(context, "entered a geofence", Toast.LENGTH_LONG).show();
            setRingerMode(context, AudioManager.RINGER_MODE_SILENT);
        }
        else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
        {
            Log.d(TAG, "exited a geofence");
            setRingerMode(context, AudioManager.RINGER_MODE_NORMAL);
        }
        else
        {
            // Log the error
            Log.e(TAG, String.format("Unknown transition: %d", geofenceTransition));
        }
    }

    public void setRingerMode(Context context, int mode)
    {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // check for permissions for API 24+
        if (Build.VERSION.SDK_INT < 24 ||
                Build.VERSION.SDK_INT >= 24 && notificationManager.isNotificationPolicyAccessGranted())
        {
            AudioManager audioManager =
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            audioManager.setRingerMode(mode);
        }
    }
}
