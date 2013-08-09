package com.example.testwidget;

import java.util.ArrayList;
import java.util.Collections;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

public class AppListViewFactory implements RemoteViewsFactory {
	private static final String TAG = "Kuikk/AppListViewFactory";
	
	private Context mContext;
	private PackageManager mPackageManager;
	private KeyInputHandler mKeyInputHandler;
	private AppSearchService mAppSearchService;
	private ArrayList<ApplicationListItem> mSearchedAppListItems =
			new ArrayList<ApplicationListItem>();
	private ArrayList<String> mHighlightedSearchResults = new ArrayList<String>();
	private Object mAppListSynch = new Object();
	// 2 MB app icon cache should be sufficient -- we may even need to 
	// reduce this further since this is a widget and we need to keep its
	// memory footprint low
	private int mAppIconCacheMaxSizeKb = 2 * 1024;
	private String mSearchString;
	
	private LruCache<String, Bitmap> mAppIconCache = new LruCache<String, Bitmap> (mAppIconCacheMaxSizeKb) {
		@Override
		protected int sizeOf(String key, Bitmap value) {
			// The size is measured in kilobytes.
			return value.getByteCount() / 1024;
		}
	};
	
	private void addAppBitmapToCache(String componentName, Bitmap bitmap) {
		if (getAppBitmapFromCache(componentName) == null) {
			if (bitmap != null) {
				mAppIconCache.put(componentName, bitmap);
			} else {
				// TODO
				// If an icon can't be found, put a place holder instead
			}
		}
	}
	
	private Bitmap getAppBitmapFromCache(String componentName) {
		return mAppIconCache.get(componentName);
	}

