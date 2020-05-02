package com.example.android.shushme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
//import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    // unique id for each notification
    public static final int SHUSHME_NOTIFICATION_ID = 5;

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
            //Toast.makeText(context, "entered a geofence", Toast.LENGTH_LONG).show();
            setNotification(context, geofenceTransition);
            setRingerMode(context, AudioManager.RINGER_MODE_SILENT);
        }
        else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
        {
            Log.d(TAG, "exited a geofence");
            setNotification(context, geofenceTransition);
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

            if (audioManager != null) {
                audioManager.setRingerMode(mode);
            }
            else
            {
                Log.e(TAG, "AudioManager is null.");
            }
        }
    }

    private void setNotification(Context context, int geofenceTransition)
    {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.SHUSHME_NOTIFICATION_CHANNEL)
//                .setSmallIcon(R.drawable.notification_icon)
//                .setContentTitle("My notification")
//                .setContentText("Hello World!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER)
            {
                builder.setSmallIcon(R.drawable.ic_volume_off_white_24dp)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_volume_off_white_24dp))
                        .setContentTitle(context.getString(R.string.silent_mode_activated))
                        .setContentText(context.getString(R.string.silent_mode_description));
            }
            else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
            {
                builder.setSmallIcon(R.drawable.ic_volume_up_white_24dp)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_volume_up_white_24dp))
                        .setContentTitle(context.getString(R.string.back_to_normal))
                        .setContentText(context.getString(R.string.normal_mode_description));
            }

        // for backwards compatibility.
        // make sure notification is displayed and makes a sound on older devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            builder.setDefaults(Notification.DEFAULT_SOUND);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // show the notification
        notificationManager.notify(SHUSHME_NOTIFICATION_ID, builder.build());
    }
}
