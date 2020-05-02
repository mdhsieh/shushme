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

        // Get the Geofence Event from the Intent sent through
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, String.format("Error code : %d", geofencingEvent.getErrorCode()));
            return;
        }

        // Get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        // Check which transition type has triggered this event
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER)
        {
            //Log.d(TAG, "entered a geofence");
            //Toast.makeText(context, "entered a geofence", Toast.LENGTH_LONG).show();
            setRingerMode(context, AudioManager.RINGER_MODE_SILENT);
        }
        else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
        {
            //Log.d(TAG, "exited a geofence");
            setRingerMode(context, AudioManager.RINGER_MODE_NORMAL);
        }
        else
        {
            // Log the error
            Log.e(TAG, String.format("Unknown transition: %d", geofenceTransition));
            // Do not send a notification
            return;
        }

        // Send the notification
        sendNotification(context, geofenceTransition);
    }

    /**
     * Changes the ringer mode on the device to either silent or back to normal
     *
     * @param context The context to access AUDIO_SERVICE
     * @param mode    The desired mode to switch device to, can be AudioManager.RINGER_MODE_SILENT or
     *                AudioManager.RINGER_MODE_NORMAL
     */
    public void setRingerMode(Context context, int mode)
    {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // check for permissions for API 24+
        if (Build.VERSION.SDK_INT < 24 ||
                (Build.VERSION.SDK_INT >= 24 && notificationManager.isNotificationPolicyAccessGranted()))
        {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager != null) {
                audioManager.setRingerMode(mode);
            }
            else
            {
                Log.e(TAG, "AudioManager is null.");
            }
        }
    }

    /**
     * Posts a notification in the notification bar when a transition is detected
     * Uses different icon drawables for different transition types
     * If the user clicks the notification, control goes to the MainActivity
     *
     * @param context        The calling context for building a task stack
     * @param transitionType The geofence transition type, can be Geofence.GEOFENCE_TRANSITION_ENTER
     *                       or Geofence.GEOFENCE_TRANSITION_EXIT
     */
    private void sendNotification(Context context, int transitionType)
    {
        // Create an explicit Intent that starts the main Activity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Get a PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // Get a notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.SHUSHME_NOTIFICATION_CHANNEL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                // Dismiss notification once the user touches it.
                .setAutoCancel(true);

        // Check the transition type to display the relevant icon image
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
        {
            builder.setSmallIcon(R.drawable.ic_volume_off_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume_off_white_24dp))
                    .setContentTitle(context.getString(R.string.silent_mode_activated));
                    //.setContentText(context.getString(R.string.silent_mode_description));
        }
        else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)
        {
            builder.setSmallIcon(R.drawable.ic_volume_up_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume_up_white_24dp))
                    .setContentTitle(context.getString(R.string.back_to_normal));
                    //.setContentText(context.getString(R.string.normal_mode_description));
        }

        // Continue building the notification
        builder.setContentText(context.getString(R.string.touch_to_relaunch));

        // for backwards compatibility.
        // make sure notification is displayed and makes a sound on older devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            builder.setDefaults(Notification.DEFAULT_SOUND);
        }

        // Get an instance of the Notification manager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Show the notification
        notificationManager.notify(SHUSHME_NOTIFICATION_ID, builder.build());
    }
}
