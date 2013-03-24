package com.example.testwidget;

import java.util.HashMap;
import java.util.List;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;

public class LettersWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "LettersWidgetProvider";
	private static final float TEXT_SCALE_FACTOR = 0.80F; 
	private Character mKeyCharacter = new Character('w');
	private Canvas mOffscreenKeyCanvas = new Canvas();
	private Bitmap mKeyCharBitmap;
	private Paint mCharPaint, mRectPaint;
	private HashMap<Integer, Bitmap> mKeyCharBitmapTable =
			new HashMap<Integer, Bitmap>();
	
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
				
				mKeyCharBitmapTable.put(resourceId,
						getBitmapForCharacter(context,
								ResourceAlphabetMapEnglish.getLetterForId(resourceId).charAt(0)));
			} else {
				// Do nothing.
			}
		}
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
		
		Log.i(TAG, "In onUpdate");
		
		appWidgetManager.updateAppWidget(appWidgetIds, lettersViewLayout);
		lettersViewLayout.apply(context, null);
		
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}	
}