	private ServiceConnection mInputHandlerConnection = new ServiceConnection() {		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mAppSearchService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			KeyInputHandler.KeyInputHandlerServiceBinder keyInputHandlerBinder =
					(KeyInputHandler.KeyInputHandlerServiceBinder) service;
			mKeyInputHandler = keyInputHandlerBinder.getService();
		}
	};
	
	private ServiceConnection mAppSearchServiceConnection = new ServiceConnection() {		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// Ignore.
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			AppSearchService.AppSearchServiceBinder appSearchServiceBinder =
					(AppSearchService.AppSearchServiceBinder) service;
			mAppSearchService = appSearchServiceBinder.getService();
		}
	};

	public AppListViewFactory(Context context, Intent intent) {
		Log.i(TAG, "In LetterViewFactory constructor");
		mContext = context;
		
		String text = intent.getStringExtra(Constants.TYPED_TEXT);
		
		if (text != null) {
			Log.i(TAG, "Entered text: " + text);
		} else {
			Log.i(TAG, "No text found.");
		}		
	}
	
	public PackageManager packageManager() {
		if (mPackageManager == null) {
			mPackageManager = mContext.getPackageManager();			
		}
		
		return mPackageManager;
	}
	
	@Override
	public int getCount() {
		synchronized (mAppListSynch) {
			if (mSearchString == null || mSearchString.equals("")) {
				return 1;
			} else if (mSearchString.length() > 0 && mSearchedAppListItems.size() == 0) {
				// A search term was typed but no matching apps were found.
				return 1;
			} else {			
				return mSearchedAppListItems.size();
			}
		}
	}

	@Override
	public long getItemId(int index) {
		return index;
	}

	@Override
	public RemoteViews getLoadingView() {
		Log.i(TAG, "Returning loading view");
		RemoteViews loadingViewLayout =
				new RemoteViews(mContext.getPackageName(),
						R.layout.loading_view_layout);
		
		return loadingViewLayout;
	}

	@Override
	public RemoteViews getViewAt(int index) {
		if ((mSearchString == null || mSearchString.equals("")) && index == 0) {
			// There's nothing to search for. Return a view
			// that just prompts the user to type in the app
			// to look for
			RemoteViews noSearchTermLayout =
					new RemoteViews(mContext.getPackageName(),
							R.layout.no_search_term_layout);
			
			Log.i(TAG, "Nothing to search for");
			
			return noSearchTermLayout;
		} else if (mSearchString.length() > 0 && mSearchedAppListItems.size() == 0) {
			RemoteViews searchFailedLayout =
					new RemoteViews(mContext.getPackageName(),
							R.layout.search_failed_layout);
			Log.i(TAG, "Search failed for " + mSearchString);
			
			searchFailedLayout.setTextViewText(R.id.search_failed_for_app,
					Html.fromHtml("No apps matching <font color=\"#33b5e5\">" + mSearchString + "</font>"));
			
			return searchFailedLayout;
		}
		
		RemoteViews letterViewLayout =
				new RemoteViews(mContext.getPackageName(),
						R.layout.letter_item_layout);

		synchronized (mAppListSynch) {
			if (index > mSearchedAppListItems.size() - 1) {
				Log.i(TAG, "Returning null view");
				return null;
			}
			
			ApplicationListItem applicationListItem = mSearchedAppListItems.get(index);
			letterViewLayout.setTextViewText(R.id.app_name,
					Html.fromHtml(mHighlightedSearchResults.get(index)));
			
			Intent fillInLaunchIntent = new Intent();
			fillInLaunchIntent.putExtra(Constants.COMPONENT_PACKAGE, mSearchedAppListItems.get(index).getComponentName().getPackageName());
			String componentClassName =  mSearchedAppListItems.get(index).getComponentName().getClassName();
			
			fillInLaunchIntent.putExtra(Constants.COMPONENT_CLASS, componentClassName);
			// Not setting the fill-in intent for the TextView and the icon because
			// it was preventing the list item selector animation from kicking in
//			letterViewLayout.setOnClickFillInIntent(R.id.app_name, fillInLaunchIntent);
//			letterViewLayout.setOnClickFillInIntent(R.id.app_icon, fillInLaunchIntent);
			letterViewLayout.setOnClickFillInIntent(R.id.app_icon_name_group, fillInLaunchIntent);
			
			// Load the icon for this app.
			// We can block here so it's alright to wait until
			// the image is loaded (there's no need to offload
			// this work to another thread)
			String componentNameKey = applicationListItem.getComponentName().getPackageName() + "." +
					applicationListItem.getComponentName().getClassName();
			Bitmap appIconBitmap = getAppBitmapFromCache(componentNameKey);
			
			if (appIconBitmap == null) {
				// Get the bitmap and put it in the cache.
				Bitmap iconBitmap = null;
				
				try {
					Drawable iconDrawable = packageManager().getActivityIcon(applicationListItem.getComponentName());
					iconDrawable.setBounds(0,  0, iconDrawable.getIntrinsicWidth(),
							iconDrawable.getIntrinsicHeight());
					
					// A drawable was found. We now need to get a (scaled) bitmap out of this
					iconBitmap = Bitmap.createBitmap(iconDrawable.getIntrinsicWidth(),
							iconDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
					Canvas fullSizeBitmapCanvas = new Canvas(iconBitmap);
					fullSizeBitmapCanvas.setBitmap(iconBitmap);
					fullSizeBitmapCanvas.save();
					iconDrawable.draw(fullSizeBitmapCanvas);
					fullSizeBitmapCanvas.restore();
					fullSizeBitmapCanvas.setBitmap(null);					
				} catch (NameNotFoundException e) {
					// TODO:
					// Set iconBitmap to a generic placeholder
					Log.e(TAG, "No icon found for " + componentNameKey);
				}
				
				// At this point, iconBitmap has the full size app icon. We need to
				// scale this to only be as large as the ImageView into which the icon
				// is supposed to go
				if (iconBitmap != null) {
					// Determine the width and height of the scaled image while keeping in mind
					// we need to maintain the aspect ratio
					double widthScaleRatio = ((double)mContext.getResources().getDimensionPixelSize(R.dimen.app_icon_size))/iconBitmap.getWidth();
					double heightScaleRatio = ((double)mContext.getResources().getDimensionPixelSize(R.dimen.app_icon_size))/iconBitmap.getHeight();
					double minScaleRatio = Math.min(widthScaleRatio, heightScaleRatio);
					int scaledWidth, scaledHeight;
					
					scaledWidth = (int)(iconBitmap.getWidth() * minScaleRatio);
					scaledHeight = (int)(iconBitmap.getHeight() * minScaleRatio);
					appIconBitmap = iconBitmap;
				}
				
				addAppBitmapToCache(componentNameKey, appIconBitmap);
			}
			
			// Set the app icon ImageView to appIconBitmap
			letterViewLayout.setImageViewBitmap(R.id.app_icon, appIconBitmap);
		}
		
		return letterViewLayout;
	}

	@Override
	public int getViewTypeCount() {
		return 4;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public void onCreate() {		
	}
	
	private void dumpAppSearchedList() {
		Log.i(TAG, "Searched app list:");
		
		for (ApplicationListItem appListItem : mSearchedAppListItems) {
			Log.i(TAG, "application: " + appListItem.getApplicationLabel() + "; launchTime: " +
					appListItem.getLaunchTime());
		}
	}
	
	@Override
	public void onDataSetChanged() {
		Log.i(TAG, "Dataset changed.");
		
		if (mKeyInputHandler == null) {
			// Get the service object.
			Intent intent = new Intent(mContext, KeyInputHandler.class);
			mContext.bindService(intent, mInputHandlerConnection, Context.BIND_AUTO_CREATE);
		}
		
		if (mAppSearchService == null) {
			// Indicate to the widget provider that the initial app list
			// loading process has just been started
			Intent intent = new Intent(mContext, AppSearchService.class);
			mContext.bindService(intent, mAppSearchServiceConnection,
					Context.BIND_AUTO_CREATE);
		}
		
		// Poll (hack warning) until the KeyHandler and AppSearch services
		// become available.
		try {
			while (mKeyInputHandler == null || mAppSearchService == null) {
				Thread.sleep(100);
				
				if (mKeyInputHandler == null) {
					Log.i(TAG, "Polling for input handler service");
				}
				
				if (mAppSearchService == null) {
					Log.i(TAG, "Polling for app search service.");
				}
			}
		} catch (InterruptedException interruptedException) {
			// Ignore
		}
		
		// At this time, both, the key input handler and the app search
		// services are ready to process requests.
		mSearchString = mKeyInputHandler.getLatestInputString();
		
		if (mSearchString.length() > 0) {
			synchronized (mAppListSynch) {
				mSearchedAppListItems = mAppSearchService.appListForSearchTerm(mSearchString);
				Collections.sort(mSearchedAppListItems, new AppLaunchTimeComparator());
				dumpAppSearchedList();
				mHighlightedSearchResults.clear();
				
				// Go through the results and highlight the entered text
				for (ApplicationListItem appListItem : mSearchedAppListItems) {
					StringBuffer applicationLabel = new StringBuffer(appListItem.getApplicationLabel());
					MatchPair matchPair = findSubsequence(applicationLabel.toString(), mSearchString);
					
					if (matchPair.to != -1 && matchPair.from != -1) {
						applicationLabel.insert(matchPair.to + 1, "</font>");
						applicationLabel.insert(matchPair.from, "<font color=\"#33b5e5\">");
					}
					
					mHighlightedSearchResults.add(applicationLabel.toString());
				}				
			}
		} else {
			synchronized (mAppListSynch) {
				mSearchedAppListItems.clear();
			}
		}
	}

	@Override
	public void onDestroy() {
	}
	
	private class MatchPair {
		public int from = -1;
		public int to = -1;
		
		public String toString() {
			return from + " - " + to;
		}
	}
	
	private boolean isAlphaNumeric(char ch) {
		return (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
	}
	
	private MatchPair findSubsequence(String mainStr, String sequence) {
		MatchPair matchPair = new MatchPair();
		char mainChars[] = mainStr.toLowerCase().toCharArray();
		char sequenceChars[] = sequence.toLowerCase().toCharArray();
		int sequenceOffset = 0, mainStrOffset = 0;
		boolean searchComplete = false;
		int possibleStartOffset = 0;		
		
		if (sequence.length() == 0) {
			matchPair.from = matchPair.to = 0;
			
			return matchPair;
		}
		
		while (possibleStartOffset < mainChars.length && !searchComplete) {
			mainStrOffset = possibleStartOffset;
			matchPair.from = possibleStartOffset;
			
			// From this point, try to search for sequence
			while (mainStrOffset < mainChars.length && !searchComplete) {
				if (sequenceChars[sequenceOffset] == mainChars[mainStrOffset]) {
					sequenceOffset++;
					mainStrOffset++;
					
					if (sequenceOffset >= sequenceChars.length) {
						searchComplete = true;
						matchPair.to = mainStrOffset - 1;
						
						break;
					}
				} else if (!isAlphaNumeric(mainChars[mainStrOffset])) {
					if (sequenceOffset == 0) {
						possibleStartOffset++;
						matchPair.from = possibleStartOffset;
					}
					
					mainStrOffset++;
				} else {
					break;
				}
			}
			
			sequenceOffset = 0;
			possibleStartOffset++;
		}
		
		return matchPair;
	}
}
