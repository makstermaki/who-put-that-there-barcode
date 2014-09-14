/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Above applies only to code derived from the ZXing project.
 */
 
package tw.com.quickmark.sdk.demo;

import java.io.IOException;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

public class CameraManager {

	// Log
	private static final String TAG = CameraManager.class.getSimpleName();
	
	static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT
	static {
	    int sdkInt;
	    try {
	      sdkInt = Integer.parseInt(Build.VERSION.SDK);
	    } catch (NumberFormatException nfe) {
	      // Just to be safe
	      sdkInt = 10000;
	    }
	    SDK_INT = sdkInt;
	}
	
	// Camera
	private static CameraManager cameraManager;  
	private Camera camera;
	private final CameraConfigurationManager configManager;
	private Rect framingRect;
	private Rect framingRectInPreview;	 //for decode
	
	// Camera Handler
	private Handler previewHandler;
	private int previewMessage;
	private Handler autoFocusHandler;
	private int autoFocusMessage;
	private static final long AUTOFOCUS_INTERVAL_MS = 1500L;
	
	// Control flags
	private boolean initialized;
	private boolean previewing;
	private final boolean useOneShotPreviewCallback;
	
	private final Context context;
	
	private static final int MIN_FRAME_WIDTH = 240;
	private static final int MIN_FRAME_HEIGHT = 240;
	private static final int MAX_FRAME_WIDTH = 480;
	private static final int MAX_FRAME_HEIGHT = 360;
	
