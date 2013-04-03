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
import java.io.OutputStream;

import com.nec.sempro.serverunit.AlertDispatch;
import com.nec.sempro.serverunit.MotionDetection;
import com.nec.sempro.serverunit.Recorder;
import com.nec.sempro.serverunit.io.DataSink;
import com.nec.sempro.serverunit.io.DataWriter;
import com.nec.sempro.serverunit.io.FileUtils;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

/* package */ final class CameraStreamer extends Object
{
    private static final String TAG = CameraStreamer.class.getSimpleName();

    private static final int MESSAGE_TRY_START_STREAMING = 0;
    private static final int MESSAGE_SEND_PREVIEW_FRAME = 1;

    private static final long OPEN_CAMERA_POLL_INTERVAL_MS = 1000L;

    private final Object mLock = new Object();
    private final MovingAverage mAverageSpf = new MovingAverage(50 /* numValues */);

    private final boolean mUseFlashLight;
    private final int mPort;
    private final int mJpegQuality;
    private final SurfaceHolder mPreviewDisplay;

    private boolean mRunning = false;
    private Looper mLooper = null;
    private Handler mWorkHandler = null;
    private Camera mCamera = null;
    private Camera mCamera1 = null;
    private int mPreviewFormat = Integer.MIN_VALUE;
    private int mPreviewWidth = Integer.MIN_VALUE;
    private int mPreviewHeight = Integer.MIN_VALUE;
    private Rect mPreviewRect = null;
    private int mPreviewBufferSize = Integer.MIN_VALUE;
    private MemoryOutputStream mJpegOutputStream = null;
    private MJpegHttpStreamer mMJpegHttpStreamer = null;
    public Context SCact;

    private long mNumFrames = 0L;
    private long mLastTimestamp = Long.MIN_VALUE;

    /* package */ CameraStreamer(final boolean useFlashLight, final int port,
            final int jpegQuality, final SurfaceHolder previewDisplay, Camera cameraarg,Context CSGL )
    {
    	
    	super();
    	Log.i(TAG,"Started Camera Stream contructor");
    	SCact = CSGL;
        if (previewDisplay == null)
        {
            throw new IllegalArgumentException("previewDisplay must not be null");
        } // if
        mCamera1 = cameraarg;
        mUseFlashLight = useFlashLight;
        mPort = port;
        mJpegQuality = jpegQuality;
        mPreviewDisplay = previewDisplay;
        
        
        
        
    } // constructor(SurfaceHolder)

    private final class WorkHandler extends Handler
    {
        private WorkHandler(final Looper looper)
        {
            super(looper);
        } // constructor(Looper)

        @Override
        public void handleMessage(final Message message)
        {
            switch (message.what)
            {
                case MESSAGE_TRY_START_STREAMING:
                    tryStartStreaming();
                    break;
                case MESSAGE_SEND_PREVIEW_FRAME:
                    final Object[] args = (Object[]) message.obj;
                    sendPreviewFrame((byte[]) args[0], (Camera) args[1], (Long) args[2]);
                    break;
                default:
                    throw new IllegalArgumentException("cannot handle message");
            } // switch
        } // handleMessage(Message)
    } // class WorkHandler

    /* package */ void start()
    {
        synchronized (mLock)
        {
            if (mRunning)
            {
                throw new IllegalStateException("CameraStreamer is already running");
            } // if
            mRunning = true;
        } // synchronized

        final HandlerThread worker = new HandlerThread(TAG, Process.THREAD_PRIORITY_MORE_FAVORABLE);
        worker.setDaemon(true);
        worker.start();
        mLooper = worker.getLooper();
        mWorkHandler = new WorkHandler(mLooper);
        mWorkHandler.obtainMessage(MESSAGE_TRY_START_STREAMING).sendToTarget();
    } // start()

    /**
     *  Stop the image streamer. The camera will be released during the
     *  execution of stop() or shortly after it returns. stop() should
     *  be called on the main thread.
     */
    /* package */ void stop()
    {
        synchronized (mLock)
        {
            if (!mRunning)
            {
                throw new IllegalStateException("CameraStreamer is already stopped");
            } // if

            mRunning = false;
            if (mMJpegHttpStreamer != null)
            {
                mMJpegHttpStreamer.stop();
            } // if
            if (mCamera != null)
            {
                mCamera.release();
                mCamera = null;
            } // if
        } // synchronized
        mLooper.quit();
    } // stop()

    private void tryStartStreaming()
    {
    	Log.i(TAG,"got into this1");
    	 //You can delete this temp
    	CameraCallback	mCameraCallback = new CameraCallback(SCact, mCamera1);
    	 Log.i(TAG,"got into this2");
    	mCamera1.setPreviewCallback(mCameraCallback);
				//You can delete this temp 
    	Log.i(TAG,"got into this3");
    	
    	
    	
    	
    	try
        {
            while (true)
            {
                try
                {
                  
                    startStreamingIfRunning();
                    
                } //try
                catch (final RuntimeException openCameraFailed)
                {
                    Log.d(TAG, "Open camera failed, retying in " + OPEN_CAMERA_POLL_INTERVAL_MS
                            + "ms", openCameraFailed);
                    Thread.sleep(OPEN_CAMERA_POLL_INTERVAL_MS);
                    continue;
                } // catch
               break;
            } // while
        } // try
        catch (final Exception startPreviewFailed)
        {
            // Captures the IOException from startStreamingIfRunning and
            // the InterruptException from Thread.sleep.
            Log.w(TAG, "Failed to start camera preview", startPreviewFailed);
        } // catch
    } // tryStartStreaming()

    private void startStreamingIfRunning() throws IOException
    {
        // Throws RuntimeException if the camera is currently opened
        // by another application.
        //final Camera camera = Camera.open();
    	final Camera camera = mCamera1;
    	if (camera == null)
        {
            throw new IllegalStateException("CameraStreamer is already running");
        }
    	final Camera.Parameters params = camera.getParameters();
        if (mUseFlashLight)
        {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        } // if

        // Set Preview FPS range. The range with the greatest maximum
        // is returned first.
        final int[] range = params.getSupportedPreviewFpsRange().get(0);
        params.setPreviewFpsRange(range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
        camera.setParameters(params);

        // Set up preview callback
        mPreviewFormat = params.getPreviewFormat();
        final Camera.Size previewSize = params.getPreviewSize();
        mPreviewWidth = previewSize.width;
        mPreviewHeight = previewSize.height;
        final int BITS_PER_BYTE = 8;
        final int bytesPerPixel = ImageFormat.getBitsPerPixel(mPreviewFormat) / BITS_PER_BYTE;
        // XXX: According to the documentation the buffer size can be
        // calculated by width * height * bytesPerPixel. However, this
        // returned an error saying it was too small. It always needed
        // to be exactly 1.5 times larger.
        mPreviewBufferSize = mPreviewWidth * mPreviewHeight * bytesPerPixel * 3 / 2 + 1;
        camera.addCallbackBuffer(new byte[mPreviewBufferSize]);
        mPreviewRect = new Rect(0, 0, mPreviewWidth, mPreviewHeight);
       // camera.setPreviewCallbackWithBuffer(mPreviewCallback);

        // We assumed that the compressed image will be no bigger than
        // the uncompressed image.
        mJpegOutputStream = new MemoryOutputStream(mPreviewBufferSize);
        
        final MJpegHttpStreamer streamer = new MJpegHttpStreamer(mPort, mPreviewBufferSize);
        streamer.start();
        
        synchronized (mLock)
        {
        	Log.i(TAG, "no problem till now1");
        	if (!mRunning)
            {
                streamer.stop();
                camera.release();
                return;
            } // if

           /* try
            {
                camera.setPreviewDisplay(mPreviewDisplay);
            } // try
            catch (final IOException e)
            {
                streamer.stop();
                camera.release();
                throw e;
            } // catch
           */
            mMJpegHttpStreamer = streamer;
            //camera.startPreview();
            mCamera = camera;
            Log.i(TAG, "no problem till now2");
        } // synchronized
    } // startStreamingIfRunning()
/*
    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback()
    {
        @Override
        public void onPreviewFrame(final byte[] data, final Camera camera)
        {
            final Long timestamp = SystemClock.elapsedRealtime();
            final Message message = mWorkHandler.obtainMessage();
            message.what = MESSAGE_SEND_PREVIEW_FRAME;
            message.obj = new Object[]{ data, camera, timestamp };
            message.sendToTarget();
        } // onPreviewFrame(byte[], Camera)
    }; // mPreviewCallback
*/
   private void sendPreviewFrame(final byte[] data, final Camera camera, final long timestamp)
   {
        // Calcalute the timestamp
        final long MILLI_PER_SECOND = 1000L;
        final long timestampSeconds = timestamp / MILLI_PER_SECOND;

        // Update and log the frame rate
        final long LOGS_PER_FRAME = 10L;
        mNumFrames++;
        if (mLastTimestamp != Long.MIN_VALUE)
        {
            mAverageSpf.update(timestampSeconds - mLastTimestamp);
            if (mNumFrames % LOGS_PER_FRAME == LOGS_PER_FRAME - 1)
            {
                Log.d(TAG, "FPS: " + 1.0 / mAverageSpf.getAverage());
            } // if
        } // else

        mLastTimestamp = timestampSeconds;

        // Create JPEG
        final YuvImage image = new YuvImage(data, mPreviewFormat, mPreviewWidth, mPreviewHeight,
                null /* strides */);
        image.compressToJpeg(mPreviewRect, mJpegQuality, mJpegOutputStream);

        mMJpegHttpStreamer.streamJpeg(mJpegOutputStream.getBuffer(), mJpegOutputStream.getLength(),
                timestamp);

        // Clean up
        mJpegOutputStream.seek(0);
        // XXX: I believe that this is thread-safe because we're not
        // calling methods in other threads. I might be wrong, the
        // documentation is not clear.
        camera.addCallbackBuffer(data);
   } // sendPreviewFrame(byte[], camera, long)


   final class CameraCallback implements Camera.PreviewCallback, 
   Camera.PictureCallback {

   private final String PICTURE_PREFIX = "/Pictures/pim/";
   private static final int PICTURE_DELAY = 4500;

   private static final String TAG = "CameraCallback";
   private MotionDetection mMotionDetection;
   private Camera mCamera;
   private Context mContextcb;

   private long mReferenceTime;
   private DataWriter mDataWriter;


   public CameraCallback(Context ct, Camera camera) {
   	mDataWriter = new DataWriter();
   	mContextcb = ct;
   	mCamera = camera;
   	//new Recorder(mCamera);
   	mMotionDetection = new MotionDetection(ct.getSharedPreferences(
   			MotionDetection.PREFS_NAME, Context.MODE_PRIVATE));
   }

   @Override
   public void onPictureTaken(byte[] data, Camera camera) {
   	Log.i(TAG, "Picture Taken");

   	String pictureName = System.currentTimeMillis()+".jpg";
   	File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Surveillance Storage");
      

      if (! mediaStorageDir.exists()){
          if (! mediaStorageDir.mkdirs()){
              Log.d("Surveillance Storage", "failed to create directory");
              
          }
      }
   	
   	
   	File f = new File(
   			mediaStorageDir.getPath(),pictureName);
   	FileOutputStream fos = null;
   	try {
   		FileUtils.touch(f);
   		fos = new FileOutputStream(f);
   		Toast.makeText(mContextcb, "Picture captured", Toast.LENGTH_SHORT).show();
   	} catch (IOException e) {
   		Log.e(TAG, "Cannot write picture to disk");
   		e.printStackTrace();
   	}
   	
   	DataSink<FileOutputStream>df = new DataSink<FileOutputStream>(data,fos);
   	mDataWriter.writeAsync(df);

   	mCamera.startPreview();
   }

   @Override
   public void onPreviewFrame(byte[] data, Camera camera) {
   	 
	   //temp
	   final Long timestamp = SystemClock.elapsedRealtime();
       final Message message = mWorkHandler.obtainMessage();
       message.what = MESSAGE_SEND_PREVIEW_FRAME;
       message.obj = new Object[]{ data, camera, timestamp };
       message.sendToTarget();
	   
	   //temp
	    
	   Context adContext = SCact;
   	  	 if (mMotionDetection.detect(data)) {
   		// the delay is necessary to avoid taking a picture while in the 
   		// middle of taking another. This problem causes a Motorola 
   		// Milestone to reboot.
   		long now = System.currentTimeMillis();
   		if (now > mReferenceTime + PICTURE_DELAY) {
   			mReferenceTime = now + PICTURE_DELAY;
   			Log.i(TAG, "Taking picture");
   			camera.takePicture(null, null, this);
   			AlertDispatch ad = new AlertDispatch(adContext);
   			 
   		} else {
   			Log.i(TAG, "Not taking picture because not enough time has "
   					+ "passed since the creation of the Surface");
   		}
   	}
   }
   }
} // class CameraStreamer

