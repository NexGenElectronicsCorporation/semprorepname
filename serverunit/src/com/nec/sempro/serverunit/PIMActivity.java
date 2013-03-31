package com.nec.sempro.serverunit;



import com.nec.sempro.serverunit.hosting.StreamCameraActivity;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class PIMActivity extends Activity {
	
	private static final String TAG = "Server Unit";
	private Button launchButton;

    
		/** Called when the activity is first created. */
	    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pim);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
	        launchButton = (Button) findViewById(R.id.launch_button);
	        
	        launchButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.i(TAG, "Starting StreamCameraActivity");
					Intent i = new Intent(PIMActivity.this,StreamCameraActivity.class);
					startActivity(i);
				}
			});
	    }
	
}