	/**
	 * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
	 * clear the handler so it will only receive one message.
	 */
	private final Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
		public void onPreviewFrame(byte[] data, Camera camera) {
		      if (!useOneShotPreviewCallback) {
		        camera.setPreviewCallback(null);
		      }
		      if (previewHandler != null) {
		    	Point cameraResolution = configManager.getCameraResolution();
		        Message message = previewHandler.obtainMessage(previewMessage, cameraResolution.x,
		            cameraResolution.y, data);
		        message.sendToTarget();
		        previewHandler = null;
		      }else {
			      Log.d(TAG, "Got preview callback, but no handler for it");
			  }
		}
	};
	
	/** Autofocus callbacks arrive here, and are dispatched to the Handler which requested them. */
	private final Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
		 public void onAutoFocus(boolean success, Camera camera) {
		      if (autoFocusHandler != null) {
		        Message message = autoFocusHandler.obtainMessage(autoFocusMessage, success);
		        // Simulate continuous autofocus by sending a focus request every
			    // AUTOFOCUS_INTERVAL_MS milliseconds.
		        autoFocusHandler.sendMessageDelayed(message, AUTOFOCUS_INTERVAL_MS);   
		        autoFocusHandler = null;
		      }
		 }
	};
	
	/**
	 * Initializes this static object with the Context of the calling Activity.
	 *
	 * @param context The Activity which wants to use the camera.
	 */
	public static void init(Context context) {
	    if (cameraManager == null) {
	      cameraManager = new CameraManager(context);
	    }
	}
	
	/**
	 * Gets the CameraManager singleton instance.
	 *
	 * @return A reference to the CameraManager singleton.
	 */
	public static CameraManager get() {
	  return cameraManager;
	}
	
	private CameraManager(Context context) {
		this.context = context;
		this.configManager = new CameraConfigurationManager(context);
	    camera = null;
		initialized = false;
		previewing = false;
	    
	    // Camera.setOneShotPreviewCallback() has a race condition in Cupcake, so we use the older
	    // Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later, we need to use
	    // the more efficient one shot callback, as the older one can swamp the system and cause it
	    // to run out of memory. We can't use SDK_INT because it was introduced in the Donut SDK.
	    useOneShotPreviewCallback = SDK_INT > Build.VERSION_CODES.CUPCAKE;
	}
	
	/**
	 * Opens the camera driver and initializes the hardware parameters.
	 *
	 * @param holder The surface object which the camera will draw preview frames into.
	 * @throws IOException Indicates the camera driver failed to open.
	 */
	public void openDriver(SurfaceHolder holder) throws IOException {
	    if (camera == null) {
	      camera = Camera.open();
	      if (camera == null) {
	        throw new IOException();
	      }
	      camera.setPreviewDisplay(holder);

	      if (!initialized) {
	        initialized = true;
	        configManager.initFromCameraParameters(camera);
	      }
	      configManager.setDesiredCameraParameters(camera);
	    }
	}
	
	/**
	 * Closes the camera driver if still in use.
	 */
	 public void closeDriver() {
	    if (camera != null) {
	      camera.release();
	      camera = null;
	    }
	 }
	 
	 /**
	  * Asks the camera hardware to begin drawing preview frames to the screen.
	  */
	 public void startPreview() {
	    if (camera != null && !previewing) {
	      camera.startPreview();
	      previewing = true;
	    }
	 }
	 
	 /**
	  * Tells the camera to stop drawing preview frames.
	  */
	  public void stopPreview() {
	    if (camera != null && previewing) {
	      if (!useOneShotPreviewCallback) {
	        camera.setPreviewCallback(null);
	      }
	      camera.stopPreview();
	      previewHandler = null;
	      autoFocusHandler = null;
	      previewing = false;
	    }
	  }
	  
	  /**
	   * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
	   * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
	   * respectively.
	   *
	   * @param handler The handler to send the message to.
	   * @param message The what field of the message to be sent.
	   */
	  public void requestPreviewFrame(Handler handler, int message) {
	    if (camera != null && previewing) {
	    	  previewHandler = handler;
		      previewMessage = message;
	      if (useOneShotPreviewCallback) {
	        camera.setOneShotPreviewCallback(previewCallback);
	      } else {
	        camera.setPreviewCallback(previewCallback);
	      }
	    }
	  }
	  
	  /**
	   * Asks the camera hardware to perform an autofocus.
	   *
	   * @param handler The Handler to notify when the autofocus completes.
	   * @param message The message to deliver.
	   */
	  public void requestAutoFocus(Handler handler, int message) {
	    if (camera != null && previewing) {
	      autoFocusHandler = handler;
		  autoFocusMessage = message;
	      camera.autoFocus(autoFocusCallback);
	    }
	  }
	  
	  /**
	   * Calculates the framing rect which the UI should draw to show the user where to place the
	   * barcode. This target helps with alignment as well as forces the user to hold the device
	   * far enough away to ensure the image will be in focus.
	   *
	   * @return The rectangle to draw on screen in window coordinates.
	   */
	  public Rect getFramingRect() {
	    Point screenResolution = configManager.getScreenResolution();
	    if (framingRect == null) {
	      if (camera == null) {
	        return null;
	      }
	      
	      int width = screenResolution.x * 3 / 4;
	      if (width < MIN_FRAME_WIDTH) {
	        width = MIN_FRAME_WIDTH;
	      } else if (width > MAX_FRAME_WIDTH) {
	        width = MAX_FRAME_WIDTH;
	      }
	      int height = screenResolution.y * 3 / 4;
	      if (height < MIN_FRAME_HEIGHT) {
	        height = MIN_FRAME_HEIGHT;
	      } else if (height > MAX_FRAME_HEIGHT) {
	        height = MAX_FRAME_HEIGHT;
	      }
	      int leftOffset = (screenResolution.x - width) / 2;
	      int topOffset = (screenResolution.y - height) / 2;
	      framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
	      Log.d(TAG, "Calculated framing rect: " + framingRect);
	    }
	    return framingRect;
	  }
	  
	  /**
	   * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
	   * not UI / screen.
	   */
	  public Rect getFramingRectInPreview() {
	    if (framingRectInPreview == null) {
	      Rect rect = new Rect(getFramingRect());
	      Point cameraResolution = configManager.getCameraResolution();
	      Point screenResolution = configManager.getScreenResolution();
	      rect.left = rect.left * cameraResolution.x / screenResolution.x;
	      rect.right = rect.right * cameraResolution.x / screenResolution.x;
	      rect.top = rect.top * cameraResolution.y / screenResolution.y;
	      rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
	      framingRectInPreview = rect;
	    }
	    return framingRectInPreview;
	  }
	  
	  /**
	   * A factory method to build the appropriate DecodeBufferSource object based on the format
	   * of the preview buffers, as described by Camera.Parameters.
	   *
	   * @param data A preview frame.
	   * @param width The width of the image.
	   * @param height The height of the image.
	   * @return A DecodeBufferSource instance.
	   */
	  public DecodeBufferSource buildDecodeBuffer(byte[] data, int width, int height) {
		    Rect rect = getFramingRectInPreview();
		    int previewFormat = configManager.getPreviewFormat();
		    String previewFormatString = configManager.getPreviewFormatString();
		    switch (previewFormat) {
		      // This is the standard Android format which all devices are REQUIRED to support.
		      // In theory, it's the only one we should ever care about.
		      case PixelFormat.YCbCr_420_SP:
		      // This format has never been seen in the wild, but is compatible as we only care
		      // about the Y channel, so allow it.
		      case PixelFormat.YCbCr_422_SP:
		        return new DecodeBufferSource(data, width, height, rect.left, rect.top,
		            rect.width(), rect.height());
		      default:
		        // The Samsung Moment incorrectly uses this variant instead of the 'sp' version.
		        // Fortunately, it too has all the Y data up front, so we can read it.
		        if ("yuv420p".equals(previewFormatString)) {
		          return new DecodeBufferSource(data, width, height, rect.left, rect.top,
		            rect.width(), rect.height());
		        }
		    }
		    throw new IllegalArgumentException("Unsupported picture format: " +
		        previewFormat + '/' + previewFormatString);
		  }
	  
	
}
