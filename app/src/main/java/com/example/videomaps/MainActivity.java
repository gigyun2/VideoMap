package com.example.videomaps;

import android.app.AlertDialog;
import android.content.*;
import android.location.*;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private boolean doubleBackToExitPressedOnce = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        ImageButton btnRecord=(ImageButton)findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(btnRecordListener);
        //Test
        ImageButton btnTestPlay=(ImageButton)findViewById(R.id.btnTestPlay) ;
        btnTestPlay.setOnClickListener(btnTestPlayListener);
        //Test
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
    }

    private ImageButton.OnClickListener btnRecordListener=new ImageButton.OnClickListener(){
        public void onClick(View V){
            Intent actRecord=new Intent();
            actRecord.setClass(MainActivity.this,RecordActivity.class);
            startActivity(actRecord);
        }
    };

    private ImageButton.OnClickListener btnTestPlayListener=new ImageButton.OnClickListener(){
        public void onClick(View V){
            Intent actPlay=new Intent();
            actPlay.setClass(MainActivity.this,PlayActivity.class);
            startActivity(actPlay);
        }
    };

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
    protected void onDestroy(){
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
        Toast.makeText(this, "Still working", Toast.LENGTH_SHORT).show();
    }

}
