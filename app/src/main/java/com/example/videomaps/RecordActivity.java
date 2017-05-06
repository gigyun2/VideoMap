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

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *  This activity uses the camera/camcorder as the A/V source for the {@link android.media.MediaRecorder} API.
 *  A {@link android.view.TextureView} is used as the camera preview which limits the code to API 14+. This
 *  can be easily replaced with a {@link android.view.SurfaceView} to run on older devices.
 */
public class RecordActivity extends MapActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = "Recorder";

    private Camera mCamera;
    private TextureView mPreview;
    private MediaRecorder mMediaRecorder;
    private File mOutputFile;
    private CamcorderProfile profile;
    private boolean isRecording = false;
    private boolean cameraPrepared = false;
    private enum face { FRONT, BACK };
    private face mfacing = face.BACK;
    private Button captureButton;
    private Button switchButton;

    private int rotation;
    private int result;
    private int pid;
    private double mLat;
    private double mLng;

    private Dialog dialog;

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

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            pid = intent.getExtras().getInt("place_id", -1);
            mLat = intent.getExtras().getDouble("latitude", 1000);
            mLng = intent.getExtras().getDouble("longitude", 1000);
        } else {
            pid = -1;
            mLat = 1000;
            mLng = 1000;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mPreview = (TextureView) findViewById(R.id.surface_view);
        mPreview.setSurfaceTextureListener(this);
        captureButton = (Button) findViewById(R.id.button_capture);
        switchButton = (Button) findViewById(R.id.button_switch);
        timer = (TextView) findViewById(R.id.time);// Obtain the SupportMapFragment and get notified when the map is ready to be used.
        //new PreviewPrepareTask().doInBackground();
        //preparePreview();

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getView().setClickable(false);
        mapFragment.getMapAsync(this);

        lat = mLat;
        lng = mLng;

        dialog = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        releaseCamera();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!cameraPrepared) preparePreview();
        //new PreviewPrepareTask().doInBackground();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaRecorder();
        releaseCamera();
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
                timerHandler.removeCallbacks(updateTimerThread);
                timer.setText("00:00");
            } catch (RuntimeException e) {
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored
                mOutputFile.delete();
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder

            if ((!setGPS && !mGoogleApiClient.isConnected()) ||
                ((mLat > 90 || mLat < -90) && (mLng > 180 || mLng < -180))) {
                dialog.setContentView(R.layout.map_dialog);
                dialog.setCancelable(true);
                dialog.findViewById(R.id.map_dialog_ok).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mLat = lat;
                        mLng = lng;
                        dialog.cancel();
                    }
                });
                dialog.show();

                MapFragment dialogMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.dialog_mapView);
                dialogMapFragment.getMapAsync(this);
            }

            DatabaseHelper dh = new DatabaseHelper(this);
            SQLiteDatabase db = dh.getWritableDatabase();
            if (pid != -1) dh.addMedia(db, mOutputFile.getName(), pid);
            else {
                Cursor c = dh.queryPlace(db, mLat, mLng);
                if (c.moveToFirst()) {
                    pid = c.getInt(c.getColumnIndex(DatabaseHelper.Place._ID));
                    dh.addMedia(db, mOutputFile.getPath(), pid);
                }
                else {
                    pid = (int) dh.addPlace(db, null, mLat, mLng, null);
                    dh.addMedia(db, mOutputFile.getPath(), pid);
                }
            }

            // inform the user that recording has stopped
            isRecording = false;
            setCaptureButtonStatus();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            new MediaPrepareTask().execute(null, null, null);
        }
    }


    @Override
    public void onMapReady(GoogleMap map) {
        super.onMapReady(map);

        if ((!setGPS && !mGoogleApiClient.isConnected()) ||
                ((mLat > 90 || mLat < -90) && (mLng > 180 || mLng < -180))) {
            // Set default location to HK
            CameraUpdate center= CameraUpdateFactory.newLatLng(new LatLng(22.3964, 114.1095));
            CameraUpdate zoom=CameraUpdateFactory.zoomTo(8);

            map.moveCamera(center);
            map.animateCamera(zoom);
        }
    }

    private void setCaptureButtonStatus() {
        int padding_in_px = (int) (15 * getResources().getDisplayMetrics().density + 0.5f);
        if (isRecording) {
            captureButton.setBackgroundDrawable(null);
            captureButton.setBackgroundResource(R.mipmap.camera_stop);
            switchButton.setClickable(false);
            switchButton.setBackgroundDrawable(null);
            timer.setBackgroundDrawable(getResources().getDrawable(R.drawable.common_google_signin_btn_icon_dark_normal_background));
            timer.setPadding(padding_in_px,padding_in_px,padding_in_px,padding_in_px);
        }
        else {
            captureButton.setBackgroundDrawable(null);
            captureButton.setBackgroundResource(R.mipmap.camera_record);
            switchButton.setClickable(true);
            switchButton.setBackgroundResource(R.mipmap.camera_switch);
            timer.setBackgroundDrawable(getResources().getDrawable(R.drawable.common_google_signin_btn_icon_light_normal_background));
            timer.setPadding(padding_in_px,padding_in_px,padding_in_px,padding_in_px);
        }
    }

    public void onSwitchClick(View view) {
        releaseMediaRecorder();
        releaseCamera();
        if (mfacing == face.FRONT)
            mfacing = face.BACK;
        else
            mfacing = face.FRONT;
        preparePreview();
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
            cameraPrepared = false;
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        releaseMediaRecorder();
        releaseCamera();
        preparePreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) { }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        releaseMediaRecorder();
        releaseCamera();

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        if (rotation != this.getWindowManager().getDefaultDisplay().getRotation()) {
            releaseMediaRecorder();
            releaseCamera();
            preparePreview();
        }
    }

    class PreviewPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            preparePreview();
            return cameraPrepared;
        }
    }

    private void preparePreview(){
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        if (mfacing == face.FRONT)
            mCamera = CameraHelper.getDefaultFrontFacingCameraInstance();
            android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, info);
        if (mfacing == face.BACK)
            mCamera = CameraHelper.getDefaultBackFacingCameraInstance();
            android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);

        //int degree = (this.getResources().getConfiguration().orientation - 1) * 90;
        rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);

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
                mCamera.setDisplayOrientation(result);
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