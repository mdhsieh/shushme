package com.example.android.shushme;

/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.shushme.provider.PlaceContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Constants
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public static final int AUTOCOMPLETE_REQUEST_CODE = 4;

    public static final String SHUSHME_NOTIFICATION_CHANNEL = "shushme_notification_channel";

    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;

    // link to Google's privacy policy
    private TextView link;

    // check whether the Geofence on/off switch is enabled or not
    private boolean isEnabled;

    private PlacesClient placesClient;

    // list of Places fetched from Google live server
    List<Place> places = new ArrayList<>();

    private Geofencing geofencing;

    /**
     * Called when the activity is starting
     *
     * @param savedInstanceState The Bundle that contains the data supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this, places);
        mRecyclerView.setAdapter(mAdapter);

        // Initialize switch and handle enable/disable switch change
        Switch onOffSwitch = (Switch) findViewById(R.id.enable_switch);
        isEnabled = getPreferences(Context.MODE_PRIVATE).getBoolean(getString(R.string.setting_enabled), false);
        onOffSwitch.setChecked(isEnabled);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // isChecked will be true if the switch is in the On position
                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.setting_enabled), isChecked);
                isEnabled = isChecked;
                editor.commit();

                if (isEnabled)
                {
                    geofencing.registerAllGeofences();
                }
                else
                {
                    geofencing.unregisterAllGeofences();
                }
            }
        });

        // get reference to link from layout
        link = findViewById(R.id.privacy_policy_link);
        // make the link clickable
        link.setMovementMethod(LinkMovementMethod.getInstance());

        // Build up the LocationServices API client
        // Uses the addApi method to request the LocationServices API
        // Also uses enableAutoManage to automatically when to connect/suspend the client
        GoogleApiClient client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .build();

        // Initialize Places.
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), ApiKey.getApiKey());
        }

        // Create a new Places client instance.
        placesClient = Places.createClient(this);

        geofencing = new Geofencing(this, client);

        // create notification channel, which is required on Android 8.0 = API 26 and up
        createNotificationChannel();
    }

    /**
     * Called when the Google API Client is successfully connected
     *
     * @param connectionHint Bundle of data provided to clients by Google Play services
     */
    @Override
    public void onConnected(@Nullable Bundle connectionHint) {
        Log.i(TAG, "API Client Connection Successful!");
        // Get live data information
        refreshPlacesData();
    }

    /**
     * Called when the Google API Client is suspended
     *
     * @param cause The reason for the disconnection. Defined by constants CAUSE_*.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "API Client Connection Suspended!");
    }

    /**
     * Called when the Google API Client failed to connect to Google Play Services
     *
     * @param connectionResult A ConnectionResult that can be used for resolving the error
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "API Client Connection Failed!");
    }

    // Queries all the locally stored Places IDs
    // Calls placesClient.fetchPlace with that list of IDs
    private void refreshPlacesData()
    {

        Cursor cursor = getContentResolver().query(
                PlaceContract.PlaceEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if (cursor != null)
        {
            cursor.moveToFirst();

            // clear the list of places since we don't want repeats and
            // are fetching from all IDs again
            places.clear();

            String placeId;
            List<Place.Field> placeFields;
            FetchPlaceRequest request;

            for (int i = 0; i < cursor.getCount(); i++)
            {
                // Define a Place ID.
                placeId = cursor.getString(cursor
                        .getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID));

                // Specify the fields to return.
                placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

                // Construct a request object, passing the place ID and fields array.
                request = FetchPlaceRequest.newInstance(placeId, placeFields);

                // to use lambdas, the module settings were changed to use Java 8 language features.
                // See Project Structure->Properties or the app build.gradle file.

                // Add a listener to handle the response.
                placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
                    Place place = response.getPlace();

                    // add the Place to the list of places
                    places.add(place);

                    // swap places to update RecyclerView
                    mAdapter.swapPlaces(places);

                    // update geofences
                    geofencing.updateGeofencesList(places);
                    // register all geofences if switch enabled
                    if (isEnabled)
                    {
                        geofencing.registerAllGeofences();
                    }
                }).addOnFailureListener((exception) -> {
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        int statusCode = apiException.getStatusCode();
                        // Handle error with given status code.
                        Log.e(TAG, "Place not found: " + exception.getMessage());
                        Log.e(TAG, "Status code: " + statusCode);
                    }
                });

                cursor.moveToNext();
            }
            // always close the cursor
            cursor.close();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize location permissions checkbox
        final CheckBox locationPermissions = (CheckBox) findViewById(R.id.location_permissions_checkbox);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (locationPermissions.isEnabled()) {
                // permissions already granted so initialize to true
                locationPermissions.setChecked(true);
                locationPermissions.setEnabled(false);
            }
        }
        else
        {
            locationPermissions.setChecked(false);
        }

        // initialize ringer permissions checkbox
        final CheckBox ringerPermissions = (CheckBox) findViewById(R.id.ringer_permissions_checkbox);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Check if the API supports such permission change and check if permission is granted.

        // In case of Android N or later, we need to check if the permission was granted using
        // isNotificationPolicyAccessGranted
        if (Build.VERSION.SDK_INT >= 24
                && !notificationManager.isNotificationPolicyAccessGranted()) {
            ringerPermissions.setChecked(false);
        }
        else
        {
            // permissions granted by default
            ringerPermissions.setChecked(true);
            ringerPermissions.setEnabled(false);
        }
    }

    // Android 6.0 and up lets user allow permissions at runtime
    // Older versions request permissions at installation
    public void onLocationPermissionsClicked(View view)
    {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_LOCATION);
    }

    public void onRingerPermissionsClicked(View view)
    {
        if (Build.VERSION.SDK_INT >= 24) {
            Intent intent = new Intent(
                    android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    /**
     * Button Click event handler to handle clicking the "Add new location" Button
     *
     * @param view
     */
    public void addPlaceButtonClicked(View view)
    {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.location_permissions_granted_message), Toast.LENGTH_LONG).show();

            // Set the fields to specify which types of place data to return.
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
            // Start the autocomplete intent.
            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.FULLSCREEN, fields)
                    .build(this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        }
        else
        {
            Toast.makeText(this, getString(R.string.need_location_permission_message), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when the Autocomplete Place Activity returns back with a selected place (or after canceling)
     *
     * @param requestCode The request code passed when calling startActivityForResult
     * @param resultCode  The result code specified by the second activity
     * @param data        The Intent that carries the result data.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK)
        {
            Place place = Autocomplete.getPlaceFromIntent(data);

            // Extract the place information from the API
            String placeId = place.getId();

            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeId);
            // Insert a new Place ID into DB
            getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, values);

            // Get live data information
            refreshPlacesData();

        }
        else if (resultCode == AutocompleteActivity.RESULT_ERROR)
        {
            // Handle the error.
            Status status = Autocomplete.getStatusFromIntent(data);
            Log.e(TAG, status.getStatusMessage());
        }
        else if (resultCode == RESULT_CANCELED) {
            // The user canceled the operation.
            Log.i(TAG, "No place was selected.");
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(SHUSHME_NOTIFICATION_CHANNEL, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
