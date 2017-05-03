package com.example.videomaps;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.location.*;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.*;
import com.google.android.gms.location.places.*;
import com.google.android.gms.location.places.ui.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import android.database.Cursor;

public class MainActivity extends MapActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "Main";
    private Bundle bundle;
    private ImageButton btnRecord, btnTestPlay, btnLocation;
    private RecyclerView videoList;
    //private ListView videoList;
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
    private static final int zoomToRate = 17;

    private static LatLng selectedLatLng;
    private static boolean hasSearchMarker=false;
    private static Marker searchMarker;
    private SimpleDateFormat sdf=new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
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
            actRecord.putExtra("latitude", lat);
            actRecord.putExtra("longitude", lng);
            actRecord.setClass(MainActivity.this, RecordActivity.class);
            startActivity(actRecord);
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
        super.onActivityResult(requestCode,resultCode,data);
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
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
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
            return;
        }

        if(videoList.getVisibility()!=View.VISIBLE) {
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        }else {
            showBasicButtons();
            hideVideoList();
        }
    }


    public void showAllRecordingLoc(){
        //Set Marker for All Recordings
        DatabaseHelper dbHelper=new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor=DatabaseHelper.queryPlaceAll(db);

        ArrayList<Hashtable<String,Object>> allRecordingLoc=new ArrayList<Hashtable<String, Object>>();
        Hashtable<String,Object> recordingLoc;
        mMap.setOnMarkerClickListener(this);
        //Test data
        recordingLoc=new Hashtable<String,Object>();
        recordingLoc.put("id",12345);
        recordingLoc.put("name","United College");
        recordingLoc.put("desc","My College");
        recordingLoc.put("lat",22.421);
        recordingLoc.put("lng",114.205);
        allRecordingLoc.add(recordingLoc);
        recordingLoc=new Hashtable<String,Object>();
        recordingLoc.put("id",12346);
        recordingLoc.put("name","Chung Chi College");
        recordingLoc.put("desc","Earilest College");
        recordingLoc.put("lat",22.416);
        recordingLoc.put("lng",114.208);
        allRecordingLoc.add(recordingLoc);
        //Test data
        if(cursor!=null){
            if(cursor.moveToFirst()){
                do{
                    recordingLoc=new Hashtable<String,Object>();
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
        Marker[] markerRecordingLoc=new Marker[allRecordingLoc.size()];
        for(int i=0;i<allRecordingLoc.size();i++){
            Hashtable<String,Object> recording=allRecordingLoc.get(i);
            LatLng testLatLng=new LatLng((double)recording.get("lat"),(double)recording.get("lng"));
            System.out.println(testLatLng.toString());
            MarkerOptions markerOptions=new MarkerOptions().title((String)recording.get("name"))
                    .snippet((String)recording.get("desc"))
                    .position(new LatLng((double)recording.get("lat"),(double)recording.get("lng")));
            markerRecordingLoc[i]=mMap.addMarker(markerOptions);
        }
    }
    @Override
    public boolean onMarkerClick(final Marker marker) {
        hideBasicButtons();
        showVideoList(marker);
        return false;
    }

    public void showVideoList(Marker marker){
        DatabaseHelper dbHelper=new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor=DatabaseHelper.queryPlace(db,marker.getPosition().latitude,marker.getPosition().longitude);
        int markerId;
        if(cursor!=null&&cursor.getCount()>0){
            cursor.moveToFirst();
            markerId=cursor.getInt(cursor.getColumnIndex("id"));
            cursor=null;
            cursor=DatabaseHelper.queryMedia(db,markerId);
        }

        final ArrayList<Hashtable<String,Object>> recordingList=new ArrayList<Hashtable<String,Object>>();
        final ArrayList<Hashtable<String,Object>> recordingListView=new ArrayList<Hashtable<String,Object>>();
        Hashtable<String,Object> recording;
        Hashtable<String,Object> recordingView;
        //Test data
        //Entry 1
        recording=new Hashtable<String,Object>();
        recording.put("id",12345);
        recording.put("path","/storage/emulated/0/DCIM/mvm/VID_20170502_212054.mp4");
        try {
            recording.put("date",sdf.format(sdf.parse("02/05/2017")));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        recording.put("lat",22.421);
        recording.put("lng",114.205);
        recordingList.add(recording);
        recordingView=new Hashtable<String,Object>();
        recordingView.put("filename","VID_20170502_212054.mp4");
        try {
            recordingView.put("date",sdf.format(sdf.parse("02/05/2017")));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        recordingListView.add(recordingView);
        //Entry 2
        recording=new Hashtable<String,Object>();
        recording.put("id",12346);
        recording.put("path","/storage/emulated/0/DCIM/mvm/VID_20170503_004742.mp4");
        try {
            recording.put("date",sdf.format(sdf.parse("03/05/2017")));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        recording.put("lat",22.421);
        recording.put("lng",114.205);
        recordingList.add(recording);
        recordingView=new Hashtable<String,Object>();
        recordingView.put("filename","VID_20170503_004742.mp4");
        try {
            recordingView.put("date",sdf.format(sdf.parse("03/05/2017")));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        recordingListView.add(recordingView);
        //Test data
        if(cursor!=null){
            if(cursor.moveToFirst()){
                do{
                    String path=cursor.getString(cursor.getColumnIndex("path"));
                    String filename=path.substring((path.lastIndexOf("/")+1));
                    Date date= null;
                    try {
                        date = sdf.parse(cursor.getString(cursor.getColumnIndex("date")));
                    } catch (ParseException e) {
                        Toast.makeText(this,"Time Format Error",Toast.LENGTH_SHORT);
                        continue;
                    }
                    recording=new Hashtable<String,Object>();
                    recording.put("id",cursor.getInt(cursor.getColumnIndex("id")));
                    recording.put("path",path);
                    recording.put("date",sdf.format(date));
                    recording.put("lat",marker.getPosition().latitude);
                    recording.put("lng",marker.getPosition().longitude);
                    recordingList.add(recording);
                    recordingView=new Hashtable<String,Object>();
                    recordingView.put("filename",filename);
                    recordingView.put("date",sdf.format(date));
                    recordingListView.add(recordingView);
                }while(cursor.moveToNext());
            }
        }

        /*ListAdapter adapter=new SimpleAdapter(this,recordingListView,android.R.layout.simple_list_item_2,new String[]{"filename","date"},new int[]{ android.R.id.text1,android.R.id.text2});
        videoList.setAdapter(adapter);
        videoList.setVisibility(View.VISIBLE);
        videoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent playIntent=new Intent(MainActivity.this,PlayActivity.class);
                Hashtable<String,Object>recordingInfo=recordingList.get(i);
                System.out.println(recordingInfo.get("path").toString());
                System.out.println(recordingInfo.get("date").toString());
                System.out.println((Double)recordingInfo.get("lat")*-1.0);
                System.out.println((Double)recordingInfo.get("lng")*-1.0);
                playIntent.putExtra("recordingInfo",recordingInfo);
                startActivity(playIntent);
            }
        });*/
        CustomHorizontalAdapter customHorizontalAdapter=new CustomHorizontalAdapter(recordingList,recordingListView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        videoList = (RecyclerView) findViewById(R.id.rvVideoList);
        videoList.setLayoutManager(layoutManager);
        videoList.setAdapter(customHorizontalAdapter);
        videoList.setVisibility(View.VISIBLE);
    }

    public void hideVideoList(){
        if(videoList!=null)
            videoList.setVisibility(View.GONE);
    }

    public void showBasicButtons(){
        btnRecord.setVisibility(View.VISIBLE);
        btnLocation.setVisibility(View.VISIBLE);
    }

    public void hideBasicButtons(){
        btnRecord.setVisibility(View.GONE);
        btnLocation.setVisibility(View.GONE);
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
                showBasicButtons();
                hideVideoList();
                // TODO: Get info about the selected place.
                if(hasSearchMarker){
                    searchMarker.remove();
                    hasSearchMarker=false;
                }
                selectedLatLng=place.getLatLng();
                MarkerOptions selectedMark=new MarkerOptions().position(selectedLatLng).title(place.getName().toString()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
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


    public class CustomHorizontalAdapter extends RecyclerView.Adapter<CustomHorizontalAdapter.MyViewHolder> {
        private ArrayList<Hashtable<String,Object>> recordingListView,recordingList;
        //RecyclerView Holder
        class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView imgThumb;
            TextView txtFilename,txtDate;

            public MyViewHolder(View view) {
                super(view);
                imgThumb=(ImageView)view.findViewById(R.id.imgThumb);
                txtFilename = (TextView) view.findViewById(R.id.txtFilename);
                txtDate=(TextView)view.findViewById(R.id.txtDate);
            }
        }
        //Construcutor
        public CustomHorizontalAdapter(ArrayList<Hashtable<String,Object>> recordingList,ArrayList<Hashtable<String,Object>> recordingListView) {
            this.recordingList=recordingList;
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
            final Hashtable<String,Object> recording=recordingList.get(position);
            final Hashtable<String,Object> recordingView=recordingListView.get(position);
            Bitmap thumb=ThumbnailUtils.createVideoThumbnail((String)recording.get("path"), MediaStore.Video.Thumbnails.MINI_KIND);
            holder.imgThumb.setImageBitmap(thumb);
            holder.txtFilename.setText((String)recordingView.get("filename"));
            holder.txtDate.setText((String)recordingView.get("date"));
            holder.imgThumb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent playIntent=new Intent(MainActivity.this,PlayActivity.class);
                    System.out.print("Recording Info:");
                    System.out.print(recording.get("path").toString()+" ");
                    System.out.print(recording.get("date").toString()+" ");
                    System.out.print((Double)recording.get("lat")*-1.0+" ");
                    System.out.println((Double)recording.get("lng")*-1.0);
                    playIntent.putExtra("recordingInfo",recording);
                    startActivity(playIntent);
                }
            });
        }
        @Override
        public int getItemCount() {
            return recordingListView.size();
        }
    }
}