# ShushMe
Google Places API demo app.

This is based off the app from
the [Advanced Android App Development course on Udacity](https://www.udacity.com/course/advanced-android-app-development--ud855),
however this app uses the new Places SDK. As [the documentation](https://developers.google.com/places/android-sdk/client-migration#place-picker-deprecation)
states:

The Google Play Services version of the Places SDK for Android (i.e. com.google.android.gms:play-services-places) was turned off on July 29, 2019, and is no longer available.

This means the old Places SDK doesn't work, which is why this app uses the new version.

## Installation
To run this app, you need to:
1. Create an account on the [Google Cloud Platform](console.developers.google.com).
2. Add a billing account.
3. Enable the Places API from your console.
4. Create an API key and register your device's SHA-1 fingerprint. See these [instructions](https://developers.google.com/places/android-sdk/get-api-key).
5. Add this API key to a static class `ApiKey` with method `getApiKey()`.
For example, you can just copy-paste this file into the "shushme" package, replacing YOUR_API_KEY with your api key:

```
package com.example.android.shushme;

class ApiKey {
    private final static String API_KEY = "YOUR_API_KEY";

    static String getApiKey()
    {
        return API_KEY;
    }
}
```

## Screenshots

![Screenshot1](screenshots/screen_1.png) ![Screenshot2](screenshots/screen_2.png) ![Screenshot3](screenshots/screen_3.png)
![Screenshot4](screenshots/screen_4.png) ![Screenshot5](screenshots/screen_5.png) ![Screenshot6](screenshots/screen_6.png)
