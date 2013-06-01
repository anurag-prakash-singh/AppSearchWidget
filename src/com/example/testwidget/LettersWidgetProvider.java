package com.example.testwidget;

import java.util.HashMap;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
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

public class LettersWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "LettersWidgetProvider";
	private static final float TEXT_SCALE_FACTOR = 0.80F;
	private static final float MODE_KEY_SCALE_FACTOR = 0.60F;
	private Character mKeyCharacter = new Character('w');
	private Canvas mOffscreenKeyCanvas = new Canvas();
	private Bitmap mKeyCharBitmap;
	private Bitmap mModeKeyBitmap;
	private Bitmap mDeleteKeyBitmap;
	private Paint mCharPaint, mRectPaint;
	private HashMap<Integer, Bitmap> mKeyCharBitmapTable =
			new HashMap<Integer, Bitmap>();
	private int mCurrentKeyboardMode = KeyboardMode.NORMAL_MODE;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
	}
	
	private float convertDpToPx(Context context, float dp) {
		DisplayMetrics displayMetrics = new DisplayMetrics();
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(displayMetrics);
		
		Log.i(TAG, "dp: " + dp + "; dpi: " + displayMetrics.densityDpi);
		
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
			
//			View keyButtonView = lettersViewLayout.
			float keyWidthPx = context.getResources().getDimension(R.dimen.key_char_width);
			float keyHeightPx = context.getResources().getDimension(R.dimen.key_char_height);
			
			Log.i(TAG, "widthpx = " + keyWidthPx);
			Log.i(TAG, "heightpx = " + keyHeightPx);
			
			mCharPaint.setTextSize(Math.min(keyWidthPx * TEXT_SCALE_FACTOR,
					keyHeightPx * TEXT_SCALE_FACTOR));
			
			Rect bounds = new Rect();
			mCharPaint.getTextBounds(mKeyCharacter + "", 0, 1, bounds);
			mKeyCharBitmap = Bitmap.createBitmap(bounds.width() + 2,
					Math.abs(bounds.bottom - bounds.top) + 2, Config.ARGB_4444);
			
			Log.i(TAG, "bounds for " + mKeyCharacter + " : " + bounds.toShortString());
			Log.i(TAG, "keychar height: " + Math.abs(bounds.bottom - bounds.top));
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
		
		Log.i(TAG, "widthpx = " + keyWidthPx);
		Log.i(TAG, "heightpx = " + keyHeightPx);
		
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
		
		Log.i(TAG, "Text drawn for " + character + " bounds: " + bounds.toString());
		
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
				Log.i(TAG, "calling getBitmapForCharacter " + "for " +
						ResourceAlphabetMapEnglish.getLetterForId(resourceId).charAt(0));
				
				// TODO: Check the keyboard mode here.
				mKeyCharBitmapTable.put(resourceId,
						getBitmapForCharacter(context,
								ResourceAlphabetMapEnglish.getLetterForId(resourceId).charAt(0)));
			} else {
				// Do nothing.
			}
		}
	}
	
	/*
	 * Set the bitmap for the mode key (normal/shift mode, for example).
	 */
	private void setModeKeyBitmap(Context context, RemoteViews lettersViewLayout) {
		// TODO: Set the mode key bitmap.
		// Get the bitmap for the mode key.
		if (mModeKeyBitmap == null) {
			// Decide on the size of the bitmap to draw.
			Drawable rawDrawable = context.getResources().getDrawable(R.drawable.sym_keyboard_shift_holo);
			
			if (rawDrawable == null) {
				Log.e(TAG, "No image found for the shift button.");
				
				return;
			}
			
			Bitmap rawBitmap = ((BitmapDrawable)rawDrawable).getBitmap();
					/*BitmapFactory.decodeResource(context.getResources(), R.drawable.sym_bkeyboard_shift);*/
			
			if (rawBitmap == null) {
				Log.e(TAG, "No image found for the shift button.");
				
				return;
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
			
			mModeKeyBitmap = Bitmap.createScaledBitmap(rawBitmap, scaledBitmapWidth, scaledBitmapHeight,
					false);
		}
		
		// Set the bitmap for the mode key.
		lettersViewLayout.setBitmap(R.id.shift_button, "setImageBitmap",
				mModeKeyBitmap);
	}
	
	private void setDeleteKeyBitmap(Context context) {
		
	}
	
	private void setBitmapsForKeys(RemoteViews lettersViewLayout, Context context) {
		initializeKeyCharBitmaps(context);
		
		// The bitmaps have been initialized. Assign them to the
		// relevant characters.
		List<Integer> resourceIds = ResourceAlphabetMapEnglish.getResourceIds();
		
		for (Integer resourceId : resourceIds) {
			if (mKeyCharBitmapTable.containsKey(resourceId)) {
				Log.i(TAG, "Setting bitmap for resource: " + resourceId);
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
		
		Log.i(TAG, "Remote view package name: " + lettersViewLayout.getPackage());
		
		// Set the collection view service for the dictionary collection view.
		try {
			Intent intent = new Intent(context, LettersRemoteViewsService.class);
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
			Log.i(TAG, "Attaching pendingintent for key " +
					ResourceAlphabetMapEnglish.getLetterForId(resourceId));
			
			Intent alphabetKeyPressedIntent = new LetterIntent(context, AppSearchService.class);
			alphabetKeyPressedIntent.setAction(AppSearchService.KEY_PRESSED_ACTION);
			alphabetKeyPressedIntent.putExtra(AppSearchService.KEY_CHARACTER_EXTRA,
					ResourceAlphabetMapEnglish.getLetterForId(resourceId));
			
			PendingIntent alphabetKeyPressedPendingIntent =
					PendingIntent.getService(context, resourceId, alphabetKeyPressedIntent, 0);
			lettersViewLayout.setOnClickPendingIntent(resourceId,
					alphabetKeyPressedPendingIntent);
		}
		
		// Special keys.
		// 'Delete'
		Intent deleteKeyPressedIntent = new Intent(context, AppSearchService.class);
		deleteKeyPressedIntent.setAction(AppSearchService.KEY_PRESSED_ACTION);
		deleteKeyPressedIntent.putExtra(AppSearchService.KEY_CHARACTER_EXTRA,
					context.getString(R.string.delete_key_identifier));
		PendingIntent deleteKeyPressedPendingIntent =
				PendingIntent.getService(context, R.id.delete_button,
						deleteKeyPressedIntent, 0);
		lettersViewLayout.setOnClickPendingIntent(R.id.delete_button,
				deleteKeyPressedPendingIntent);
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		RemoteViews lettersViewLayout = new RemoteViews(context.getPackageName(),
				R.layout.dictionary_layout);
		 
		// Set the collection view service for the dictionary collection view.
		try {
			Intent intent = new Intent(context, LettersRemoteViewsService.class);
			lettersViewLayout.setRemoteAdapter(R.id.widgetLetterView, intent);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		
		setBitmapsForKeys(lettersViewLayout, context);
		setModeKeyBitmap(context, lettersViewLayout);
		attachPendingIntentsToKeys(context, lettersViewLayout);
		
		Log.i(TAG, "In onUpdate");
		
		appWidgetManager.updateAppWidget(appWidgetIds, lettersViewLayout);
		
		lettersViewLayout.apply(context, null);
		
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}	
}
