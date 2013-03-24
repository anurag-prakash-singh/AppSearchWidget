package com.example.testwidget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class NewsWidgetProvider extends AppWidgetProvider {
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		if (intent.getAction().equals(Constants.NEW_ITEMS_INTENT)) {
			Log.i(TAG, "New items available.");
			CharSequence[] newItems = intent.getCharSequenceArrayExtra("newItems");
		}
	}

	private static String TAG = "NewsWidgetProvider"; 
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		Log.i(TAG, "onUpdate called");

		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.news_widget_layout);
		Intent helloClickedIntent = new Intent(context, InputHandlerService.class);
		helloClickedIntent.putExtra("buttonClicked", "btnHello");
		PendingIntent helloClickedPendingIntent = PendingIntent.getService(context, 0, helloClickedIntent, 0);
		views.setOnClickPendingIntent(R.id.btnHello, helloClickedPendingIntent);

		Log.i(TAG, "Set pending click handling intent 2");
		
		
		appWidgetManager.updateAppWidget(appWidgetIds, views);
	}
	
	
}
