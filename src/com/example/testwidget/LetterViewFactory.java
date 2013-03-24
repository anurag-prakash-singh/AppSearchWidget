package com.example.testwidget;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

public class LetterViewFactory implements RemoteViewsFactory {
	private static final String TAG = "LetterViewFactory";
	
	private Context mContext;
	private Intent mIntent;
	
	public LetterViewFactory(Context context, Intent intent) {
		Log.i(TAG, "In LetterViewFactory constructor");
		mContext = context;
		mIntent = intent;
	}
	
	@Override
	public int getCount() {
		return 26;
	}

	@Override
	public long getItemId(int index) {
		return index;
	}

	@Override
	public RemoteViews getLoadingView() {
		return null;
	}

	@Override
	public RemoteViews getViewAt(int index) {
		RemoteViews letterViewLayout =
				new RemoteViews(mContext.getPackageName(),
						R.layout.letter_item_layout);
		
		Log.i(TAG, "Setting text for view at index " + index);
		
		letterViewLayout.setTextViewText(R.id.letterTextView,
				((char)((int)'A' + index)) + "");
		
		return letterViewLayout;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDataSetChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub

	}

}
