package com.example.videomaps;

import android.content.Intent;
import android.hardware.Camera;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class RecordActivity extends FragmentActivity implements SurfaceHolder.Callback {
    private Camera svCam;
    private SurfaceView svCamPreview;
    private SurfaceHolder svCamHolder;
    private MediaRecorder svRecorder;
    private boolean isRecording=false;
    private int mRotateDegree;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat=new SimpleDateFormat("yyyyMMdd-HHmmss");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        svCamPreview=(SurfaceView)findViewById(R.id.svCam);
        ImageButton btnLocation=(ImageButton)findViewById(R.id.btnLocation);
        ImageButton btnSwitch=(ImageButton)findViewById(R.id.btnSwitch);
        ImageButton btnRecord=(ImageButton)findViewById(R.id.btnRecord);
        svCamHolder=svCamPreview.getHolder();
        svCamHolder.addCallback(this);

    }
    @Override
    protected void onResume(){
        svRecorder=new MediaRecorder();
        svCam=Camera.open();
        super.onResume();
    }
    @Override
    protected  void onPause(){
        svCam.stopPreview();
        svCam.release();
        svCam=null;
        super.onPause();
    }
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder){
        try{
            svCam.setPreviewDisplay(svCamHolder);

            Camera.CameraInfo camInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(0, camInfo);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0; break;
                case Surface.ROTATION_90:
                    degrees = 90; break;
                case Surface.ROTATION_180:
                    degrees = 180; break;
                case Surface.ROTATION_270:
                    degrees = 270; break;
            }

            mRotateDegree = (camInfo.orientation - degrees + 360) % 360;
            svCam.setDisplayOrientation(mRotateDegree);

            svCam.startPreview();

            Camera.Parameters camParas = svCam.getParameters();
            if (camParas.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO) ||
                    camParas.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_MACRO))
                svCam.autoFocus(null);
            else
                Toast.makeText(this, "Auto focus is unsupport", Toast.LENGTH_SHORT).show();
        }catch(Exception e){
            Intent actMain=new Intent();
            actMain.setClass(RecordActivity.this,MainActivity.class);
            Toast.makeText(this,"Camera Error",Toast.LENGTH_SHORT).show();
            startActivity(actMain);
            finish();
            return;
        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder,int format,int height, int width){

    }
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder){
        if(isRecording){
            svRecorder.stop();
            isRecording=false;
        }
        svRecorder.release();
        finish();
    }
    
    private void startRecording() {
        try {
            File folder=new File(Environment.getExternalStorageDirectory()+"/MVM");
            boolean isSucess=true;
            if(!folder.exists())
                isSucess=folder.mkdir();
            if(!isSucess) {
                Toast.makeText(this,"Storage Opening Error",Toast.LENGTH_SHORT).show();
                return;
            }
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            String fileName="mvm_"+dateFormat.format(calendar.getTime())+".mp4";
            svRecorder.setCamera(svCam);
            svRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            svRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
            svRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            svRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
            svRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
            svRecorder.setOutputFile(folder+"/" +fileName);
            svRecorder.setPreviewDisplay(svCamHolder.getSurface());
            svRecorder.setOrientationHint(mRotateDegree);
            svRecorder.prepare();
            svRecorder.start();
        } catch (Exception e) {
            Toast.makeText(this, "Recording error", Toast.LENGTH_LONG).show();
        }
    }
}
