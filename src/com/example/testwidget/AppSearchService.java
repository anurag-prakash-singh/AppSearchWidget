package com.example.testwidget;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AppSearchService extends Service {
	private static final String TAG = "AppSearchService";
	
	public static final String KEY_PRESSED_ACTION = "KEY_PRESSED_ACTION";
	public static final String KEY_CHARACTER_EXTRA = "KEY_CHARACTER_EXTRA";
	
	private StringBuffer mLatestInputString = new StringBuffer(); 
	private String mDeleteKeyValue = null;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.getAction() != null &&
				intent.getAction().equals(KEY_PRESSED_ACTION)) {
			String keyContents = intent.getStringExtra(KEY_CHARACTER_EXTRA);
				
			if (keyContents != null) {
				Log.i(TAG, "Key pressed: " + keyContents);
				
				if (mDeleteKeyValue == null) {
					mDeleteKeyValue = getString(R.string.delete_key_identifier);
				}
				
				if (mDeleteKeyValue != null && mDeleteKeyValue.equals(keyContents)) {
					if (mLatestInputString.length() >= 1) {
						mLatestInputString.deleteCharAt(mLatestInputString.length() - 1);
					}
				} else {
					mLatestInputString.append(keyContents);
				}
				
				Log.i(TAG, "Current string: " + mLatestInputString.toString());
				
				// Compute the resulting string, perform the search
				// and pass on the results to the list factory service.
			} else {
				Log.e(TAG, "Unidentified key pressed.");
			}
		}
		
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
