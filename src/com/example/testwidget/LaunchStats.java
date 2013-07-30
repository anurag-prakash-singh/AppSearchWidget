package com.example.testwidget;

public class LaunchStats {
	private int mUsageCount;
	private long mLastLaunchTimeSeconds;
	
	public LaunchStats(int usageCount, long lastLaunchTimeSeconds) {
		mUsageCount = usageCount;
		mLastLaunchTimeSeconds = lastLaunchTimeSeconds;
	}
	
	public void setUsageCount(int usageCount) {
		mUsageCount = usageCount;
	}
	
	public void incrementUsageCount() {
		mUsageCount++;
	}
	
	public int getUsageCount() {
		return mUsageCount;
	}
	
	public void setLastLaunchTimeSeconds(long lastLaunchTimeSeconds) {
		mLastLaunchTimeSeconds = lastLaunchTimeSeconds;
	}
	
	public long getLastLaunchTimeSeconds() {
		return mLastLaunchTimeSeconds;
	}
}
