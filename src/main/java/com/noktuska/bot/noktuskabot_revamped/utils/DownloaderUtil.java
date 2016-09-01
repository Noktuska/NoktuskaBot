package com.noktuska.bot.noktuskabot_revamped.utils;

import java.util.Arrays;

public class DownloaderUtil {
	
	private final static int[] illegalChars = {
			34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
			24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47
	};
	static {
		Arrays.sort(illegalChars);
	}
	
	public static String bytesToString(long bytes) {
		if (bytes < 1024)
			return bytes + " B";
		int exp = (int)(Math.log(bytes) / Math.log(1024));
		char pre = "KMGTPE".charAt(exp - 1);
		return String.format("%.1f %cB", bytes / Math.pow(1024,  exp), pre);
	}
	
	public static String cleanFileName(String badFileName, char replace) {
		if (badFileName == null)
			return null;
		
		boolean doReplace = (replace > 0 && Arrays.binarySearch(illegalChars, replace) < 0);
		StringBuilder cleanName = new StringBuilder();
		for (int i = 0, n = badFileName.length(); i < n; i++) {
			int c = badFileName.charAt(i);
			if (Arrays.binarySearch(illegalChars, c) < 0)
				cleanName.append((char)c);
			else if (doReplace)
				cleanName.append(replace);
		}
		return cleanName.toString();
	}
	
}
