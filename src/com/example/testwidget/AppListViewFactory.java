package com.example.testwidget;

import java.util.ArrayList;

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
import android.util.Log;
import android.util.LruCache;
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
	private Object mAppListSynch = new Object();
	// 2 MB app icon cache should be sufficient -- we may even need to 
	// reduce this further since this is a widget and we need to keep its
	// memory footprint low
	private int mAppIconCacheMaxSizeKb = 2 * 1024;
	
	private LruCache<String, Bitmap> mAppIconCache = new LruCache<String, Bitmap> (mAppIconCacheMaxSizeKb) {
		@Override
		protected int sizeOf(String key, Bitmap value) {
			// The size is measured in kilobytes.
			return value.getByteCount() / 1024;
		}
	};
	
	private void addAppBitmapToCache(String componentName, Bitmap bitmap) {
		if (getAppBitmapFromCache(componentName) == null) {
			mAppIconCache.put(componentName, bitmap);
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
			return mSearchedAppListItems.size();
		}
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

		synchronized (mAppListSynch) {
			ApplicationListItem applicationListItem = mSearchedAppListItems.get(index);
			letterViewLayout.setTextViewText(R.id.app_name,
					applicationListItem.getApplicationLabel());
			
			// TODO:
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
//					fullSizeBitmapCanvas.drawColor(R.color.app_name_bg_color);
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
					
					Log.i(TAG, "Scaling to " + scaledWidth + " x " + scaledHeight);
					
//					appIconBitmap = Bitmap.createScaledBitmap(iconBitmap, scaledWidth, scaledHeight, true);
					appIconBitmap = iconBitmap;
				}
				
				addAppBitmapToCache(componentNameKey, appIconBitmap);
			}
			
			// Set the app icon ImageView to appIconBitmap
			letterViewLayout.setImageViewBitmap(R.id.app_icon, appIconBitmap);	
//			letterViewLayout.setImageViewResource(R.id.app_icon, R.drawable.sym_bkeyboard_shift);
		}
		
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
			// Get the app search service object.
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
		String searchString = mKeyInputHandler.getLatestInputString();
		
		if (searchString.length() > 0) {
			synchronized (mAppListSynch) {
				mSearchedAppListItems = mAppSearchService.appListForSearchTerm(searchString);
				
				// TODO:
				// We're also tracking the number of times an application was
				// selected to run. This means an application that has been selected
				// more often should appear closer to the top. Arrange the list based
				// on this information.
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
}
