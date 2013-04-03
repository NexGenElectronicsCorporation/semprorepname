/* Copyright 2013 Foxdog Studios Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nec.sempro.serverunit.hosting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.nec.sempro.serverunit.AlertDispatch;
//import com.nec.sempro.serverunit.CameraCallback;
import com.nec.sempro.serverunit.MotionDetection;
import com.nec.sempro.serverunit.R;
import com.nec.sempro.serverunit.io.DataSink;
import com.nec.sempro.serverunit.io.DataWriter;
import com.nec.sempro.serverunit.io.FileUtils;

//for bringing preview here
import com.nec.sempro.serverunit.Recorder;
import com.nec.sempro.serverunit.hosting.StreamCameraActivity;


import org.apache.http.conn.util.InetAddressUtils;


public final class StreamCameraActivity extends Activity implements SurfaceHolder.Callback  
{
    private static final String TAG = StreamCameraActivity.class.getSimpleName();
    private SurfaceView mCameraView;
    private static final String PREF_FLASH_LIGHT = "flash_light";
    private static final boolean PREF_FLASH_LIGHT_DEF = false;
    private static final String PREF_PORT = "port";
    private static final int PREF_PORT_DEF = 8080;
    private static final String PREF_JPEG_QUALITY = "jpeg_quality";
    private static final int PREF_JPEG_QUALITY_DEF = 40;

    private boolean mRunning = false;
    public  boolean mPreviewDisplayCreated = false;
       private SurfaceHolder mPreviewDisplay = null;
    private CameraStreamer mCameraStreamer = null;
    
    private String mIpAddress = "";
    private boolean mUseFlashLight = PREF_FLASH_LIGHT_DEF;
    private int mPort = PREF_PORT_DEF;
    private int mJpegQuality = PREF_JPEG_QUALITY_DEF;
    private TextView mIpAddressView = null;
    private LoadPreferencesTask mLoadPreferencesTask = null;
    private SharedPreferences mPrefs = null;
    private MenuItem mSettingsMenuItem = null;

    public SurfaceHolder mHolder; 
	private static final String PREFS_NAME = "prefs_camera";
	private final static String MOTION_DETECTION_KEY = "motion_detection_active";
	// Available from API level 9
	private static final String FOCUS_MODE_CONTINUOS_VIDEO = "continuos-video";
	public Camera mCamera;
	public Context mContext;
	public boolean mMotionDetectionActive;
	
    
    
    public StreamCameraActivity()
    {
        super();
       
    } // constructor()

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        		
      setContentView(R.layout.main);

        new LoadPreferencesTask().execute();

        mPreviewDisplay = ((SurfaceView) findViewById(R.id.camera)).getHolder();
        mPreviewDisplay.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreviewDisplay.addCallback(this);
        mContext = this;
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME,
				mContext.MODE_PRIVATE);
		mMotionDetectionActive = prefs.getBoolean(MOTION_DETECTION_KEY, true);

        mIpAddress = tryGetIpAddress();
        mIpAddressView = (TextView) findViewById(R.id.ip_address);
        updatePrefCacheAndUi();
    } // onCreate(Bundle)

    @Override
    protected void onResume()
    {
        super.onResume();
        mRunning = true;
        if (mPrefs != null)
        {
            mPrefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        } // if
        updatePrefCacheAndUi();
        tryStartCameraStreamer();
    } // onResume()

    @Override
    protected void onPause()
    {
        super.onPause();
        mRunning = false;
        if (mPrefs != null)
        {
            mPrefs.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        } // if
        ensureCameraStreamerStopped();
    } // onPause()

    @Override
    protected void onStop()
    {
        super.onStop();
        finishActivity(0);      
        
    } // onPause()
    @Override
    
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width,
            final int height)
    {
    	Camera.Parameters parameters = mCamera.getParameters();
		parameters.setPreviewSize(width, height);
		mCamera.setParameters(parameters);
		//mCamera.setDisplayOrientation(90);
		mCamera.startPreview(); // Ingored
    } // surfaceChanged(SurfaceHolder, int, int, int)

    @Override
    public void surfaceCreated(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = true;
        Log.i(TAG, "Entered into surfaceCreated");
        mCamera = Camera.open();
        if(mCamera == null) // TODO show Toast
			throw new RuntimeException("Camera is null");
		
		configure(mCamera);
			if(mMotionDetectionActive) {

			try {
				mCamera.setPreviewDisplay(holder);
		    	mPreviewDisplayCreated = true;
				
			} catch (IOException exception) {
				Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
				closeCamera();
			}
			
    } // surfaceCreated(SurfaceHolder) 
		tryStartCameraStreamer();
    }
    @Override
    public void surfaceDestroyed(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = false;
       ensureCameraStreamerStopped();
       	closeCamera();
    } // surfaceDestroyed(SurfaceHolder)

    
    public void tryStartCameraStreamer()
    {
        
    		    	
    	   	
    	if (mRunning && mPreviewDisplayCreated && mPrefs != null)
        {
            mCameraStreamer = new CameraStreamer(mUseFlashLight, mPort, mJpegQuality,
                    mPreviewDisplay,mCamera,mContext);
            
            mCameraStreamer.start();
       } // if
    } // tryStartCameraStreamer()

    public void ensureCameraStreamerStopped()
    {
        if (mCameraStreamer != null)
        {
            mCameraStreamer.stop();
            mCameraStreamer = null;
        } // if
    } // stopCameraStreamer()
    

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        mSettingsMenuItem = menu.add(R.string.settings);
        mSettingsMenuItem.setIcon(android.R.drawable.ic_menu_manage);
        return true;
    } // onCreateOptionsMenu(Menu)

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        if (item != mSettingsMenuItem)
        {
            return super.onOptionsItemSelected(item);
        } // if
        startActivity(new Intent(this, PeepersPreferenceActivity.class));
        return true;
    } // onOptionsItemSelected(MenuItem)

    private final class LoadPreferencesTask extends AsyncTask<Void, Void, SharedPreferences>
    {
        private LoadPreferencesTask()
        {
            super();
        } // constructor()

        @Override
        protected SharedPreferences doInBackground(final Void... noParams)
        {
            return PreferenceManager.getDefaultSharedPreferences(StreamCameraActivity.this);
        } // doInBackground()

        @Override
        protected void onPostExecute(final SharedPreferences prefs)
        {
            StreamCameraActivity.this.mPrefs = prefs;
            prefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceListener);
            updatePrefCacheAndUi();
            tryStartCameraStreamer();
        } // onPostExecute(Void)


    } // class LoadPreferencesTask

    private final OnSharedPreferenceChangeListener mSharedPreferenceListener = 
    		new OnSharedPreferenceChangeListener()
    {
        @Override
        public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key)
        {
            updatePrefCacheAndUi();
        } // onSharedPreferenceChanged(SharedPreferences, String)

    }; // mSharedPreferencesListener

    private final int getPrefInt(final String key, final int defValue)
    {
        // We can't just call getInt because the preference activity
        // saves everything as a string.
        try
        {
            return Integer.parseInt(mPrefs.getString(key, null /* defValue */));
        } // try
        catch (final NullPointerException e)
        {
            return defValue;
        } // catch
        catch (final NumberFormatException e)
        {
            return defValue;
        } // catch
    } // getPrefInt(String, int)

    private final void updatePrefCacheAndUi()
    {
        if (hasFlashLight())
        {
            if (mPrefs != null)
            {
                mUseFlashLight = mPrefs.getBoolean(PREF_FLASH_LIGHT, PREF_FLASH_LIGHT_DEF);
            } // if
            else
            {
                mUseFlashLight = PREF_FLASH_LIGHT_DEF;
            } // else
        } //if
        else
        {
            mUseFlashLight = false;
        } // else

        // XXX: This validation should really be in the preferences activity.
        mPort = getPrefInt(PREF_PORT, PREF_PORT_DEF);
        // The port must be in the range [1024 65535]
        if (mPort < 1024)
        {
            mPort = 1024;
        } // if
        else if (mPort > 65535)
        {
            mPort = 65535;
        } // else if
        mJpegQuality = getPrefInt(PREF_JPEG_QUALITY, PREF_JPEG_QUALITY_DEF);
        // The JPEG quality must be in the range [0 100]
        if (mJpegQuality < 0)
        {
            mJpegQuality = 0;
        } // if
        else if (mJpegQuality > 100)
        {
            mJpegQuality = 100;
        } // else if
        mIpAddressView.setText("http://" + mIpAddress + ":" + mPort + "/");
    } // updatePrefCacheAndUi()

    private boolean hasFlashLight()
    {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    } // hasFlashLight()

    /**
     *  Try to get the IP address of this device. Base on code from
     *  http://stackoverflow.com/a/13007325
     *
     *  @return the first IP address of the device, or null
     */
    public static String tryGetIpAddress()
    {
         	
    	
    	        try {
    	            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
    	            for (NetworkInterface intf : interfaces) {
    	                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
    	                for (InetAddress addr : addrs) {
    	                    if (!addr.isLoopbackAddress()) {
    	                        String sAddr = addr.getHostAddress().toUpperCase();
    	                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); 
    	                        boolean useIPv4 = true;
    	                        if (useIPv4) {
    	                            if (isIPv4) 
    	                                return sAddr;
    	                        } else {
    	                            if (!isIPv4) {
    	                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
    	                                return delim<0 ? sAddr : sAddr.substring(0, delim);
    	                            }
    	                        }
    	                    }
    	                }
    	            }
    	        } catch (Exception ex) { } // for now eat exceptions
    	        return "";
    	    

        
     
    
    } // tryGetIpAddress()
    
    
    //from Preview
    
    
    public void closeCamera() {
		Log.i(TAG, "Closing camera and freeing its resources");

		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
         //	mHolder.removeCallback(this);
		}
	}

	public void configure(Camera camera) {
		Log.i(TAG, "got into configure");
		SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);
		Log.i(TAG, "no problem till now1");
		Camera.Parameters params = camera.getParameters();
		Log.i(TAG, "no problem till now2");
		params.set("jpeg-quality", prefs.getInt("pim.image-quality", 75));
		
		// Configure image format
		List<Integer> formats = params.getSupportedPictureFormats();
		if (formats.contains(PixelFormat.RGB_565))
			params.setPictureFormat(PixelFormat.RGB_565);
		else
			params.setPictureFormat(PixelFormat.JPEG);

		// FIXME Configure picture size, choose the smallest supported for now
		List<Size> sizes = params.getSupportedPictureSizes();
		Camera.Size size = sizes.get(0);// sizes.get(sizes.size()-1);
		params.setPictureSize(size.width, size.height);

		/*
		 * FIXME A wrong config cause the screen to go black on a Milestone so I
		 * just leave the default one. 
		 * sizes = params.getSupportedPreviewSizes(); 
		 * smallestSize = sizes.get(0);
		 * params.setPreviewSize(smallestSize.width, smallestSize.height);
		 */

		List<String> flashModes = params.getSupportedFlashModes();
		// Camera has flash, all flash modes are supported since API level 5
		if (flashModes.size() > 0)
			params.setFlashMode(prefs.getString("pim.camera.flash",
					Camera.Parameters.FLASH_MODE_AUTO));

		// Action mode take pictures of fast moving objects
		List<String> sceneModes = params.getSupportedSceneModes();
		if (sceneModes.contains(Camera.Parameters.SCENE_MODE_ACTION))
			params.setSceneMode(Camera.Parameters.SCENE_MODE_ACTION);
		else
			params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);

		// TODO test FOCUS_MODE_INFINITY against FOCUS_MODE_FIXED
		if (params.getSupportedFocusModes()
				.contains(FOCUS_MODE_CONTINUOS_VIDEO))
			params.setFocusMode(prefs.getString("pim.camera.focus_mode",
					FOCUS_MODE_CONTINUOS_VIDEO));
		else
			params.setFocusMode(prefs.getString("pim.camera.focus_mode",
					Camera.Parameters.FOCUS_MODE_INFINITY));

		camera.setParameters(params);

		if (Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "Picture format: " + params.getPictureFormat());
			Log.d(TAG, "Picture size: " + params.getPictureSize().width + " - "
					+ params.getPictureSize().height);
			Log.d(TAG, "Preview size: " + params.getPreviewSize().width + " - "
					+ params.getPreviewSize().height);
			Log.d(TAG, "Flash mode: " + params.getFlashMode());
			Log.d(TAG, "Scene mode: " + params.getSceneMode());
			Log.d(TAG, "Focus mode: " + params.getFocusMode());
		}
	}
	
/*
//implements SurfaceHolder.Callback 
  
*/

} // class StreamCameraActivity

