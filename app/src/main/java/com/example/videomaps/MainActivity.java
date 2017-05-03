package com.example.videomaps;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.*;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.*;
import com.google.android.gms.location.places.*;
import com.google.android.gms.location.places.ui.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import android.database.Cursor;

public class MainActivity extends MapActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "Main";
    private Bundle bundle;
    private ImageButton btnRecord, btnTestPlay, btnLocation;
    //private RecyclerView rvVideoList;
    private ListView lvVideoList;
    private boolean doubleBackToExitPressedOnce = false;

    private static final int zoomToRate = 17;

    private static LatLng selectedLatLng;
    private static boolean hasSearchMarker=false;
    private static Marker searchMarker;
    //Permission variable(Android 6.0 or above
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
        btnTestPlay = (ImageButton) findViewById(R.id.btnTestPlay);
        btnTestPlay.setOnClickListener(btnTestPlayListener);
        btnLocation = (ImageButton) findViewById(R.id.btnLocation);
        btnLocation.setOnClickListener(btnLocationListener);
        //rvVideoList = (RecyclerView) findViewById(R.id.rvVideoList);
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
            if ((setGPS && mGoogleApiClient.isConnected())) { // TODO: if marker selected
                actRecord.putExtra("latitude", lat);
                actRecord.putExtra("longitude", lng);
            } else {
                actRecord.putExtra("latitude", 1000);
                actRecord.putExtra("longitude", 1000);
            }
            actRecord.setClass(MainActivity.this, RecordActivity.class);
            startActivity(actRecord);
        }
    };

    private ImageButton.OnClickListener btnTestPlayListener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View V) {
            Intent actPlay = new Intent();
            //actPlay.putExtra("path", );
            if ((setGPS && mGoogleApiClient.isConnected())) { // TODO: if marker selected
                actPlay.putExtra("latitude", lat);
                actPlay.putExtra("longitude", lng);
            } else {
                actPlay.putExtra("latitude", 1000);
                actPlay.putExtra("longitude", 1000);
            }
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
    public void onMapReady(GoogleMap map) {
        super.onMapReady(map);
    }

    // Succeed GoogleApiClient 객체 연결되었을 때 실행
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        super.onConnected(bundle);

        LatLng currentLatLng=null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (setGPS && mGoogleApiClient.isConnected()) {
                Log.d(TAG, "onConnected " + "requestLocationUpdates");

                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (location == null)
                    return;
                // Move map to the current location
                mMap.clear();
                currentLatLng= new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomToRate));
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
        }
        //Set Markers for All Recordings
        showAllRecordingLoc();
        // Place Searching
        placeSearching(currentLatLng);
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
        super.onLocationChanged(location);
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

    @Override
    public void onMapClick(LatLng point) {
        super.onMapClick(point);
    }
    
    public void showAllRecordingLoc(){
        //Set Marker for All Recordings
        DatabaseHelper dbHelper=new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor=DatabaseHelper.queryPlaceAll(db);

        ArrayList<Hashtable<String,Object>> allRecordingLoc=new ArrayList<Hashtable<String, Object>>();
        Hashtable<String,Object> recordingLoc=new Hashtable<String,Object>();

        if(cursor!=null){
            if(cursor.moveToFirst()){
                do{
                    recordingLoc.put("id",cursor.getInt(cursor.getColumnIndex("id")));
                    recordingLoc.put("name",cursor.getString(cursor.getColumnIndex("name")));
                    recordingLoc.put("desc",cursor.getString(cursor.getColumnIndex("description")));
                    recordingLoc.put("lat",cursor.getDouble(cursor.getColumnIndex("latitude")));
                    recordingLoc.put("lng",cursor.getDouble(cursor.getColumnIndex("longitude")));
                    allRecordingLoc.add(recordingLoc);
                }while(cursor.moveToNext());
            }
        }
        recordingMarker(allRecordingLoc);
    }
    //Show Markers for Recording
    public void recordingMarker(ArrayList<Hashtable<String,Object>> allRecordingLoc){
        Marker[] makerRecordingLoc=new Marker[allRecordingLoc.size()];
        for(int i=0;i<allRecordingLoc.size();i++){
            Hashtable<String,Object> recording=allRecordingLoc.get(i);
            LatLng testLatLng=new LatLng((double)recording.get("lat"),(double)recording.get("lng"));
            System.out.println(testLatLng.toString());
            MarkerOptions markerOptions=new MarkerOptions().title((String)recording.get("name"))
                    .snippet((String)recording.get("desc"))
                    .position(new LatLng((double)recording.get("lat"),(double)recording.get("lng")));
            makerRecordingLoc[i]=mMap.addMarker(markerOptions);
        }
    }
    @Override
    public boolean onMarkerClick(Marker marker) {
        showVideoList(marker);

        /*lvVideoList=(ListView)findViewById(R.id.lvVideoList);
        final ArrayList<String> pathList=new ArrayList<String>();
        ArrayList<String> fileNameList=new ArrayList<String>();
        ArrayList<String> pidList=new ArrayList<String>();
        ArrayList<String> placeNameList=new ArrayList<String>();
        ArrayList<String> latList=new ArrayList<String>();
        ArrayList<String> longList=new ArrayList<String>();
        ArrayList<String> descList=new ArrayList<String>();
        ArrayAdapter<String> adapter;
        Cursor cursor=DatabaseHelper.queryMedia(db,);

        if(cursor!=null){
            if(cursor.moveToFirst()){
                do{
                    String path= cursor.getString(cursor.getColumnIndex("path"));
                    pathList.add(path);
                    String filename=path.substring(path.lastIndexOf("/")+1);
                    fileNameList.add(filename);
                }while(cursor.moveToNext());
            }
        }
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,fileNameList);
        lvVideoList.setAdapter(adapter);
        lvVideoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent=new Intent(MainActivity.this,PlayActivity.class);
                String path = (String)pathList.get(i);
                intent.putExtra("path",path);
                startActivity(intent);
            }
        });*/
        return false;
    }

    public void showVideoList(Marker marker){
        DatabaseHelper dbHelper=new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor=DatabaseHelper.queryPlace(db,marker.getPosition().latitude,marker.getPosition().longitude);
        cursor.moveToFirst();
        int markerId=cursor.getInt(cursor.getColumnIndex("id"));
        cursor=DatabaseHelper.queryMedia(db,markerId);
        ArrayList<Hashtable<String,Object>> recordingList=new ArrayList<Hashtable<String,Object>>();
        Hashtable<String,Object> recording=new Hashtable<String,Object>();
        if(cursor!=null){
            if(cursor.moveToFirst()){
                do{
                    String path=cursor.getString(cursor.getColumnIndex("path"));
                    String filename=path.substring((path.lastIndexOf("/")+1));
                    SimpleDateFormat sdf=new SimpleDateFormat("dd/MM/YYYY", Locale.getDefault());
                    Date date= null;
                    try {
                        date = sdf.parse(cursor.getString(cursor.getColumnIndex("date")));
                    } catch (ParseException e) {
                        Toast.makeText(this,"Time Format Error",Toast.LENGTH_SHORT);
                        continue;
                    }
                    recording.put("id",cursor.getInt(cursor.getColumnIndex("id")));
                    recording.put("path",path);
                    recording.put("filename",filename);
                    recording.put("date",date);
                    recordingList.add(recording);
                }while(cursor.moveToNext());
            }
        }

    }
    // Search Place
    public void placeSearching(LatLng currentLatLng){
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        final double boundRange=1;
        if(currentLatLng!=null) {
            autocompleteFragment.setBoundsBias(new LatLngBounds(
                    new LatLng(currentLatLng.latitude - boundRange, currentLatLng.longitude - boundRange),
                    new LatLng(currentLatLng.latitude + boundRange, currentLatLng.longitude + boundRange)));
        }else{
            autocompleteFragment.setBoundsBias(new LatLngBounds(
                    new LatLng(lat-boundRange,lng-boundRange),
                    new LatLng(lat+boundRange,lng+boundRange)
            ));
        }
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                if(hasSearchMarker){
                    searchMarker.remove();
                    hasSearchMarker=false;
                }
                selectedLatLng=place.getLatLng();
                MarkerOptions selectedMark=new MarkerOptions().position(selectedLatLng).title(place.getName().toString());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(selectedLatLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomToRate));
                searchMarker=mMap.addMarker(selectedMark);
                hasSearchMarker=true;
            }
            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Toast.makeText(getApplicationContext(), "Search Error: " + status,Toast.LENGTH_SHORT);
            }
        });
    }
}
