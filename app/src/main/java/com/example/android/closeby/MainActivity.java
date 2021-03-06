package com.example.android.closeby;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    // Tags
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    protected static final String NEARBY_PLACE_KEY = "NEARBY_PLACE_KEY";
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final String PLACE_DETAIL_FRAGMENT_TAG = "PLACE_DETAIL_FRAGMENT_TAG";

    // Default starting location (San Francisco, CA) and map settings
    private static final LatLng DEFAULT_LOCATION = new LatLng(37.7386881, -122.4712451);
    private static final int DEFAULT_ZOOM = 16;
    private static final long DEFAULT_INTERVAL = 7 * 1000;
    private static final long DEFAULT_FASTEST_INTERVAL = 5 * 1000;

    // Permissions
    private boolean mLocationPermissionGranted;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 1;

    private GoogleMap mGmap;

    // Places API
    private PlaceDetectionClient mPlaceDetectionClient;

    // LocationProvider to get last known location
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // Location items needed to grab periodic updated position
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    // The geographical representation of where the device is located
    private Location mLastKnownLocation;

    // Nearby places storage
    private List<PlaceContainer> mSortedCurNearbyPlaces;

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        // Default values and High Accuracy are set for typical tracking when walking
        mLocationRequest.setInterval(DEFAULT_INTERVAL);
        mLocationRequest.setFastestInterval(DEFAULT_FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Instantiate the clients
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Define the LocationCallback for our updates
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // Grab the most recent location result
                Location mostRecentLocation = locationResult.getLastLocation();

                /**
                 * TODO: Maybe put a restriction here if we haven't moved
                 */
                if (mostRecentLocation != null) {
                    // Update the maps positioning if we have a response
                    mLastKnownLocation = mostRecentLocation;
                    updateCameraMapPosition();

                    // Find the nearest places
                    findNearByPlaces();
                }
            }
        };

        // Find our fragment through its ID and build the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        if (id == R.id.action_showList) {
            // Check if we have data
            if (mSortedCurNearbyPlaces == null) {
                // Do not allow to show a list without any valid data
                checkLocationSettings();
                return false;
            }
            // If our "Show List" was clicked
            // Bundle up our sorted nearby list data
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(NEARBY_PLACE_KEY
                    , (ArrayList<? extends Parcelable>) mSortedCurNearbyPlaces);

            // Create the new fragment and setArguments with the bundle
            PlaceDetailFragment pdFragment = new PlaceDetailFragment();
            pdFragment.setArguments(bundle);

            // Start up the new fragment within this current main activity container
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.activity_main_container, pdFragment, PLACE_DETAIL_FRAGMENT_TAG)
                    .addToBackStack(null)
                    .commit();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLocationPermissionGranted)
            startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    /**
     * The callback that is triggered when a GoogleMap instance is available for use.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Grab the map and set a minimum zoom
        mGmap = googleMap;
        mGmap.setMinZoomPreference(DEFAULT_ZOOM);

        // Get user permission for location
        getPermission();

        // Update the My Location layer
        updateLocationUI();

        // Check that we have all available LocationSettings to grab our position
        checkLocationSettings();

        // Get the current location and set it on the map
        //getCurrentLocation();
    }

    /**
     * The callback that is triggered when back-button is pressed
     */
    @Override
    public void onBackPressed() {
        // On back button pressed, return back to our initial map fragment
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Ask the user for permission regarding device location
     */
    private void getPermission() {
        // Request the location permission. The request will be handled with the
        // onRequestPermissionResult callback

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            createLocationRequest();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * The callback used to resolve the permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;

        // Handle by the requestCode that is passed in
        switch (requestCode) {
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION: {
                // Check for a granted permission
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    createLocationRequest();
                }
            }
        }

        // Update the location UI settings after resolving the permission
        updateLocationUI();
    }

    /**
     * Updates the UI settings based on if the user has given permission
     */
    private void updateLocationUI() {
        if (mGmap == null)
            return;

        try {
            // Set the map UI settings based on our granted permission
            if (mLocationPermissionGranted) {
                mGmap.setMyLocationEnabled(true);
                mGmap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mGmap.setMyLocationEnabled(false);
                mGmap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;

                // Call for location permission if it is still not granted
                getPermission();
            }
        } catch (SecurityException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    /**
     * Get the most recent location and set it on the map
     */
    private void getCurrentLocation() {
        try {
            // Get the most current/recent location
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set our new location and update the map camera
                            mLastKnownLocation = task.getResult();
                            updateCameraMapPosition();
                        }
                    }
                });

                // Request location updates
                startLocationUpdates();
            }
        } catch (SecurityException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    /**
     * Method to check if our Location settings is satisfactory for the LocationRequest
     */
    private void checkLocationSettings() {
        // Build a LocationSettingRequest and add our LocationRequest to it
        LocationSettingsRequest.Builder locSetRequestBuilder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        // Check if we have satisfied our location settings
        final Task<LocationSettingsResponse> settingResponse =
                LocationServices.getSettingsClient(this)
                        .checkLocationSettings(locSetRequestBuilder.build());

        // Check the response
        settingResponse.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);

                    // All location settings are satisfied. Client can perform location requests.
                    Log.d(LOG_TAG, "checkLocationSettings(): All settings were satisfied");
                    getCurrentLocation();

                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {

                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied.
                            try {
                                // Attempt to resolve the exception
                                ResolvableApiException resolvable = (ResolvableApiException) exception;

                                // Show a dialog and check the results in onActivityResult()
                                resolvable.startResolutionForResult(MainActivity.this
                                        , REQUEST_CHECK_SETTINGS);

                            } catch (IntentSender.SendIntentException e) {
                                // Can ignore this error
                            } catch (ClassCastException e) {
                                // Can ignore this error
                            }
                            break;

                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:

                            /* Location settings are not satisfied, but this case may be due to
                            ** network issues.
                             */

                            // Output a snackbar message suggesting a network error
                            Snackbar.make((CoordinatorLayout) findViewById(R.id.activity_main_container)
                                    , R.string.snackbar_network_message, Snackbar.LENGTH_SHORT).show();
                            getCurrentLocation();
                            Log.d(LOG_TAG, "checkLocationSettings(): Unable to resolve, could be network issue");
                            break;
                    }
                }
            }
        });
    }

    /**
     * The method which will check if the correct changes were made to service our LocationRequest
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {

            case REQUEST_CHECK_SETTINGS:
                switch(resultCode) {
                    case Activity.RESULT_OK:

                        // The necessary changes were made
                        Log.d(LOG_TAG, "onActivityResult: GPS setting was resolved");
                        getCurrentLocation();
                        break;

                    case Activity.RESULT_CANCELED:

                        // The necessary changes were not made
                        Log.d(LOG_TAG, "onActivityResult: GPS setting was not resolved");
                        break;

                    default:
                        break;
                }
                break;
        }
    }

    /**
     * Updates the camera position of the map
     */
    private void updateCameraMapPosition() {
        // Check that we actually have a valid location response
        if (mLastKnownLocation == null) {
            // Set the camera location to our default location
            mGmap.animateCamera(CameraUpdateFactory.newLatLng(DEFAULT_LOCATION));

            Log.d(LOG_TAG, "Location is null, default location was used.");
        } else {
            // Set the camera location to our response
            mGmap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude())
                    , mGmap.getCameraPosition().zoom));
        }
    }

    /**
     * Finds the nearest places on the map based on current location
     */
    private void findNearByPlaces() {
        if (mGmap == null)
            return;

        if (mLocationPermissionGranted) {
            // Find the nearby places based on current location. In this case, we want to find nearby bars.
            @SuppressWarnings("MissingPermission") final Task<PlaceLikelihoodBufferResponse> placeResult =
                    mPlaceDetectionClient.getCurrentPlace(null);

            placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                @Override
                public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                    // Check if our task was successful/contains a response
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<PlaceContainer> placesList = new ArrayList<>();
                        PlaceLikelihoodBufferResponse nearbyPlacesResponse = task.getResult();

                        // Iterate through our list while finding only bar type places
                        for (PlaceLikelihood nearbyPlace : nearbyPlacesResponse) {
                            // Check that the nearbyPlace is a bar type
                            if (nearbyPlace.getPlace().getPlaceTypes().contains(Place.TYPE_BAR)) {

                                // Current Place is a bar, create our container and add it to our list
                                PlaceContainer curPlace = new PlaceContainer(nearbyPlace.getPlace()
                                        , new LatLng(mLastKnownLocation.getLatitude()
                                        , mLastKnownLocation.getLongitude()));

                                placesList.add(curPlace);
                            }
                        }

                        // Release the PlaceLikelihoodBuffer to avoid memory leaks
                        nearbyPlacesResponse.release();

                        // Sort our placesList based on distance between current location
                        Collections.sort(placesList, new PlaceContainer.PlaceDistanceComparator());
                        mSortedCurNearbyPlaces = placesList;

                        // Check if we need to update the PlaceDetailFragment
                        updatePlaceDetailData(mSortedCurNearbyPlaces);

                        // Mark the places on the map
                        addMarkersToMap();
                    } else {
                        // Task was not successful, log it
                        Log.e(LOG_TAG, "Exception: %s", task.getException());
                    }
                }
            });
        }
    }

    /**
     * Method to update the data of an already inflated PlaceDetailFragment
     * @param newSortedNearbyPlacesData
     */
    private void updatePlaceDetailData(List<PlaceContainer> newSortedNearbyPlacesData) {
        // Find our the inflated fragment by its tag
        PlaceDetailFragment alreadyInflatedFragment =
                (PlaceDetailFragment) getSupportFragmentManager()
                        .findFragmentByTag(PLACE_DETAIL_FRAGMENT_TAG);

        if (alreadyInflatedFragment != null) {
            // We found the fragment, update its data
            alreadyInflatedFragment.setNewData(newSortedNearbyPlacesData);
        }
    }

    /**
     * Add the markers for our list of places
     */
    private void addMarkersToMap() {
        if (mGmap == null)
            return;

        // Clear previous points
        mGmap.clear();

        for (PlaceContainer curPlace : mSortedCurNearbyPlaces) {
            // Create a custom marker to use for our map
            // Inflate the Layout that will be used as our marker
            LinearLayout markerLayout = (LinearLayout) this.getLayoutInflater()
                    .inflate(R.layout.map_marker_layout, null, false);

            // Set the name in the textview
            TextView view = markerLayout.findViewById(R.id.map_marker_title);
            view.setText(curPlace.getName());

            // Build/draw the bitmap
            markerLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            markerLayout.layout(0, 0, markerLayout.getMeasuredWidth(), markerLayout.getMeasuredHeight());
            markerLayout.setDrawingCacheEnabled(true);
            markerLayout.buildDrawingCache();
            Bitmap drawnBitMap = markerLayout.getDrawingCache();

            // Add a marker for each associated place
            mGmap.addMarker(new MarkerOptions()
                    .title(curPlace.getName())
                    .position(curPlace.getLatLng())
                    .snippet(curPlace.getAddress())
                    .icon(BitmapDescriptorFactory.fromBitmap(drawnBitMap)));
        }
    }

    /**
     * Method to check if we are connected to the network
     * @return true - if connected
     *
     * TODO: Possibly move method into a Utility class if we acquire more methods like this one
     */
    protected boolean isNetworkAvailable() {
        // Get the manager
        ConnectivityManager cManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // Find the network info
        NetworkInfo networkInfo = cManager.getActiveNetworkInfo();
        boolean isAvailable = false;

        if (networkInfo != null && networkInfo.isConnected())
            isAvailable = true;

        return isAvailable;
    }

    /**
     * Method for checking if GPS is turned on
     * @return true if turned on
     *
     * TODO: Possibly move method into a Utility class if we acquire more methods like this one
     */
    protected boolean isGpsOn() {
        // Get the manager
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Check our GPS
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Starts the process of obtaining location updates
     */
    private void startLocationUpdates() {
        try {
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback
                    , null);
            Log.d(LOG_TAG, "Running startLocationUpdates()");
        } catch (SecurityException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    /**
     * Stops the process of location updates
     */
    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        Log.d(LOG_TAG, "Running stopLocationUpdates()");
    }

    /**
     * Attempt to center the map when the fab is pressed. This will check
     * @param view
     */
    public void fabButtonClick(View view) {
        if (!isGpsOn()) {
            // Attempt to resolve the GPS issue
            checkLocationSettings();
        } else {
            // Update our camera positioning
            updateCameraMapPosition();
        }
    }
}
