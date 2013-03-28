package com.nec.sempro.clientunit;

import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.widget.TextView;

public class AlertNotifier extends Activity {

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
		  MediaPlayer mp = MediaPlayer.create(AlertNotifier.this, R.raw.alerttune);
		  
		 
          OnPreparedListener listener = new MediaPlayer.OnPreparedListener() {
			
			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				
			}
		};
         mp.setOnPreparedListener(listener);
          mp.start();

         
	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.alert_notifier, menu);
		return true;
	}

}
