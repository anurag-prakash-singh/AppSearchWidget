package com.example.testwidget;

import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

public class LettersRemoteViewsService extends RemoteViewsService {
	private static final String TAG = "LettersRemoteViewsService"; 
	
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		Log.i(TAG, "In onGetViewFactory");
		
		return new LetterViewFactory(getApplicationContext(), intent);
	}
}
