package com.example.videomaps;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private static final String TAG = "Main";
    private Bundle bundle;
    private ImageButton btnRecord, btnTestPlay, btnLocation;
    private RecyclerView rvVideoList;

    private boolean doubleBackToExitPressedOnce = false;

    private GoogleMap mMap;
    LocationManager locationManager;
    MapFragment mapFragment;
    boolean setGPS = false;
    public double lng;
    public double lat;
    private GoogleApiClient mGoogleApiClient = null;
    private LocationRequest mLocationRequest;
    private static final int REQUEST_CODE_LOCATION = 2000;
    private static final int REQUEST_CODE_GPS = 2001;
    private static final int REQUEST_ADD_REVIEW = 2002;
    private static final int zoomToRate = 15;
    //Permission variable(Android 6.0 or above)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<String> permissionList= getGrantedPermissions(this);
        String[] permissions=permissionList.toArray(new String[permissionList.size()]);
        final int permissionsAll = 1;
        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, permissionsAll);
        }
        setContentView(R.layout.activity_main);
        bundle = savedInstanceState;
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        btnRecord = (ImageButton) findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(btnRecordListener);
        btnTestPlay = (ImageButton) findViewById(R.id.btnTestPlay);
        btnTestPlay.setOnClickListener(btnTestPlayListener);
        btnLocation = (ImageButton) findViewById(R.id.btnLocation);
        btnLocation.setOnClickListener(btnLocationListener);
        rvVideoList = (RecyclerView) findViewById(R.id.rvVideoList);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // /Grant All Permissions (Android 6.0 or above)
    List<String> getGrantedPermissions(Context context) {
        List<String> granted = new ArrayList<String>();
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (int i = 0; i < pi.requestedPermissions.length; i++) {
                    if ((pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0)
                        granted.add(pi.requestedPermissions[i]);
                }
            }
        } catch (Exception e) {}
        return granted;
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null)
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                    return false;
            }
        return true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    //Event Listener Setup
    private ImageButton.OnClickListener btnRecordListener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View V) {
            Intent actRecord = new Intent();
            actRecord.setClass(MainActivity.this, RecordActivity.class);
            startActivity(actRecord);
        }
    };

    private ImageButton.OnClickListener btnTestPlayListener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View V) {
            Intent actPlay = new Intent();
            actPlay.setClass(MainActivity.this, PlayActivity.class);
            startActivity(actPlay);
        }
    };

    private ImageButton.OnClickListener btnLocationListener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View V) {
            onConnected(bundle);
        }
    };

    // dialog for GPS ON
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_GPS:
                if (resultCode == RESULT_OK) {
                    if (locationManager == null)
                        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        setGPS = true;
                        mapFragment.getMapAsync(this);
                    }
                }
                break;
            case REQUEST_ADD_REVIEW:
                if (resultCode == RESULT_OK) {
                    LatLng latLng = new LatLng(lat, lng);
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    mMap.addMarker(markerOptions);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS)
                showGPSDisabledAlertToUser();

            if (mGoogleApiClient == null)
                buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setCompassEnabled(true);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                Log.d(TAG, "onMapLoaded");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkLocationPermission();
                } else {
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS) {
                        Log.d(TAG, "onMapLoaded");
                        showGPSDisabledAlertToUser();
                    }

                    if (mGoogleApiClient == null)
                        buildGoogleApiClient();
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    mMap.getUiSettings().setCompassEnabled(true);
                }
            }
        });

        // initialize google play services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                buildGoogleApiClient();
            else
                mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomToRate));
        } else
            buildGoogleApiClient();

        mMap.setOnMapClickListener(this);
        /*
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        */
    }

    // after succeeded GoogleApiClient 객체 연결되었을 때 실행
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            setGPS = true;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "onConnected " + "getLocationAvailability mGoogleApiClient.isConnected()=" + mGoogleApiClient.isConnected());
            if (!mGoogleApiClient.isConnected()) mGoogleApiClient.connect();

            if (setGPS && mGoogleApiClient.isConnected()) {
                Log.d(TAG, "onConnected " + "requestLocationUpdates");

                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (location == null)
                    return;
                // Move map to the current location
                mMap.clear();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomToRate));
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
        }
    }

    // google play service connection
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onMapClick(LatLng point) {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(point));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            ActivityCompat.finishAffinity(this);
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lng = location.getLongitude();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    // dialog for GPS enable
    private void showGPSDisabledAlertToUser() {
        android.support.v7.app.AlertDialog.Builder alertDialogBuilder = new android.support.v7.app.AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is currently off. Would you like to turn it on?")
                .setCancelable(false)
                .setPositiveButton("ON", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(callGPSSettingIntent, REQUEST_CODE_GPS);
                    }
                });

        alertDialogBuilder.setNegativeButton("BACK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        android.support.v7.app.AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public boolean checkLocationPermission() {
        Log.d(TAG, "checkLocationPermission");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION))
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
                else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);

                return false;
            } else {
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS)
                    showGPSDisabledAlertToUser();

                if (mGoogleApiClient == null)
                    buildGoogleApiClient();

                else mGoogleApiClient.reconnect();
            }
        } else {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS)
                showGPSDisabledAlertToUser();

            if (mGoogleApiClient == null)
                buildGoogleApiClient();
        }
        return true;
    }

    public void getCurrentLocation() {
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "OnDestroy");
        if (mGoogleApiClient != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);

            if (mGoogleApiClient.isConnected())
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
        Toast.makeText(this, "Shouldn't be displayed", Toast.LENGTH_SHORT).show();
    }

}
