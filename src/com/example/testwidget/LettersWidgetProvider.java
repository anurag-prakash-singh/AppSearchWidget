package com.example.testwidget;

import java.util.HashMap;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

public class LettersWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "Kuikk/LettersWidgetProvider";
	private static final float TEXT_SCALE_FACTOR = 0.80F;
	private static final float MODE_KEY_SCALE_FACTOR = 0.60F;
	private Character mKeyCharacter = new Character('w');
	private Canvas mOffscreenKeyCanvas = new Canvas();
	private Bitmap mKeyCharBitmap;
	private Bitmap mModeKeyBitmap;
	private Bitmap mDeleteKeyBitmap;
	private Bitmap mUnlockedShiftKeyBitmap;
	private Bitmap mLockedShiftKeyBitmap;
	private Bitmap mUnlockedDeleteKeyBitmap;
	private Bitmap mLockedDeleteKeyBitmap;
	private Paint mCharPaint, mRectPaint;
	private HashMap<Integer, Bitmap> mKeyCharBitmapTable =
			new HashMap<Integer, Bitmap>();
	private int mCurrentKeyboardMode = KeyboardMode.NORMAL_MODE;
	private boolean mShiftMode = false;
	private String mEnteredText = "";
	private AppWidgetManager mWidgetManager;
	private boolean mDebug = true;
	private boolean mSpew = false;
	
	private AppWidgetManager getAppWidgetManager(Context context) {
		if (mWidgetManager == null) {
			mWidgetManager = AppWidgetManager.getInstance(context);
		}
		
		return mWidgetManager;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		// Process the intent.
		Log.i(TAG, "Received intent:" + intent.getAction());
		
		if (intent.getAction().equals(Constants.TEXT_INTENT)) {
			// Toggle shift mode.
			mShiftMode = intent.getBooleanExtra(Constants.CURRENT_SHIFT_MODE, false);
			mEnteredText = intent.getStringExtra(Constants.TYPED_TEXT);
			
			getAppWidgetManager(context).updateAppWidget(
					new ComponentName(context, LettersWidgetProvider.class),
					buildUpdate(context));			
		} else if (intent.getAction().equals(Constants.COMPONENT_LAUNCH_INTENT)) {
			Intent launchIntent = new Intent(Intent.ACTION_MAIN, null);
			launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			ComponentName launchComponent = new ComponentName(intent.getStringExtra(Constants.COMPONENT_PACKAGE),
					intent.getStringExtra(Constants.COMPONENT_CLASS));
			launchIntent.setComponent(launchComponent);
			
			Log.i(TAG, launchComponent.getPackageName() + "/" + launchComponent.getClassName());
			
			try {
				context.startActivity(launchIntent);
				
				// App launch successful. Generate an 'erase-all' key event.
				Intent eraseAllIntent = new LetterIntent(context, KeyInputHandler.class);
				eraseAllIntent.setAction(KeyInputHandler.KEY_PRESSED_ACTION);
				eraseAllIntent.putExtra(KeyInputHandler.KEY_CHARACTER_EXTRA,
						KeyInputHandler.ERASE_ALL_KEY);
				
				context.startService(eraseAllIntent);
				
				// Let the AppSearchService know so that the launch count
				// can be incremented
				Intent updateLaunchCountIntent = new Intent(context, AppSearchService.class);
				updateLaunchCountIntent.setAction(AppSearchService.ACTION_APP_LAUNCHED);
				updateLaunchCountIntent.putExtra(AppSearchService.PACKAGE_NAME_EXTRA, launchComponent.getPackageName());
				updateLaunchCountIntent.putExtra(AppSearchService.CLASS_NAME_EXTRA, launchComponent.getClassName());
				context.startService(updateLaunchCountIntent);				
			} catch (Exception exception) {
				exception.printStackTrace();
				Log.e(TAG, exception.getMessage());
				Toast.makeText(context, "Failed to launch application.",
						Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private float convertDpToPx(Context context, float dp) {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(displayMetrics);
		
		if (mSpew) {
			Log.i(TAG, "dp: " + dp + "; dpi: " + displayMetrics.densityDpi);
		}
		
		return dp * displayMetrics.densityDpi/160F;
	}
	
	private void allocateCharBitmapIfNeeded(Context context) {
		if (mKeyCharBitmap == null || mCharPaint == null) {
			if (mCharPaint == null) {
				mCharPaint = new Paint();
				mCharPaint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);
				mCharPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
			}
			
			RemoteViews lettersViewLayout = new RemoteViews(context.getPackageName(),
					R.layout.dictionary_layout);
			
			float keyWidthPx = context.getResources().getDimension(R.dimen.key_char_width);
			float keyHeightPx = context.getResources().getDimension(R.dimen.key_char_height);
			
			if (mSpew) {
				Log.i(TAG, "widthpx = " + keyWidthPx);
				Log.i(TAG, "heightpx = " + keyHeightPx);
			}
			
			mCharPaint.setTextSize(Math.min(keyWidthPx * TEXT_SCALE_FACTOR,
					keyHeightPx * TEXT_SCALE_FACTOR));
			
			Rect bounds = new Rect();
			mCharPaint.getTextBounds(mKeyCharacter + "", 0, 1, bounds);
			mKeyCharBitmap = Bitmap.createBitmap(bounds.width() + 2,
					Math.abs(bounds.bottom - bounds.top) + 2, Config.ARGB_4444);
			
			if (mSpew) {
				Log.i(TAG, "bounds for " + mKeyCharacter + " : " + bounds.toShortString());
				Log.i(TAG, "keychar height: " + Math.abs(bounds.bottom - bounds.top));
			}
		}
	}
	
	/*
	 * Draw 'character' into the offscreen canvas.
	 */
	private void drawCharInBitmap(Context context, Character character) {
		allocateCharBitmapIfNeeded(context);
		mOffscreenKeyCanvas.setBitmap(mKeyCharBitmap);
		
		Rect bounds = new Rect();
		mCharPaint.getTextBounds(mKeyCharacter + "", 0, 1, bounds);
		
		mOffscreenKeyCanvas.drawText(character + "", -bounds.left, -bounds.top, mCharPaint);		
//		mOffscreenKeyCanvas.drawRect(new Rect(0,0,
//				Math.abs(bounds.right - bounds.left),
//				Math.abs(bounds.bottom - bounds.top)), mRectPaint);
	}
	
	private Bitmap getBitmapForCharacter(Context context, Character character) {
		Bitmap characterBitmap = null;
		
		if (mCharPaint == null) {
			mCharPaint = new Paint();
			mCharPaint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);
			mCharPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		}
		
		float keyWidthPx = context.getResources().getDimension(R.dimen.key_char_width);
		float keyHeightPx = context.getResources().getDimension(R.dimen.key_char_height);
		
		if (mSpew) {
			Log.i(TAG, "widthpx = " + keyWidthPx);
			Log.i(TAG, "heightpx = " + keyHeightPx);
		}
		
		mCharPaint.setTextSize(Math.min(keyWidthPx * TEXT_SCALE_FACTOR,
				keyHeightPx * TEXT_SCALE_FACTOR));
		
		// Create the bitmap.
		Rect bounds = new Rect();
		mCharPaint.getTextBounds(character + "", 0, 1, bounds);
//		characterBitmap = Bitmap.createBitmap(bounds.width() + 2,
//				Math.abs(bounds.bottom - bounds.top) + 2, Config.ARGB_8888);
		
		characterBitmap = Bitmap.createBitmap((int)keyWidthPx, (int)keyHeightPx, Config.ARGB_4444);
		
		// Draw the character on the bitmap.
		mOffscreenKeyCanvas.setBitmap(characterBitmap);
		
//		mOffscreenKeyCanvas.drawText(character + "", -bounds.left, -bounds.top, mCharPaint);
		mOffscreenKeyCanvas.drawText(character + "",
				(keyWidthPx - bounds.width())/2 - bounds.left,
				(keyHeightPx)/2 + context.getResources().getDimension(R.dimen.key_char_vertical_offset), mCharPaint);
		
		if (mSpew) {
			Log.i(TAG, "Text drawn for " + character + " bounds: " + bounds.toString());
		}
		
		return characterBitmap;
	}
	
	/*
	 * Populates the keychar bitmap table.
	 */
	private void initializeKeyCharBitmaps(Context context) {
		// Get all the resource IDs and build bitmaps for them.
		List<Integer> resourceIds = ResourceAlphabetMapEnglish.getResourceIds();
		
		for (Integer resourceId : resourceIds) {
			if (!mKeyCharBitmapTable.containsKey(resourceId)) {
				// This resource doesn't have a corresponding bitmap.
				// Create it.
				int keycharIndex = 0;
				
				if (ResourceAlphabetMapEnglish.getLetterForId(resourceId).length() > 1) {
					if (mShiftMode) {
						keycharIndex = 1;
					}
				}
				
				if (mSpew) {
					Log.i(TAG, "calling getBitmapForCharacter " + "for " +
							ResourceAlphabetMapEnglish.getLetterForId(resourceId).charAt(keycharIndex));
				}
				
				mKeyCharBitmapTable.put(resourceId,
						getBitmapForCharacter(context,
								ResourceAlphabetMapEnglish.getLetterForId(resourceId).charAt(keycharIndex)));
			} else {
				// Do nothing.
			}
		}
	}
	
	private Bitmap getDeleteKeyBitmap(Context context, boolean shiftMode) {
		Drawable rawDrawable = null;
		
		if (shiftMode == false) {
			rawDrawable = context.getResources().getDrawable(R.drawable.sym_keyboard_delete);
		} else {
			rawDrawable = context.getResources().getDrawable(R.drawable.sym_keyboard_delete_locked);
		}
		
		Bitmap rawBitmap = ((BitmapDrawable)rawDrawable).getBitmap();
		
		if (rawBitmap == null) {
			Log.e(TAG, "No image found for the delete button.");
			
			return null;
		}
		
		int rawBitmapWidth = rawBitmap.getWidth();
		int rawBitmapHeight = rawBitmap.getHeight();
		int scaledBitmapWidth, scaledBitmapHeight;
		
		if (mSpew) {
			Log.i(TAG, "Delete key raw bitmap size: " + rawBitmapHeight + " x " + rawBitmapHeight);
		}
		
		if (rawBitmapWidth < rawBitmapHeight) {
			scaledBitmapHeight = (int) (MODE_KEY_SCALE_FACTOR * context.getResources().getDimension(R.dimen.delete_char_height));
			scaledBitmapWidth = (int)(scaledBitmapHeight*(rawBitmapWidth/rawBitmapHeight));
		} else {
			scaledBitmapWidth = (int) (MODE_KEY_SCALE_FACTOR * context.getResources().getDimension(R.dimen.delete_char_width));
			scaledBitmapHeight = (int)(scaledBitmapWidth/(rawBitmapWidth/rawBitmapHeight));
		}
		
		return Bitmap.createScaledBitmap(rawBitmap, scaledBitmapWidth, scaledBitmapHeight,
				true);
	}
	
	/*
	 * Set the bitmap for the mode key (normal/shift mode, for example).
	 */
	private void setDeleteKeyBitmap(Context context, RemoteViews lettersViewLayout) {
		// Get the bitmap for the mode key.
		if (mLockedDeleteKeyBitmap == null) {
			mLockedDeleteKeyBitmap = getDeleteKeyBitmap(context, true);
		}
		
		if (mUnlockedDeleteKeyBitmap == null) {
			mUnlockedDeleteKeyBitmap = getDeleteKeyBitmap(context, false);
		}
		
		if (mSpew) {
			Log.i(TAG, "shiftmode: " + mShiftMode);
			Log.i(TAG, "Setting bitmap for delete key");
		}
		
		// Set the bitmap for the mode key.
		lettersViewLayout.setBitmap(R.id.delete_button, "setImageBitmap",
				mShiftMode ? mLockedDeleteKeyBitmap : mUnlockedDeleteKeyBitmap);
	}
	
	private Bitmap getModeKeyBitmap(Context context, boolean shiftMode) {
		Drawable rawDrawable = null;
		
		if (shiftMode == false) {
			rawDrawable = context.getResources().getDrawable(R.drawable.sym_keyboard_shift_holo);
		} else {
			rawDrawable = context.getResources().getDrawable(R.drawable.sym_keyboard_shift_locked_holo);
		}
		
		Bitmap rawBitmap = ((BitmapDrawable)rawDrawable).getBitmap();
		
		if (rawBitmap == null) {
			Log.e(TAG, "No image found for the shift button.");
			
			return null;
		}
		
		int rawBitmapWidth = rawBitmap.getWidth();
		int rawBitmapHeight = rawBitmap.getHeight();
		int scaledBitmapWidth, scaledBitmapHeight;
		
		if (rawBitmapWidth < rawBitmapHeight) {
			scaledBitmapHeight = (int) (MODE_KEY_SCALE_FACTOR * context.getResources().getDimension(R.dimen.mode_key_height));
			scaledBitmapWidth = (int)(scaledBitmapHeight*(rawBitmapWidth/rawBitmapHeight));
		} else {
			scaledBitmapWidth = (int) (MODE_KEY_SCALE_FACTOR * context.getResources().getDimension(R.dimen.mode_key_width));
			scaledBitmapHeight = (int)(scaledBitmapWidth/(rawBitmapWidth/rawBitmapHeight));
		}
		
		return Bitmap.createScaledBitmap(rawBitmap, scaledBitmapWidth, scaledBitmapHeight,
				true);
	}
	
	/*
	 * Set the bitmap for the mode key (normal/shift mode, for example).
	 */
	private void setModeKeyBitmap(Context context, RemoteViews lettersViewLayout) {
		// Get the bitmap for the mode key.
		if (mLockedShiftKeyBitmap == null) {
			mLockedShiftKeyBitmap = getModeKeyBitmap(context, true);
		}
		
		if (mUnlockedShiftKeyBitmap == null) {
			mUnlockedShiftKeyBitmap = getModeKeyBitmap(context, false);
		}
		
		if (mSpew) {
			Log.i(TAG, "shiftmode: " + mShiftMode);
		}
		
		// Set the bitmap for the mode key.
		lettersViewLayout.setBitmap(R.id.shift_button, "setImageBitmap",
				mShiftMode ? mLockedShiftKeyBitmap : mUnlockedShiftKeyBitmap);
	}
	
	private void setBitmapsForKeys(RemoteViews lettersViewLayout, Context context) {
		initializeKeyCharBitmaps(context);
		
		// The bitmaps have been initialized. Assign them to the
		// relevant characters.
		List<Integer> resourceIds = ResourceAlphabetMapEnglish.getResourceIds();
		
		for (Integer resourceId : resourceIds) {
			if (mKeyCharBitmapTable.containsKey(resourceId)) {
				if (mSpew) {
					Log.i(TAG, "Setting bitmap for resource: " + resourceId);
				}
				
				lettersViewLayout.setBitmap(resourceId, "setImageBitmap",
						mKeyCharBitmapTable.get(resourceId));
			}
		}
	}
	
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        // scale the fonts of the clock to fit inside the new size
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        
        RemoteViews lettersViewLayout = new RemoteViews(context.getPackageName(),
				R.layout.dictionary_layout);
		
        if (mSpew) {
        	Log.i(TAG, "Remote view package name: " + lettersViewLayout.getPackage());
        }
		
		// Set the collection view service for the dictionary collection view.
		try {
			Intent intent = new Intent(context, AppListRemoteViewsService.class);
			lettersViewLayout.setRemoteAdapter(R.id.widgetLetterView, intent);
			
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		
		setBitmapsForKeys(lettersViewLayout, context);
		widgetManager.updateAppWidget(appWidgetId, lettersViewLayout);
    }

	private void attachPendingIntentsToKeys(Context context, RemoteViews lettersViewLayout) {
		// Check the keyboard mode before setting the KEY_CHARACTER_EXTRA information.
		// Do the alphabet keys first.
		for (Integer resourceId : ResourceAlphabetMapEnglish.getResourceIds()) {
			// Create the intent, add information about the key and
			// set the pending intent for that key.
			Intent alphabetKeyPressedIntent = new LetterIntent(context, KeyInputHandler.class);
			alphabetKeyPressedIntent.setAction(KeyInputHandler.KEY_PRESSED_ACTION);
			alphabetKeyPressedIntent.putExtra(KeyInputHandler.KEY_CHARACTER_EXTRA,
					ResourceAlphabetMapEnglish.getLetterForId(resourceId));
			
			PendingIntent alphabetKeyPressedPendingIntent =
					PendingIntent.getService(context, resourceId, alphabetKeyPressedIntent, 0);
			lettersViewLayout.setOnClickPendingIntent(resourceId,
					alphabetKeyPressedPendingIntent);
		}
		
		// Special keys.
		// 'Delete'
		Intent deleteKeyPressedIntent = new Intent(context, KeyInputHandler.class);
		deleteKeyPressedIntent.setAction(KeyInputHandler.KEY_PRESSED_ACTION);
		deleteKeyPressedIntent.putExtra(KeyInputHandler.KEY_CHARACTER_EXTRA,
					context.getString(R.string.delete_key_identifier));
		PendingIntent deleteKeyPressedPendingIntent =
				PendingIntent.getService(context, R.id.delete_button,
						deleteKeyPressedIntent, 0);
		lettersViewLayout.setOnClickPendingIntent(R.id.delete_button,
				deleteKeyPressedPendingIntent);
		
		// 'Shift'
		Intent shiftKeyPressedIntent = new Intent(context, KeyInputHandler.class);
		shiftKeyPressedIntent.setAction(KeyInputHandler.KEY_PRESSED_ACTION);
		shiftKeyPressedIntent.putExtra(KeyInputHandler.KEY_CHARACTER_EXTRA,
					context.getString(R.string.shift_key_identifier));
		PendingIntent shiftKeyPressedPendingIntent =
				PendingIntent.getService(context, R.id.shift_button,
						shiftKeyPressedIntent, 0);
		lettersViewLayout.setOnClickPendingIntent(R.id.shift_button,
				shiftKeyPressedPendingIntent);
	}
	
	private void updateItemCollection(Context context) {
		AppWidgetManager appWidgetManager = getAppWidgetManager(context);
		int[] widgetIDs = appWidgetManager.getAppWidgetIds(
				new ComponentName(context, LettersWidgetProvider.class));
		
		appWidgetManager.notifyAppWidgetViewDataChanged(widgetIDs, R.id.widgetLetterView);
	}
	
	private RemoteViews buildUpdate(Context context) {
		RemoteViews lettersViewLayout = new RemoteViews(context.getPackageName(),
				R.layout.dictionary_layout);
		
		// Set the collection view service for the dictionary collection view.
		try {
			Intent intent = new Intent(context, AppListRemoteViewsService.class);
			intent.putExtra(Constants.TYPED_TEXT, mEnteredText);
			lettersViewLayout.setRemoteAdapter(R.id.widgetLetterView, intent);
		} catch(Exception exception) {
			exception.printStackTrace();
		}

		setBitmapsForKeys(lettersViewLayout, context);
		setModeKeyBitmap(context, lettersViewLayout);
		setDeleteKeyBitmap(context, lettersViewLayout);
		attachPendingIntentsToKeys(context, lettersViewLayout);
		updateItemCollection(context);
		
		lettersViewLayout.apply(context, null);
		
		Intent launchTemplateIntent = new Intent(context, LettersWidgetProvider.class);
		launchTemplateIntent.setAction(Constants.COMPONENT_LAUNCH_INTENT);
		PendingIntent launchTemplatePendingIntent = PendingIntent.getBroadcast(context, 0, launchTemplateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		lettersViewLayout.setPendingIntentTemplate(R.id.widgetLetterView, launchTemplatePendingIntent);
		
		return lettersViewLayout;
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		RemoteViews lettersViewLayout = new RemoteViews(context.getPackageName(),
				R.layout.dictionary_layout);
		 
		// Set the collection view service for the dictionary collection view.
		try {
			Intent intent = new Intent(context, AppListRemoteViewsService.class);
			intent.putExtra(Constants.TYPED_TEXT, mEnteredText);
			lettersViewLayout.setRemoteAdapter(R.id.widgetLetterView, intent);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		
		setBitmapsForKeys(lettersViewLayout, context);
		setModeKeyBitmap(context, lettersViewLayout);
		setDeleteKeyBitmap(context, lettersViewLayout);
		attachPendingIntentsToKeys(context, lettersViewLayout);
		
		Log.i(TAG, "In onUpdate");
		
		appWidgetManager.updateAppWidget(appWidgetIds, lettersViewLayout);		
		lettersViewLayout.apply(context, null);
		
		if (appWidgetIds.length > 0) {
			Intent launchTemplateIntent = new Intent(context, LettersWidgetProvider.class);
			launchTemplateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[0]);
			launchTemplateIntent.setAction(Constants.COMPONENT_LAUNCH_INTENT);
			PendingIntent launchTemplatePendingIntent = PendingIntent.getActivity(context, 0, launchTemplateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			lettersViewLayout.setPendingIntentTemplate(R.id.widgetLetterView, launchTemplatePendingIntent);
		}
		
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}	
}
