package com.example.videomaps;
//For Android services

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
//For location services
import android.location.*;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.*;
import com.google.android.gms.location.places.ui.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
//For text conversion
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
//For database
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

public class MainActivity extends MapActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "Main";

    private static final int zoomToRate = 17;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat dbSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Bundle bundle;
    private ImageButton btnRecord, btnTestPlay, btnLocation;
    private RecyclerView videoList;
    private static PlaceAutocompleteFragment autocompleteFragment;

    private boolean doubleBackToExitPressedOnce = false;
    private static boolean isOnConnectedInit = false;
    private static LatLng selectedLatLng;
    private static boolean hasSearchMarker = false;
    private static Marker searchMarker;
    private boolean backFromIntent = false;
    private static ArrayList<Marker> markerRecordingLoc = new ArrayList<Marker>();

    //Permission variable(Android 6.0 or above)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<String> permissionList = getGrantedPermissions(this);
        String[] permissions = permissionList.toArray(new String[permissionList.size()]);
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
        btnLocation = (ImageButton) findViewById(R.id.btnLocation);
        btnLocation.setOnClickListener(btnLocationListener);

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
        } catch (Exception e) {
        }
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
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    //Event Listener Setup
    private ImageButton.OnClickListener btnRecordListener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View V) {
            Intent actRecord = new Intent();
            actRecord.putExtra("latitude", lat);
            actRecord.putExtra("longitude", lng);
            actRecord.setClass(MainActivity.this, RecordActivity.class);
            backFromIntent = true;
            startActivity(actRecord);

        }
    };

    private ImageButton.OnClickListener btnLocationListener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View V) {
            isOnConnectedInit = false;
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
    }

    @Override
    public void onMapReady(GoogleMap map) {
        super.onMapReady(map);
    }

    // Succeed GoogleApiClient 객체 연결되었을 때 실행
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (!isOnConnectedInit) {
            super.onConnected(bundle);
            isOnConnectedInit = true;
        }
        LatLng currentLatLng = MapActivity.currentLatLng;
        //Set Markers for All Recordings
        showAllRecordingLoc();
        // Place Searching
        placeSearching(currentLatLng);
    }

    // google play service connection
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        super.onConnectionFailed(result);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        super.onConnectionSuspended(cause);
    }

    @Override
    public void onMapClick(LatLng point) {
        super.onMapClick(point);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("backFromIntent:" + backFromIntent);
        if (!backFromIntent) {
            android.os.Process.killProcess(android.os.Process.myPid());
            Toast.makeText(this, "Shouldn't be displayed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
    }

    protected synchronized void buildGoogleApiClient() {
        super.buildGoogleApiClient();
    }

    public boolean checkLocationPermission() {
        return super.checkLocationPermission();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            ActivityCompat.finishAffinity(this);
            finish();
            return;
        }

        if (videoList != null) {
            if (videoList.getVisibility() != View.VISIBLE) {
                this.doubleBackToExitPressedOnce = true;
                Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
            } else {
                showBasicButtons();
                hideVideoList();
                return;
            }
        } else {
            this.doubleBackToExitPressedOnce = true;
        }

        if (hasSearchMarker) {
            searchMarker.remove();
            autocompleteFragment.setText("");
            this.doubleBackToExitPressedOnce = false;
            hasSearchMarker = false;
            return;
        }
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
    }


    public void showAllRecordingLoc() {
        //Set Marker for All Recordings
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = DatabaseHelper.queryPlaceAll(db);
        //Set up the list to save data
        ArrayList<Hashtable<String, Object>> allRecordingLoc = new ArrayList<Hashtable<String, Object>>();
        Hashtable<String, Object> recordingLoc;
        mMap.setOnMarkerClickListener(this);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Cursor mediaCursor = DatabaseHelper.queryMedia(db, cursor.getInt(cursor.getColumnIndex(DatabaseHelper.Place._ID)));
                    if (mediaCursor != null && mediaCursor.getCount() > 0) {
                        recordingLoc = new Hashtable<String, Object>();
                        recordingLoc.put("id", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.Place._ID)));
                        //recordingLoc.put("name",cursor.getString(cursor.getColumnIndex(DatabaseHelper.Place.NAME)));
                        //recordingLoc.put("desc",cursor.getString(cursor.getColumnIndex(DatabaseHelper.Place.DESCRIPTION)));
                        recordingLoc.put("lat", cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.Place.LATITUDE)));
                        recordingLoc.put("lng", cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.Place.LONGITUDE)));
                        allRecordingLoc.add(recordingLoc);
                    }
                } while (cursor.moveToNext());
            }
        }
        recordingMarker(allRecordingLoc);
    }

    //Show Markers for Recording
    public void recordingMarker(ArrayList<Hashtable<String, Object>> allRecordingLoc) {
        for (int i = 0; i < allRecordingLoc.size(); i++) {
            Hashtable<String, Object> recordingLoc = allRecordingLoc.get(i);
            MarkerOptions markerOptions = new MarkerOptions()
                    .title((String) recordingLoc.get("name"))
                    .snippet((String) recordingLoc.get("desc"))
                    .position(new LatLng((double) recordingLoc.get("lat"), (double) recordingLoc.get("lng")));
            markerRecordingLoc.add(mMap.addMarker(markerOptions));
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        hideBasicButtons();
        showVideoList(marker);
        return false;
    }

    public void showVideoList(Marker marker) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        if (marker != null)
            cursor = DatabaseHelper.queryPlace(db, marker.getPosition().latitude, marker.getPosition().longitude);
        final ArrayList<Hashtable<String, Object>> recordingList = new ArrayList<Hashtable<String, Object>>();
        final ArrayList<Hashtable<String, Object>> recordingListView = new ArrayList<Hashtable<String, Object>>();

        if (cursor != null && cursor.getCount() > 0) {
            Hashtable<String, Object> recording;
            Hashtable<String, Object> recordingView;
            cursor.moveToFirst();
            int pid = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.Place._ID));
            cursor = null;
            cursor = DatabaseHelper.queryMedia(db, pid);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String path = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Media.PATH));
                        String filename = path.substring((path.lastIndexOf("/") + 1), (path.lastIndexOf(".")));
                        Date date = null;
                        try {
                            date = dbSdf.parse(cursor.getString(cursor.getColumnIndex(DatabaseHelper.Media.DATE)));
                        } catch (ParseException e) {
                            Toast.makeText(this, "", Toast.LENGTH_SHORT);
                        }
                        recording = new Hashtable<String, Object>();
                        recording.put("id", cursor.getInt(cursor.getColumnIndex(DatabaseHelper.Media._ID)));
                        recording.put("pid", pid);
                        recording.put("path", path);
                        recording.put("date", sdf.format(date));
                        recording.put("lat", marker.getPosition().latitude);
                        recording.put("lng", marker.getPosition().longitude);
                        recordingList.add(recording);
                        recordingView = new Hashtable<String, Object>();
                        recordingView.put("filename", filename);
                        recordingView.put("date", sdf.format(date));
                        recordingListView.add(recordingView);
                    } while (cursor.moveToNext());
                }
            } else {
                Toast.makeText(this, "Marker Error", Toast.LENGTH_SHORT);
            }
        }

        CustomHorizontalAdapter customHorizontalAdapter = new CustomHorizontalAdapter(recordingList, recordingListView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        videoList = (RecyclerView) findViewById(R.id.rvVideoList);
        videoList.setLayoutManager(layoutManager);
        videoList.setAdapter(customHorizontalAdapter);
        videoList.setVisibility(View.VISIBLE);
    }

    public void hideVideoList() {
        if (videoList != null)
            videoList.setVisibility(View.GONE);
    }

    public void showBasicButtons() {
        btnRecord.setVisibility(View.VISIBLE);
        btnLocation.setVisibility(View.VISIBLE);
    }

    public void hideBasicButtons() {
        btnRecord.setVisibility(View.GONE);
        btnLocation.setVisibility(View.GONE);
    }

    // Search Place
    public void placeSearching(LatLng currentLatLng) {
        autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        final double boundRange = 1;
        if (currentLatLng != null) {
            autocompleteFragment.setBoundsBias(new LatLngBounds(
                    new LatLng(currentLatLng.latitude - boundRange, currentLatLng.longitude - boundRange),
                    new LatLng(currentLatLng.latitude + boundRange, currentLatLng.longitude + boundRange)));
        } else {
            autocompleteFragment.setBoundsBias(new LatLngBounds(
                    new LatLng(lat - boundRange, lng - boundRange),
                    new LatLng(lat + boundRange, lng + boundRange)
            ));
        }
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                showBasicButtons();
                hideVideoList();
                // TODO: Get info about the selected place.
                if (hasSearchMarker) {
                    searchMarker.remove();
                    hasSearchMarker = false;
                }
                selectedLatLng = place.getLatLng();
                MarkerOptions selectedMark = new MarkerOptions().position(selectedLatLng).title(place.getName().toString()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(selectedLatLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomToRate));
                searchMarker = mMap.addMarker(selectedMark);
                hasSearchMarker = true;
                isOnConnectedInit = true;
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Toast.makeText(getApplicationContext(), "Search Error: " + status, Toast.LENGTH_SHORT);
            }
        });
    }

    public void removeVideoListItem(LatLng markerPosition, int place_id) {
        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = DatabaseHelper.queryMedia(db, place_id);
        Marker changeMarker = null;
        int markerId;
        for (markerId = 0; markerId < markerRecordingLoc.size(); markerId++) {
            if (markerRecordingLoc.get(markerId).getPosition().equals(markerPosition)) {
                changeMarker = markerRecordingLoc.get(markerId);
                break;
            }
        }

        if (cursor.getCount() > 0) {
            hideVideoList();
            showVideoList(changeMarker);
        } else {
            changeMarker.remove();
            markerRecordingLoc.remove(markerId);
            hideVideoList();
            showBasicButtons();
        }
    }

    public class CustomHorizontalAdapter extends RecyclerView.Adapter<CustomHorizontalAdapter.MyViewHolder> {
        private ArrayList<Hashtable<String, Object>> recordingListView, recordingList;
        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        //RecyclerView Holder
        class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView imgThumb;
            TextView txtFilename, txtDate;

            public MyViewHolder(View view) {
                super(view);
                imgThumb = (ImageView) view.findViewById(R.id.imgThumb);
                txtFilename = (TextView) view.findViewById(R.id.txtFilename);
                txtDate = (TextView) view.findViewById(R.id.txtDate);
            }
        }

        //Construcutor
        public CustomHorizontalAdapter(ArrayList<Hashtable<String, Object>> recordingList, ArrayList<Hashtable<String, Object>> recordingListView) {
            this.recordingList = recordingList;
            this.recordingListView = recordingListView;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_video, parent, false);
            return new MyViewHolder(itemView);
        }

        //Set  Recycler View
        @Override
        public void onBindViewHolder(final MyViewHolder holder, final int position) {
            if (recordingList == null || recordingListView == null)
                return;
            final Hashtable<String, Object> recording = recordingList.get(position);
            final Hashtable<String, Object> recordingView = recordingListView.get(position);
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail((String) recording.get("path"), MediaStore.Video.Thumbnails.MINI_KIND);
            holder.imgThumb.setImageBitmap(thumb);
            holder.txtFilename.setText((String) recordingView.get("filename"));
            holder.txtDate.setText((String) recordingView.get("date"));
            holder.imgThumb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent playIntent = new Intent(MainActivity.this, PlayActivity.class);
                    final String TAG = "Recording Information:";
                    Log.i(TAG, (recording.get("path").toString()) + " "
                            + (recording.get("date").toString()) + " "
                            + ((Double) recording.get("lat") * -1.0) + " "
                            + ((Double) recording.get("lng") * -1.0));
                    playIntent.putExtra("recordingInfo", recording);
                    backFromIntent = true;
                    startActivity(playIntent);
                }
            });

            holder.imgThumb.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    new AlertDialog.Builder(MainActivity.this).setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Delete")
                            .setMessage("Are you sure to delete " + recordingView.get("filename") + " ?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String deletePath = (String) recording.get("path");
                                    System.out.println("Deletion Path:" + deletePath);
                                    int deleteMediaCount = DatabaseHelper.deleteMedia(db, deletePath);
                                    System.out.println("Meida Count:" + deleteMediaCount);
                                    if (deleteMediaCount > 0) {
                                        File deleteFile = new File(deletePath);
                                        boolean isDeleteFile = deleteFile.delete();
                                        System.out.println("Delete Status:" + isDeleteFile);
                                        if (!isDeleteFile)
                                            Toast.makeText(getApplicationContext(), "Deletion Error", Toast.LENGTH_SHORT);
                                    }
                                    LatLng recordingLatLng = new LatLng((Double) recording.get("lat"), (Double) recording.get("lng"));
                                    MainActivity.this.removeVideoListItem(recordingLatLng, (int) recording.get("pid"));
                                }
                            }).setNegativeButton("No", null).show();
                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            return recordingListView.size();
        }
    }
}