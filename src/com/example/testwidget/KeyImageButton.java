package com.example.testwidget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageButton;

public class KeyImageButton extends ImageButton {
	private static final String TAG = "KeyImageButton";
	private static final double TEXT_SCALE_FACT = 0.8;
	private Character mKeyCharacter = new Character('j');
	private Canvas mOffscreenKeyCanvas = new Canvas();
	private Bitmap mKeyCharBitmap;
	private Paint mCharPaint, mRectPaint;
	
	public KeyImageButton(Context context) {
		super(context);
		initCharPaint();
	}

	public KeyImageButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		extractKeyCharacterFromResources(context, attrs);
		initCharPaint();
	}

	public KeyImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		extractKeyCharacterFromResources(context, attrs);
		initCharPaint();
	}
	
	private void extractKeyCharacterFromResources(Context context, AttributeSet attrs) {
//		TypedArray a = context.obtainStyledAttributes(attrs,
//				R.styleable.com_example_keysimulator_KeyImageButton);
//		
//		String keyCharAttr = a.getString(R.styleable.com_example_keysimulator_KeyImageButton_key_character);
//		
//		if (keyCharAttr != null) {
//			mKeyCharacter = keyCharAttr.charAt(0);
//		}
//		
//		a.recycle();
	}
	
	public Character getKeyCharacter() {
		return mKeyCharacter;
	}
	
	public void setKeyCharacter(Character keyCharacter) {
		mKeyCharacter = Character.valueOf(keyCharacter.charValue());
	}
	
	private void initCharPaint() {
		mCharPaint = new Paint();
		mCharPaint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);
		mCharPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		
		mRectPaint = new Paint();
		mRectPaint.setARGB(0xFF, 0xFF, 0, 0);
		mRectPaint.setStyle(Paint.Style.STROKE);
		mRectPaint.setStrokeWidth(1);
	}
	
	private void allocateCharBitmapIfNeeded() {
		if (mKeyCharBitmap == null) {
			mCharPaint.setTextSize((float) Math.min(getWidth() * TEXT_SCALE_FACT,
					getHeight() * TEXT_SCALE_FACT));
			
			Rect bounds = new Rect();
			mCharPaint.getTextBounds(mKeyCharacter + "", 0, 1, bounds);
			mKeyCharBitmap = Bitmap.createBitmap(bounds.width() + 2,
					Math.abs(bounds.bottom - bounds.top) + 2, Config.ARGB_8888);
			
			Log.i(TAG, "bounds for " + mKeyCharacter + " : " + bounds.toShortString());
			Log.i(TAG, "keychar height: " + Math.abs(bounds.bottom - bounds.top));
		}
	}
	
	/*
	 * Draw 'character' into the offscreen canvas.
	 */
	private void drawCharInBitmap(Character character) {
		allocateCharBitmapIfNeeded();
		mOffscreenKeyCanvas.setBitmap(mKeyCharBitmap);
		
		Rect bounds = new Rect();
		mCharPaint.getTextBounds(mKeyCharacter + "", 0, 1, bounds);
		
		mOffscreenKeyCanvas.drawText(character + "", -bounds.left, -bounds.top, mCharPaint);		
		mOffscreenKeyCanvas.drawRect(new Rect(0,0,
				Math.abs(bounds.right - bounds.left),
				Math.abs(bounds.bottom - bounds.top)), mRectPaint);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		// Draw the key character in the box
		drawCharInBitmap(mKeyCharacter);
		
		// Figure out where to position the character bitmap.
		float displacementX = getWidth()/2 - mKeyCharBitmap.getWidth()/2;
		float displacementY = getHeight()/2 - mKeyCharBitmap.getHeight()/2;
		canvas.drawBitmap(mKeyCharBitmap, displacementX, displacementY, null);
	}
}
