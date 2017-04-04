package org.lip6.scheduler.utils;

public class Utils {
	public static int requireValidBounds(int value, int min, int max) {
		if (value < min || value > max) {
			throw new IllegalArgumentException("Value " + value + " is not valid.");
		}
		return value;
	}

	public static int requireValidBounds(int value, int min, int max, String message) {
		if (value < min || value > max) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}
}
