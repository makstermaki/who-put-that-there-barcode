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
import tw.com.quickmark.sdk.demo.R;
import android.os.Handler;
import android.os.Message;

public class CaptureActivityHandler extends Handler {

	private static final String TAG = CaptureActivityHandler.class.getSimpleName();
	
	private final CaptureActivity activity;
	private final DecodeThread decodeThread;
	private State state;

	  private enum State {
	    PREVIEW,
	    SUCCESS,
	    DONE
	  }
	  
	CaptureActivityHandler(CaptureActivity activity){
		this.activity = activity;
		decodeThread = new DecodeThread(activity);
		decodeThread.start();
		state = State.SUCCESS;
		
		// Start ourselves capturing previews and decoding.
	    CameraManager.get().startPreview();
	    restartPreviewAndDecode();
	}

	@Override
	public void handleMessage(Message msg) {
		if (msg.what == R.id.auto_focus)
			if (state == State.PREVIEW) {
		          CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
		    }
		if (msg.what == R.id.restart_preview)
			restartPreviewAndDecode();
		if (msg.what == R.id.decode_succeeded){
			state = State.SUCCESS;
			activity.handleDecode((Result) msg.obj);
		}
		if (msg.what == R.id.decode_failed){
			state = State.PREVIEW;
	        CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
		}
	}
	
	public void quitSynchronously() {
		state = State.DONE;
	    CameraManager.get().stopPreview();
	    Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
	    quit.sendToTarget();
	    try {
	      decodeThread.join();
	    } catch (InterruptedException e) {
	    }

	    // Be absolutely sure we don't send any queued up messages
	    removeMessages(R.id.decode_succeeded);
	    removeMessages(R.id.decode_failed);
	}
	
	private void restartPreviewAndDecode() {
	    if (state == State.SUCCESS) {
	      state = State.PREVIEW;
	      CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
	      CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
	      activity.drawViewfinder();
	    }
	}
}
