package com.example.testwidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ResourceAlphabetMapEnglish {
	private static HashMap<Integer, String> mIdLetterMap =
			new HashMap<Integer, String>();
	private static List<Integer> mResourceIds;
	
	static {
		mIdLetterMap.put(R.id.q_button, "q");
		mIdLetterMap.put(R.id.w_button, "w");
		mIdLetterMap.put(R.id.e_button, "e");
		mIdLetterMap.put(R.id.r_button, "r");
		mIdLetterMap.put(R.id.t_button, "t");
		mIdLetterMap.put(R.id.y_button, "y");
		mIdLetterMap.put(R.id.u_button, "u");
		mIdLetterMap.put(R.id.i_button, "i");
		mIdLetterMap.put(R.id.o_button, "o");
		mIdLetterMap.put(R.id.p_button, "p");
		
		mIdLetterMap.put(R.id.a_button, "a");
		mIdLetterMap.put(R.id.s_button, "s");
		mIdLetterMap.put(R.id.d_button, "d");
		mIdLetterMap.put(R.id.f_button, "f");
		mIdLetterMap.put(R.id.g_button, "g");
		mIdLetterMap.put(R.id.h_button, "h");
		mIdLetterMap.put(R.id.j_button, "j");
		mIdLetterMap.put(R.id.k_button, "k");
		mIdLetterMap.put(R.id.l_button, "l");
		
		mIdLetterMap.put(R.id.z_button, "z");
		mIdLetterMap.put(R.id.x_button, "x");
		mIdLetterMap.put(R.id.c_button, "c");
		mIdLetterMap.put(R.id.v_button, "v");
		mIdLetterMap.put(R.id.b_button, "b");
		mIdLetterMap.put(R.id.n_button, "n");
		mIdLetterMap.put(R.id.m_button, "m");
	}
	
	public static String getLetterForId(int id) {
		if (mIdLetterMap.containsKey(id)) {
			return mIdLetterMap.get(id);
		}
		
		return null;
	}
	
	public static List<Integer> getResourceIds() {
		if (mResourceIds == null) {
			mResourceIds = new ArrayList<Integer>();
			Set<Integer> resourceIdSet = mIdLetterMap.keySet();
			
			for (Integer resourceId : resourceIdSet) {
				mResourceIds.add(resourceId);
			}
		}
		
		return mResourceIds;
	}
}
