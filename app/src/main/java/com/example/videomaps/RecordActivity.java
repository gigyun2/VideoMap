/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.videomaps;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;

/**
 *  This activity uses the camera/camcorder as the A/V source for the {@link android.media.MediaRecorder} API.
 *  A {@link android.view.TextureView} is used as the camera preview which limits the code to API 14+. This
 *  can be easily replaced with a {@link android.view.SurfaceView} to run on older devices.
 */
public class RecordActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, OnMapReadyCallback, GoogleMap.OnMapClickListener,
        TextureView.SurfaceTextureListener {

    private static final String TAG = "Recorder";

    private Camera mCamera;
    private TextureView mPreview;
    private MediaRecorder mMediaRecorder;
    private File mOutputFile;

    private boolean isRecording = false;
    private ImageButton captureButton;

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
    private CamcorderProfile profile;
    private boolean cameraPrepared = false;

    private TextView timer;
    private long startHTime = 0L;
    private Handler timerHandler = new Handler();
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;

    private Runnable updateTimerThread = new Runnable() {

        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startHTime;

            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            if (timer != null)
                timer.setText("" + String.format("%02d", mins) + ":" + String.format("%02d", secs));
            timerHandler.postDelayed(this, 0);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        // TODO: orientation changes with sensor
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mPreview = (TextureView) findViewById(R.id.surface_view);
        mPreview.setSurfaceTextureListener(this);
        captureButton = (ImageButton) findViewById(R.id.button_capture);
        timer = (TextView) findViewById(R.id.time);// Obtain the SupportMapFragment and get notified when the map is ready to be used.
        //new PreviewPrepareTask().doInBackground();
        //preparePreview();

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);
    }


    // dialog for GPS ON
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult (requestCode, resultCode, data);

        switch (requestCode) {
            case  REQUEST_CODE_GPS :
                if (resultCode == RESULT_OK) {
                    if ( locationManager == null)
                        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                    if ( locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
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
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED))
        {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS)
                showGPSDisabledAlertToUser();

            if (mGoogleApiClient == null)buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    @Override
    public void onMapReady(GoogleMap map)
    {
        mMap = map;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(20));

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                Log.d( TAG, "onMapLoaded" );
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkLocationPermission();
                }
                else
                {
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS) {
                        Log.d(TAG, "onMapLoaded");
                        showGPSDisabledAlertToUser();
                    }

                    if (mGoogleApiClient == null) buildGoogleApiClient();
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                }
            }
        });

        // initialize google play services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                buildGoogleApiClient();
            else
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }
        else
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
        Log.d( TAG, "onConnected" );
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            setGPS = true;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Log.d( TAG, "onConnected " + "getLocationAvailability mGoogleApiClient.isConnected()="+mGoogleApiClient.isConnected() );
            if ( !mGoogleApiClient.isConnected()  ) mGoogleApiClient.connect();

            if (setGPS && mGoogleApiClient.isConnected())
            {
                Log.d( TAG, "onConnected " + "requestLocationUpdates" );
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if ( location == null ) return;
                // Move map to the current location
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
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
    protected void onPause() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();

        super.onPause();
        releaseMediaRecorder();
        releaseCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();

        if (!cameraPrepared) preparePreview();
        //new PreviewPrepareTask().doInBackground();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d( TAG, "OnDestroy");
        if (mGoogleApiClient != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);

            if (mGoogleApiClient.isConnected())
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
        super.onDestroy();
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
    private void showGPSDisabledAlertToUser()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is currently off. Would you like to turn it on?")
                .setCancelable(false)
                .setPositiveButton("ON", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(callGPSSettingIntent, REQUEST_CODE_GPS);
                    }
                });

        alertDialogBuilder.setNegativeButton("BACK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.cancel();
            }
        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public boolean checkLocationPermission()
    {
        Log.d( TAG, "checkLocationPermission");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION))
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
                else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);

                return false;
            }
            else
            {
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS)
                    showGPSDisabledAlertToUser();

                if (mGoogleApiClient == null)
                    buildGoogleApiClient();

                else mGoogleApiClient.reconnect();
            }
        }
        else
        {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS)
                showGPSDisabledAlertToUser();

            if (mGoogleApiClient == null)
                buildGoogleApiClient();
        }
        return true;
    }

    /**
     * When recording, the button click stops recording, releases
     *  {@link android.media.MediaRecorder} and {@link android.hardware.Camera}. When not recording,
     * it prepares the {@link android.media.MediaRecorder} and starts recording.
     *
     * @param view the view generating the event.
     */
    public void onCaptureClick(View view) {
        if (isRecording) {
            // stop recording and release camera
            try {
                mMediaRecorder.stop();  // stop the recording
                timeSwapBuff += timeInMilliseconds;
                timerHandler.removeCallbacks(updateTimerThread);
            } catch (RuntimeException e) {
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored
                mOutputFile.delete();
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder

            // inform the user that recording has stopped
            isRecording = false;
            setCaptureButtonStatus();
            //releaseCamera();
            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        else {
            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            new MediaPrepareTask().execute(null, null, null);
        }
    }

    private void setCaptureButtonStatus() {
        if (isRecording)
            captureButton.setImageDrawable(getResources().getDrawable(R.mipmap.camera_stop));
        else
            captureButton.setImageDrawable(getResources().getDrawable(R.mipmap.camera_record));
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            // release the camera for other applications
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        if (cameraPrepared) {
            try {
                mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
            preparePreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    class PreviewPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            preparePreview();
            return cameraPrepared;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void preparePreview(){
        mCamera = CameraHelper.getDefaultCameraInstance();

        // Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
        Camera.Size optimalSize = mSupportedVideoSizes.get(0);
        //Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
        //        mSupportedPreviewSizes, mPreview.getWidth(), mPreview.getHeight());

        // Use the same size for recording profile.
        profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;

        // likewise for the camera object itself.
        //parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        parameters.setPreviewSize(mSupportedPreviewSizes.get(0).width, mSupportedPreviewSizes.get(0).height);
        mCamera.setParameters(parameters);
        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return;
        }

        cameraPrepared = true;
        return;
    }

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (cameraPrepared) {
                mMediaRecorder = new MediaRecorder();
                mCamera.unlock();
                mMediaRecorder.setCamera(mCamera);
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mMediaRecorder.setProfile(profile);
                mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
                if (mOutputFile == null)
                    return false;
                mMediaRecorder.setOutputFile(mOutputFile.getPath());

                try {
                    mMediaRecorder.prepare();
                } catch (IllegalStateException e) {
                    Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
                    releaseMediaRecorder();
                    return false;
                } catch (IOException e) {
                    Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
                    releaseMediaRecorder();
                    return false;
                }

                startHTime = SystemClock.uptimeMillis();
                timerHandler.postDelayed(updateTimerThread, 0);
                isRecording = true;
                mMediaRecorder.start();
            } else {
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) RecordActivity.this.finish();
            // inform the user that recording has started
            setCaptureButtonStatus();
        }
    }

}