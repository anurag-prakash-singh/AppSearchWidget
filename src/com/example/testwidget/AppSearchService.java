package com.example.testwidget;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class AppSearchService extends Service {
	private static String TAG = "Kuikk/AppSearchService";
	private static int UPDATE_LAUNCH_STATS = 1;
	
	public static final String ACTION_APP_LAUNCHED = "com.example.testwidget.ACTION_APP_LAUNCHED";
	public static final String PACKAGE_NAME_EXTRA = "PACKAGE_NAME";
	public static final String CLASS_NAME_EXTRA = "CLASS_NAME";
	
	// Used to synchronize access to the list of applications/activities
	private Object mAppListSynch = new Object();
	private ArrayList<String> mInstalledPackageNames = new ArrayList<String>();
	private ArrayList<ApplicationListItem> mApplicationListItems = null;
	private AppSearchServiceBinder mAppSearchServiceBinder =
			new AppSearchServiceBinder(); 
	private boolean mInitialAppListAvailable = false;
	private Semaphore mAppListAvailability = null;
	private PackageUpdater mPackageUpdater = null;
	private long mLastLaunchTimeSeconds;
	private boolean mStatsSaverStarted = false;
	
	private Handler mLaunchStatsSaverHandler = new Handler() {
		private long mLastSavedTimeSeconds = 0;
		
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == UPDATE_LAUNCH_STATS) {
				if (mLastLaunchTimeSeconds > mLastSavedTimeSeconds) {
					LaunchCountBookKeeper.saveLaunchStats(AppSearchService.this);
					mLastSavedTimeSeconds = mLastLaunchTimeSeconds;
				}
				
				// Update every 2 minutes approximately
				this.sendMessageDelayed(this.obtainMessage(UPDATE_LAUNCH_STATS),
						120 * 1000);
			}
		}		
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Starting service");
		
		if (mApplicationListItems == null) {
			mAppListAvailability = new Semaphore(1);
			mAppListAvailability.acquireUninterruptibly();
			new AppListUpdater().execute(new Void[] {});
		}
		
		if (mPackageUpdater == null) {
			mPackageUpdater = new PackageUpdater();
			
			// Set up the intent filter
			IntentFilter packageUpdateIntentFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
			packageUpdateIntentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
			packageUpdateIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
			packageUpdateIntentFilter.addDataScheme("package");
			registerReceiver(mPackageUpdater, packageUpdateIntentFilter);
			Log.i(TAG, "Registered packageUpdater");
		}
		
		if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_APP_LAUNCHED)) {
			// Update the launch count for the launched component
			Log.i(TAG, "Updating launch count");
			String packageName = intent.getStringExtra(PACKAGE_NAME_EXTRA);
			String className = intent.getStringExtra(CLASS_NAME_EXTRA);
			
			synchronized (mAppListSynch) {
				for (ApplicationListItem appListItem : mApplicationListItems) {
					if (appListItem.getComponentName().getPackageName().equals(packageName) &&
							appListItem.getComponentName().getClassName().equals(className)) {
						Log.i(TAG, "Updated launchcount for " + packageName + "/" + className);
						appListItem.incrementLaunchCount();
						// Set the launch time
						long currentTimeSeconds = System.currentTimeMillis()/1000;
						appListItem.setLaunchTimeSeconds(currentTimeSeconds);
						mLastLaunchTimeSeconds = currentTimeSeconds;
						
						// Update the launch count table with this data
						LaunchCountBookKeeper.updateLaunchStats(appListItem.getComponentName());
						
						if (!mStatsSaverStarted) {
							// Only schedule the thread the first time round
							mLaunchStatsSaverHandler.sendMessage(mLaunchStatsSaverHandler.obtainMessage(UPDATE_LAUNCH_STATS));
							mStatsSaverStarted = true;
						}						
						
						break;
					}
				}
			}
		}
		
		return START_STICKY;
	}
	
	public ArrayList<ApplicationListItem> appListForSearchTerm(String searchComponentName) {
		ArrayList<ApplicationListItem> foundAppListItems =
				new ArrayList<ApplicationListItem>();
		
		synchronized (mAppListSynch) {
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
				
				// Retrieve the stored launch statistics
				LaunchCountBookKeeper.fetchLaunchStats(AppSearchService.this);
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
						ApplicationListItem launchableComponent = new ApplicationListItem(resolveInfo.loadIcon(packageManager), null,
								label , componentName);
						
						// If the launch stats for a component are available, set them
						LaunchStats launchStats = LaunchCountBookKeeper.getLaunchStats(componentName);						
						
						if (launchStats != null) {
							launchableComponent.setLaunchTimeSeconds(launchStats.getLastLaunchTimeSeconds());
							launchableComponent.setLaunchCount(launchStats.getUsageCount());
						}
						
						mApplicationListItems.add(launchableComponent);
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
	
	class PackageUpdater extends BroadcastReceiver {
		private void addLaunchActivitiesForPackage(String packageName) {
			synchronized (mAppListSynch) {
				Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
				mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
				mainIntent.setPackage(packageName);
				
				PackageManager packageManager = getPackageManager();
				List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0);
				boolean activitiesAdded = false;

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
						
						// According to the documentation, ComponentName (a 'component' may be an activity, a BroadcastReceiver,
						// a service etc.) needs two pieces of information -- the package name (resolveInfo.activityInfo.packageName)
						// and the name of the class within the package (resolveInfo.activityInfo.name).
						ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName,
								resolveInfo.activityInfo.name);
						mApplicationListItems.add(new ApplicationListItem(resolveInfo.loadIcon(packageManager), null,
								label , componentName));
						activitiesAdded = true;
					}
				}
				
				if (activitiesAdded) {
					Log.i(TAG, "Added launchable activities for " + packageName);
				} else {
					Log.i(TAG, "No launchable activities added for " + packageName);
				}
			}
		}
		
		private void removeActivitiesForPackage(String packageName) {
			synchronized(mAppListSynch) {
				// Iterate over mApplicationListItems and remove entries
				// belonging to packageName
				int appsListOffset = 0;
				boolean activitiesRemoved = false;
				
				while (appsListOffset < mApplicationListItems.size()) {
					ApplicationListItem applicationListItem = mApplicationListItems.get(appsListOffset);
					
					if (applicationListItem.getComponentName().getPackageName().equals(packageName)) {
						// Delete this item
						mApplicationListItems.remove(appsListOffset);
						activitiesRemoved = true;
					} else {
						appsListOffset++;
					}
				}
				
				if (activitiesRemoved) {
					Log.i(TAG, "Removed activities for " + packageName);
				} else {
					Log.i(TAG, "No activities removed (or no removable candidates found) for " + packageName);
				}
			}
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
				// Package added
				String newPackageName = intent.getData().getSchemeSpecificPart();
				addLaunchActivitiesForPackage(newPackageName);				
			} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
				// Package removed
				String packageName = intent.getData().getSchemeSpecificPart();
				removeActivitiesForPackage(packageName);
			} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_CHANGED)) {
				// Package changed
				String packageName = intent.getData().getSchemeSpecificPart();
				removeActivitiesForPackage(packageName);
				addLaunchActivitiesForPackage(packageName);
			}
		}		
	}
}
