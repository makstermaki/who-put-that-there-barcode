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

import tw.com.quickmark.sdk.Result;
import tw.com.quickmark.sdk.qmcore;
import tw.com.quickmark.sdk.demo.R;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;

public class DecodeThread extends Thread{
	private final CaptureActivity activity;
	private Handler handler;
	private qmcore QuickMarkDecoder;
	private int decodeFormat;
	
	DecodeThread(CaptureActivity activity){
		this.activity = activity;
		
		this.QuickMarkDecoder= activity.QuickMarkDecoder;
		
		
		if(PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PreferencesActivity.PREFERENCE_QRCODE, true))
			decodeFormat |= qmcore.TWOD_QRCODE;
		if(PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PreferencesActivity.PREFERENCE_DATAMATRIX, true))	
			decodeFormat |= qmcore.TWOD_DATAMATRIX;
		if(PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PreferencesActivity.PREFERENCE_1D_EAN, true))		
			decodeFormat |= qmcore.ONED_EAN;
		if(PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PreferencesActivity.PREFERENCE_1D_CODE39, true))
			decodeFormat |= qmcore.ONED_CODE39;
		if(PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(PreferencesActivity.PREFERENCE_1D_CODE128, true))
			decodeFormat |= qmcore.ONED_CODE128;
		
	}

	@Override
	public void run() {
		super.run();
		
		Looper.prepare();
		handler = new Handler() {
			@Override
		    public void handleMessage(Message message) {
				if (message.what == R.id.decode)
					decode((byte[]) message.obj, message.arg1, message.arg2);
				if (message.what == R.id.quit)
					Looper.myLooper().quit();
			}
	    };
		Looper.loop();
	}
	
	Handler getHandler() {
	    return handler;
	}
	
	/**
	   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
	   * reuse the same reader objects from one decode to the next.
	   *
	   * @param data   The YUV preview frame.
	   * @param width  The width of the preview frame.
	   * @param height The height of the preview frame.
	   */
	private void decode(byte[] data, int width, int height) {
		
		DecodeBufferSource source = CameraManager.get().buildDecodeBuffer(data, width, height);
		
		/**
	     *  QuickMarkSDK Decoder
	     *  Param : 
	     *  	byte[] imageBuffer    	: The data of the bitmap.
	     *  	int    imageWidth   	: The width of the bitmap.
	     *  	int    imageHeight  	: The height of the bitmap.
	     *  	int    imageBitDepth   	: The number of bits used to represent the color of a single pixel.
	     *  	int    decodeFormat 	: Scan formats requested in parameters.(all formats if none specified)
	     *  Output:
	     *  	String					: result.
	     */
		Result rawResult = QuickMarkDecoder.decode(source.getMatrix(), source.getWidth(), source.getHeight(), 8, decodeFormat);
		
		if (rawResult != null) {
			Message message = Message.obtain(activity.getHandler(), R.id.decode_succeeded, rawResult);
			message.sendToTarget();
		}else{
			Message message = Message.obtain(activity.getHandler(), R.id.decode_failed);
		    message.sendToTarget();
		}
	}
}
