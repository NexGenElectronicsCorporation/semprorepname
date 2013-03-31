package com.nec.sempro.clientunit;

import java.io.IOException;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.widget.TextView;

public class AlertNotifier extends Activity {
	
	
	MediaPlayer mp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alert_notifier);
		AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		int maxvol = audioManager.getStreamMaxVolume(audioManager.STREAM_MUSIC);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxvol, 0);
		Intent intent = getIntent();
		String str = intent.getExtras().getString("sendstr"); 
		TextView t= (TextView) findViewById(R.id.poptext);
	      t.setText(str);
	     mp =  MediaPlayer.create(AlertNotifier.this, R.raw.alerttune);
		  
		 try {
             mp.prepareAsync();
         } catch (IllegalStateException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         } 
          
         mp.start();

         
	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.alert_notifier, menu);
		return true;
	}




	protected void onPause()
	{
		mp.pause();
		super.onPause();
	}
	protected void onStop()
	{
		mp.pause();
		mp.stop();
		super.onStop();
	}
	public void onDestroy()
	{
		super.onDestroy();
	}
}
