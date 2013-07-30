package com.example.testwidget;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

/*
 * Abstracts the job of reading and writing launch counts atomically
 */
@SuppressLint("UseValueOf")
public class LaunchCountBookKeeper {
	private static final String TAG = "Kuikk/LaunchCountBookKeeper";
	private static String LAUNCH_COUNT_SAVE_FILENAME = "launchcounts.dat";
	private static final Hashtable<String, LaunchStats> mLaunchTable = new Hashtable<String, LaunchStats>();
	private static final Object mSynch = new Object();
	
	@SuppressLint("UseValueOf")
	public static void fetchLaunchStats(Context context) {
		synchronized (mSynch) {
			// Read the file
			try {
				FileInputStream fis = context.openFileInput(LAUNCH_COUNT_SAVE_FILENAME);
				BufferedReader br = new BufferedReader(new InputStreamReader(fis));
				String line;

				while ((line = br.readLine()) != null) {
					String[] lineComponents = line.split("\t");
					Log.i(TAG, "Read " + line);

					if (lineComponents.length >= 3) {
						try {
							String componentNameString = lineComponents[0];
							int usageCount = new Integer(lineComponents[1]);
							int lastLaunchTime = new Integer(lineComponents[2]);

							LaunchStats launchStats = new LaunchStats(usageCount,
									lastLaunchTime);
							mLaunchTable.put(componentNameString, launchStats);
						} catch (NumberFormatException numberFormatException) {
							Log.e(TAG, "Failed to parse fields for \"" + line + "\"");
						}
					}
				}

				br.close();
			} catch (FileNotFoundException fileNotFoundException) {
				Log.e(TAG, "Unable to find " + LAUNCH_COUNT_SAVE_FILENAME);
			} catch (IOException ioException) {
				Log.e(TAG, "Error occurred while using " + LAUNCH_COUNT_SAVE_FILENAME);
			}
		}
	}
	
	/*
	 * Write the launch statistics table to a file
	 */
	public static void saveLaunchStats(Context context) {
		synchronized (mSynch) {
			try {
				// Write to a temporary file.
				FileOutputStream fos = context.openFileOutput("temp_" + LAUNCH_COUNT_SAVE_FILENAME,
						Context.MODE_PRIVATE);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));			
				Enumeration<String> keys = mLaunchTable.keys();

				while (keys.hasMoreElements()) {
					String key = keys.nextElement();
					LaunchStats launchStats = mLaunchTable.get(key);

					if (launchStats != null) {
						StringBuffer line = new StringBuffer();
						line.append(key);
						line.append("\t");

						line.append(launchStats.getUsageCount());
						line.append("\t");

						line.append(launchStats.getLastLaunchTimeSeconds());
						line.append("\t");

						bw.write(line.toString());
						bw.newLine();
					}
				}

				bw.close();

				// Now, replace the launch count save file
				File privateFilesDir = context.getFilesDir();

				// Remove the original file if it exists
				File launchCountSaveFile = new File(privateFilesDir, LAUNCH_COUNT_SAVE_FILENAME);

				if (launchCountSaveFile.exists()) {
					if (!launchCountSaveFile.delete()) {
						Log.e(TAG, "Unable to remove " + launchCountSaveFile.getAbsolutePath());
					}
				}

				File tempLaunchCountSaveFile = new File(privateFilesDir, "temp_" + LAUNCH_COUNT_SAVE_FILENAME);

				if (!tempLaunchCountSaveFile.renameTo(launchCountSaveFile)) {
					Log.e(TAG, "Unable to rename " + tempLaunchCountSaveFile.getName() +
							" to " + launchCountSaveFile.getName());
				}

				Log.i(TAG, "Saved launch stats to " + launchCountSaveFile.getAbsolutePath());
			} catch (IOException ioException) {
				Log.e(TAG, "Error occurred while using " + LAUNCH_COUNT_SAVE_FILENAME);
			}
		}
	}
	
	/*
	 * Looks up the launch stats data for componentName, increments the
	 * usage count and sets the launch time to the current time. If the
	 * stats don't exist, creates the object 
	 */
	public static void updateLaunchStats(ComponentName componentName) {
		String packageName = componentName.getPackageName();
		String className = componentName.getClassName();
		
		synchronized (mSynch) {
			String componentKey = packageName + "/" + className;
			LaunchStats launchStats = getLaunchStats(componentName);

			if (launchStats == null) {
				launchStats = new LaunchStats(1,  System.currentTimeMillis()/1000);
				mLaunchTable.put(componentKey, launchStats);
			} else {
				launchStats.incrementUsageCount();
				launchStats.setLastLaunchTimeSeconds(System.currentTimeMillis()/1000);
			}
		}
	}
	
	public static LaunchStats getLaunchStats(ComponentName componentName) {
		String packageName = componentName.getPackageName();
		String className = componentName.getClassName();
		String componentKey = packageName + "/" + className;
		
		synchronized (mSynch) {
			if (mLaunchTable.containsKey(componentKey) == false) {
				return null;
			} else {
				return mLaunchTable.get(componentKey);
			}
		}
	}
}
