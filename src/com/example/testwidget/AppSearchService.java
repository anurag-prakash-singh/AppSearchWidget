package com.example.testwidget;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AppSearchService extends Service {
	private static String TAG = "Kuikk/AppSearchService";
	
	// Used to synchronize access to the list of applications/activities
	private Object mAppListSynch = new Object();
	private ArrayList<String> mInstalledPackageNames = new ArrayList<String>();
	private ArrayList<ApplicationListItem> mApplicationListItems = null;
	private AppSearchServiceBinder mAppSearchServiceBinder =
			new AppSearchServiceBinder(); 
	private boolean mInitialAppListAvailable = false;
	private Semaphore mAppListAvailability = null;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Starting service");
		
		if (mApplicationListItems == null) {
			mAppListAvailability = new Semaphore(1);
			mAppListAvailability.acquireUninterruptibly();
			new AppListUpdater().execute(new Void[] {});
		}
		
		return START_STICKY;
	}
	
	public ArrayList<ApplicationListItem> appListForSearchTerm(String searchComponentName) {
		ArrayList<ApplicationListItem> foundAppListItems =
				new ArrayList<ApplicationListItem>();
		
		synchronized (mApplicationListItems) {
			// Sometimes, the clients of this service can race with it for
			// the apps list. This will happen when the list is queried (
			// such as through this method) before the AppListUpdater has
			// had a chance to populate it. Here, we check if the list is
			// available, and if not, we wait for a signal (from AppListUpdater)
			// that it is before proceeding with the search. 
			if (!mInitialAppListAvailable) {
				mAppListAvailability.acquireUninterruptibly();
			}
			
			Log.i(TAG, "Searching for '" + searchComponentName + "'");
			
			for (ApplicationListItem appListItem : mApplicationListItems) {
				if (appListItem.getSimplifiedApplicationLabel().indexOf(searchComponentName) != -1) {
					foundAppListItems.add(appListItem);
					
					Log.i(TAG, "Found " + appListItem.getSimplifiedApplicationLabel());
				}
			}
		}
		
		return foundAppListItems;
	}
	
	private class AppListUpdater extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... args) {
			synchronized (mAppListSynch) {
				// Build a list of launchable activities
				PackageManager packageManager = getPackageManager();
				Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
				mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
				mApplicationListItems = new ArrayList<ApplicationListItem>();
				mInstalledPackageNames.clear();

				List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0);

				for (ResolveInfo resolveInfo : resolveInfoList) {
					if (resolveInfo.activityInfo != null && resolveInfo.activityInfo.name != null) {
						String label = null;
						
						// Let's get the label for this activity.
						if (label == null) {
							label = resolveInfo.loadLabel(packageManager).toString();
						}

						if (label == null) {
							label = (String) packageManager.getApplicationLabel(resolveInfo.activityInfo.applicationInfo);
						}
						
						if (label == null) {
							label = "";
						}
						
						mInstalledPackageNames.add(label + " : " + resolveInfo.activityInfo.packageName);

						// According to the documentation, ComponentName (a 'component' may be an activity, a BroadcastReceiver,
						// a service etc.) needs two pieces of information -- the package name (resolveInfo.activityInfo.packageName)
						// and the name of the class within the package (resolveInfo.activityInfo.name).
						ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName,
								resolveInfo.activityInfo.name);
						mApplicationListItems.add(new ApplicationListItem(resolveInfo.loadIcon(packageManager), null,
								label , componentName));
					}
				}
				
				mAppListAvailability.release();
				mInitialAppListAvailable = true;
			}
			
			return null;
		}		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mAppSearchServiceBinder;
	}
	
	public class AppSearchServiceBinder extends Binder {
		AppSearchService getService() {
			return AppSearchService.this; 
		}
	}
}
