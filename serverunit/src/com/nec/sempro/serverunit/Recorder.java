package com.nec.sempro.serverunit;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

public class Recorder 
{
	
	MediaRecorder recorder = new MediaRecorder();
	

	
	public Recorder (Camera camera)
	
	{
	
		camera.unlock();
	
	recorder.setCamera(camera);
	recorder.setPreviewDisplay(null);  
	recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
	recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	 CamcorderProfile camprof;
	 camprof = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
	 camprof.videoFrameRate=25;
	 camprof.duration=60;
	 
	 camprof.fileFormat=MediaRecorder.OutputFormat.MPEG_4;
	 camprof.audioCodec=MediaRecorder.AudioEncoder.DEFAULT;
	 camprof.videoCodec= MediaRecorder.VideoEncoder.DEFAULT;
	 recorder.setProfile(camprof);
	 	 
	 File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
             Environment.DIRECTORY_PICTURES), "Surveillance Storage");
   

   if (! mediaStorageDir.exists()){
       if (! mediaStorageDir.mkdirs()){
           Log.d("Surveillance Storage", "failed to create directory");
           
       }
   }
   String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
   //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
   File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_"+ timeStamp + ".mp4");
   String filename = mediaFile.getPath();
     recorder.setOutputFile(filename);
     
     
         
     
     try {
         recorder.prepare();
     } catch (IllegalStateException e) {
         camera.lock();
    	 recorder.release();
         
     } catch (IOException e) {
         camera.lock();
         recorder.release();
     }    
     
     recorder.start();
     try {
         		
    	 Thread.sleep(60000);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	 recorder.stop();
	 recorder.reset();   
	 camera.lock();
	 recorder.release();
	 
   	

	}
}
