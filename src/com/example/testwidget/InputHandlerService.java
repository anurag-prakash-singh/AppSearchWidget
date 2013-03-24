package com.example.testwidget;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class InputHandlerService extends Service {
	private final String TAG = "NewsWidgetProvider";
	
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Intent received: " + intent.getStringExtra("buttonClicked"));
		
		// send broadcast now.
		sendBroadcast(new Intent(Constants.NEW_ITEMS_INTENT));
		
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
