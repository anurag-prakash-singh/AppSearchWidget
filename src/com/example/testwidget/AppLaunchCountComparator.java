package com.example.testwidget;

import java.util.Comparator;

public class AppLaunchCountComparator implements Comparator<ApplicationListItem> {
	@Override
	public int compare(ApplicationListItem item1, ApplicationListItem item2) {
		return item1.getLaunchCount() - item2.getLaunchCount();
	}
}
