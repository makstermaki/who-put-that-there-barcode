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

import tw.com.quickmark.sdk.Result;
import tw.com.quickmark.sdk.qmcore;
import tw.com.quickmark.sdk.demo.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public final class CaptureActivity extends Activity implements SurfaceHolder.Callback{

	private static final String TAG = CaptureActivity.class.getSimpleName();
	
	// ViewFinderView
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private TextView fmtText;
	private String result = "";
	
	// Decoder
	private CaptureActivityHandler handler;
	protected qmcore QuickMarkDecoder;
	
	// vibrate
	private static final long VIBRATE_DURATION = 200L;
	
	// Menu Items
	private static final int SETTINGS_ID = Menu.FIRST;
	private static final int ABOUT_ID = Menu.FIRST + 1;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Window window = getWindow();
	    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.capture);
	    
	    CameraManager.init(getApplication());
	    viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
	    fmtText = (TextView)findViewById(R.id.barcodeformat_text);
	    if (fmtText == null)
	    	fmtText = new TextView(this);
	    handler = null;
	    hasSurface = false;
	    
	    /**
	     *  QuickMarkSDK 
	     */
	    QuickMarkDecoder = new qmcore(this);
	    if (!QuickMarkDecoder.mSdkLoaded)
	    {
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle(getString(R.string.app_name));
	        builder.setMessage("Can't load quickmarksdk.");
	        builder.setNegativeButton("ok", null);
	        builder.show();
	    }
	    
	    String decodeFormatSupported = "Supported: ";
		if (QuickMarkDecoder.EQRCodeSupported)
			decodeFormatSupported = decodeFormatSupported + "QRCode";
		if (QuickMarkDecoder.EDataMatrixSupported)
			decodeFormatSupported = decodeFormatSupported + ",Data Matrix";
		if (QuickMarkDecoder.EEanSupported)
			decodeFormatSupported = decodeFormatSupported + ",EAN";
		if (QuickMarkDecoder.ECode39Supported)
			decodeFormatSupported = decodeFormatSupported + ",Code 39";
		if (QuickMarkDecoder.ECode128Supported)
			decodeFormatSupported = decodeFormatSupported + ",Code 128";
		fmtText.setText(decodeFormatSupported);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		if (!hasSurface) {
		      hasSurface = true;
		      initCamera(arg0);
		 }
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		hasSurface = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
	    SurfaceHolder surfaceHolder = surfaceView.getHolder();
	    if (hasSurface) {
	      // The activity was paused but not stopped, so the surface still exists. Therefore
	      // surfaceCreated() won't be called, so init the camera here.
	      initCamera(surfaceHolder);
	    } else {
	      // Install the callback and wait for surfaceCreated() to init the camera.
	      surfaceHolder.addCallback(this);
	      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	    }
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
		      handler.quitSynchronously();
		      handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}

	public Handler getHandler() {
	    return handler;
	  }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// Do nothing, this is to prevent the activity from being restarted when the keyboard opens.
		super.onConfigurationChanged(newConfig);
	}
	
	private void initCamera(SurfaceHolder surfaceHolder) {
	    try {
	      CameraManager.get().openDriver(surfaceHolder);
	    } catch (IOException ioe) {
	      Log.w(TAG, ioe);
	      return;
	    } catch (RuntimeException e) {
	      Log.w(TAG, "Unexpected error initializating camera", e);
	      return;
	    }
	    if (handler == null) {
	      handler = new CaptureActivityHandler(this);
	    }
	}
	
	public void drawViewfinder() {
	    viewfinderView.drawViewfinder();
	  }
	
	public void handleDecode(Result rawResult){
		/**
		 * Result :Encapsulates the result of decoding a barcode within an image.
		 * 
		 * String getText()
		 * @return raw text encoded by the barcode, if applicable, otherwise <code>null</code>
		 * 
		 * BarcodeFormat getBarcodeFormat()
		 * @return {@link BarcodeFormat} representing the format of the barcode that was decoded
		 *
		 * BarcodeFormat :Enumerates barcode formats.
		 * QR_CODE
		 * DATA_MATRIX
		 * EAN_8
		 * EAN_13
		 * UPC_A
		 * UPC_E
		 * CODE_39 
		 * CODE_128
		 */
		
		// Display the result
        if(rawResult.getText().indexOf('*') == -1){
        	playVibrate();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.app_name));
			builder.setMessage(rawResult.getBarcodeFormat().getName() + "---"
					+ rawResult.getText());
			result = rawResult.getText();
			builder.setNegativeButton("ok", okListener);

			builder.show();
        }else{
        	if (handler != null) {
	            handler.sendEmptyMessage(R.id.restart_preview);
	        }
        }
		
		
	}
	
	private final DialogInterface.OnClickListener okListener =
	      new DialogInterface.OnClickListener() {
	    public void onClick(DialogInterface dialogInterface, int i) {
//	    	if (handler != null) {
//	            handler.sendEmptyMessage(R.id.restart_preview);
//	        }


	    	//go back to main class
	    	Intent resultIntent = new Intent();
	    	resultIntent.putExtra("code", result);
	    	setResult(Activity.RESULT_OK, resultIntent);
	    	finish();
	    }
	  };
	  
	private final DialogInterface.OnClickListener aboutListener =
	      new DialogInterface.OnClickListener() {
	    public void onClick(DialogInterface dialogInterface, int i) {
	      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_weburl)));
	      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	      startActivity(intent);
	    }
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, SETTINGS_ID, 0, "Preferences").setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, ABOUT_ID, 0, "About").setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case SETTINGS_ID: 
			{
				Intent intent = new Intent(Intent.ACTION_VIEW);
		        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		        intent.setClassName(this, PreferencesActivity.class.getName());
		        startActivity(intent);
			}
			break;
			case ABOUT_ID:
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        builder.setTitle(getString(R.string.title_about));
		        builder.setMessage(getString(R.string.msg_about) + "\n\n" + getString(R.string.about_weburl));
		        builder.setIcon(R.drawable.icon);
		        builder.setPositiveButton(R.string.button_open_browser, aboutListener);
		        builder.setNegativeButton(R.string.button_cancel, null);
		        builder.show();
			}
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void playVibrate() {
	    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
	    vibrator.vibrate(VIBRATE_DURATION);
	}
}
