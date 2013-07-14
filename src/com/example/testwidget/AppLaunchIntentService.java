package com.example.testwidget;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class AppLaunchIntentService extends IntentService {
	private static String TAG = "Kuikk/AppLaunchIntentService";
		
	public AppLaunchIntentService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "Launching " + intent.getStringExtra(Constants.COMPONENT_NAME));
	}
}
