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

import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class CameraConfigurationManager {

	private static final String TAG = CameraConfigurationManager.class.getSimpleName();
	private static final Pattern COMMA_PATTERN = Pattern.compile(",");
	
	private final Context context;
	private Point screenResolution;
	private Point cameraResolution;
	private int previewFormat;
	private String previewFormatString;
	  
	CameraConfigurationManager(Context context) {
	    this.context = context;
	}  
	
	/**
	   * Reads, one time, values from the camera that are needed by the app.
	   */
	void initFromCameraParameters(Camera camera) {
	    Camera.Parameters parameters = camera.getParameters();
	    
	    previewFormat = parameters.getPreviewFormat();
	    previewFormatString = parameters.get("preview-format");
	    Log.d(TAG, "Default preview format: " + previewFormat + '/' + previewFormatString);
	    
	    WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
	    Display display = manager.getDefaultDisplay();
	    screenResolution = new Point(display.getWidth(), display.getHeight());
	    Log.d(TAG, "Screen resolution: " + screenResolution);
	    
	    cameraResolution = getCameraResolution(parameters, screenResolution);
	    Log.d(TAG, "Camera resolution: " + screenResolution);
	}
	
	/**
	   * Sets the camera up to take preview images which are used for both preview and decoding.
	   * We detect the preview format here so that buildLuminanceSource() can build an appropriate
	   * LuminanceSource subclass. In the future we may want to force YUV420SP as it's the smallest,
	   * and the planar Y can be used for barcode scanning without a copy in some cases.
	   */
	void setDesiredCameraParameters(Camera camera) {
	    Camera.Parameters parameters = camera.getParameters();
	    Log.d(TAG, "Setting preview size: " + cameraResolution);
	    parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
	    
	    // This is the standard setting to turn the flash off that all devices should honor.
	    parameters.set("flash-mode", "off");
	    
	    // Set zoom to 2x if available. This helps encourage the user to pull back.
	    // Some devices like the Behold have a zoom parameter
	    parameters.set("zoom", "2.0");
	    // Most devices, like the Hero, appear to expose this zoom parameter.
	    // (I think) This means 2.0x
	    parameters.set("taking-picture-zoom", "20");
	    
	    camera.setParameters(parameters);
	}
	
	Point getCameraResolution() {
	    return cameraResolution;
	}

	Point getScreenResolution() {
	    return screenResolution;
	}

	int getPreviewFormat() {
	    return previewFormat;
	}

	String getPreviewFormatString() {
	    return previewFormatString;
	}
	
	private static Point getCameraResolution(Camera.Parameters parameters, Point screenResolution) {
		String previewSizeValueString = parameters.get("preview-size-values");
	    
		// saw this on Xperia
	    if (previewSizeValueString == null) {
	      previewSizeValueString = parameters.get("preview-size-value");
	    }

	    Point cameraResolution = null;

	    if (previewSizeValueString != null) {
	      Log.d(TAG, "preview-size-values parameter: " + previewSizeValueString);
	      cameraResolution = findBestPreviewSizeValue(previewSizeValueString, screenResolution);
	    }

	    if (cameraResolution == null) {
	      // Ensure that the camera resolution is a multiple of 8, as the screen may not be.
	      cameraResolution = new Point(
	          (screenResolution.x >> 3) << 3,
	          (screenResolution.y >> 3) << 3);
	    }

	    return cameraResolution;
	}
	
	private static Point findBestPreviewSizeValue(String previewSizeValueString,
		      Point screenResolution) {
		    int bestX = 0;
		    int bestY = 0;
		    int diff = Integer.MAX_VALUE;
		    for (String previewSize : COMMA_PATTERN.split(previewSizeValueString)) {

		      previewSize = previewSize.trim();
		      int dimPosition = previewSize.indexOf('x');
		      if (dimPosition < 0) {
		        Log.w(TAG, "Bad preview-size: " + previewSize);
		        continue;
		      }

		      int newX;
		      int newY;
		      try {
		        newX = Integer.parseInt(previewSize.substring(0, dimPosition));
		        newY = Integer.parseInt(previewSize.substring(dimPosition + 1));
		      } catch (NumberFormatException nfe) {
		        Log.w(TAG, "Bad preview-size: " + previewSize);
		        continue;
		      }

		      int newDiff = Math.abs(newX - screenResolution.x) + Math.abs(newY - screenResolution.y);
		      if (newDiff == 0) {
		        bestX = newX;
		        bestY = newY;
		        break;
		      } else if (newDiff < diff) {
		        bestX = newX;
		        bestY = newY;
		        diff = newDiff;
		      }

		    }

		    if (bestX > 0 && bestY > 0) {
		      return new Point(bestX, bestY);
		    }
		    return null;
	}
	
}
