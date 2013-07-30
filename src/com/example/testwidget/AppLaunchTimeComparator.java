package com.example.testwidget;

import java.util.Comparator;

public class AppLaunchTimeComparator implements Comparator<ApplicationListItem> {
	@Override
	public int compare(ApplicationListItem lhs, ApplicationListItem rhs) {
		if (lhs.getLaunchTime() < rhs.getLaunchTime()) {
			return 1;
		} else if (rhs.getLaunchTime() < lhs.getLaunchTime()) {
			return -1;
		} else {
			return 0;			
		}
	}
}
