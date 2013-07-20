package com.example.testwidget;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class KeyInputHandler extends Service {
	private static final String TAG = "Kuikk/KeyInputHandler";
	
	public static final String KEY_PRESSED_ACTION = "KEY_PRESSED_ACTION";
	public static final String KEY_CHARACTER_EXTRA = "KEY_CHARACTER_EXTRA";
	public static final String ERASE_ALL_KEY = "bksp_all";
	
	private IBinder mAppSearchServiceBinder = new KeyInputHandlerServiceBinder();
	
	private StringBuffer mLatestInputString = new StringBuffer(); 
	private String mDeleteKeyValue = null;
	private String mShiftKeyValue = null;
	private boolean mShiftMode = false;
	private boolean mAppSearchServiceStarted = false;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Try starting the application search service if it's
		// not already been started. It could already be up even if
		// mAppSearchServiceStarted is false, but calling startService
		// on a started service is harmless anyway.
		if (!mAppSearchServiceStarted) {
			Intent appSearchServiceStartIntent = new Intent(getApplicationContext(),
					AppSearchService.class);
			startService(appSearchServiceStartIntent);
			mAppSearchServiceStarted = true;
		}
		
		if (intent != null && intent.getAction() != null &&
				intent.getAction().equals(KEY_PRESSED_ACTION)) {
			String keyContents = intent.getStringExtra(KEY_CHARACTER_EXTRA);
				
			if (keyContents != null) {
				Log.i(TAG, "Key pressed: " + keyContents);
				
				if (mDeleteKeyValue == null) {
					mDeleteKeyValue = getString(R.string.delete_key_identifier);
				}
				
				if (mShiftKeyValue == null) {
					mShiftKeyValue = getString(R.string.shift_key_identifier);
				}
				
				if (mDeleteKeyValue.equals(keyContents)) {
					if (mLatestInputString.length() >= 1) {
						if (mShiftMode) {
							mLatestInputString.replace(0, mLatestInputString.length(), "");
						} else {
							mLatestInputString.deleteCharAt(mLatestInputString.length() - 1);
						}
					}
				} else if (mShiftKeyValue.equals(keyContents)) {
					// Invert the shift mode
					Log.i(TAG, "Shift key pressed.");
					mShiftMode = !mShiftMode;
				} else if (ERASE_ALL_KEY.equals(keyContents)) {
					// Erase the contents of the input
					mLatestInputString.replace(0, mLatestInputString.length(), "");
				} else {
					if (mShiftMode) {
						// If we're in shift mode, pick the character at position 1.
						// We're ignoring the case of keyContents being empty.
						// That will cause an exception and that's okay.
						if (keyContents.length() > 1) {
							mLatestInputString.append(keyContents.charAt(1));
						} else {
							mLatestInputString.append(keyContents.charAt(0));
						}
					} else {
						mLatestInputString.append(keyContents.charAt(0));
					}
				}

				// Send the stored text to the UI (do we need to send the
				// entire text?)
				Intent enteredTextIntent = new Intent(getApplicationContext(),
						LettersWidgetProvider.class);
				
				enteredTextIntent.setAction(Constants.TEXT_INTENT);
				enteredTextIntent.putExtra(Constants.CURRENT_SHIFT_MODE,
						mShiftMode);
				enteredTextIntent.putExtra(Constants.TYPED_TEXT, mLatestInputString.toString());
				getApplicationContext().sendBroadcast(enteredTextIntent);
				
				// Send the stored text to the list service (do we need to send the
				// entire text?)
				Intent listRefreshIntent = new Intent(getApplicationContext(),
						AppListViewFactory.class);
				
				listRefreshIntent.setAction(Constants.TEXT_INTENT);
				listRefreshIntent.putExtra(Constants.CURRENT_SHIFT_MODE,
						mShiftMode);
				listRefreshIntent.putExtra(Constants.TYPED_TEXT, mLatestInputString.toString());
				getApplicationContext().sendBroadcast(listRefreshIntent);
				
				Log.i(TAG, "Current string: " + mLatestInputString.toString());
				
				// Compute the resulting string, perform the search
				// and pass on the results to the list factory service.
			} else {
				Log.e(TAG, "Unidentified key pressed.");
			}
		}
		
		return START_STICKY;
	}

	public String getLatestInputString() {
		return mLatestInputString.toString();
	}
	
	public class KeyInputHandlerServiceBinder extends Binder {
		KeyInputHandler getService() {
			return KeyInputHandler.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mAppSearchServiceBinder;
	}
}
