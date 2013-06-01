package com.example.testwidget;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LetterIntent extends Intent {
	private static final String TAG = "LetterIntent"; 
	
	public LetterIntent(Context packageContext, Class<?> cls) {
		super(packageContext, cls);
		Log.i(TAG, "Creating letterintent");
	}

	@Override
	public boolean filterEquals(Intent other) {
		Log.i(TAG, "Comparing probable LetterIntents");
		
		if (other instanceof LetterIntent) {
			LetterIntent otherLetterIntent = (LetterIntent)other;
			String letterIntentExtra =
					this.getStringExtra(AppSearchService.KEY_CHARACTER_EXTRA);
			String otherLetterIntentExtra =
					otherLetterIntent.getStringExtra(AppSearchService.KEY_CHARACTER_EXTRA);
			
			
			if (letterIntentExtra != null && otherLetterIntentExtra != null) {
				return letterIntentExtra.equals(otherLetterIntentExtra);
			}
		}
		
		return super.filterEquals(other);
	}

}
