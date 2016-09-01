package com.noktuska.bot.noktuskabot_revamped.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.Console;
import com.noktuska.bot.noktuskabot_revamped.api.OsuAPI;
import com.noktuska.bot.noktuskabot_revamped.structs.Mod;

public class Func {
	
	public static enum FileSearchMethod {
		EXACT,
		BEGINS_WITH,
		ENDS_WITH,
		CONTAINS
	}
	
	public static double clamp(double val, double min, double max) {
		return (val < min ? min : (val > max ? max : val));
	}
	
	public static String readUrl(String url, Console logger) {
		URL site;
		
		try {
			site = new URL(url);
			BufferedReader in = new BufferedReader(new InputStreamReader(site.openStream()));
			
			String line = "";
			String result = "";
			while ((line = in.readLine()) != null) {
				result += line + "\n";
			}
			
			return result;
		} catch (Exception e) {
			if (logger != null)
				logger.log("Error: " + e.getMessage());
		}
		
		return "";
	}
	
	public static double getAccuracy(int countgeki, int countkatu, int num300, int num100, int num50, int numMiss, int mode) {
		int total = num300 + num100 + num50 + numMiss;
		int totalWithout50 = num300 + num100 + numMiss;
		int totalAll = total + countgeki + countkatu;
		
		double acc = 100;
		
		switch (mode) {
		default:
		case 0:
			acc = num300 * 100;
			acc += num100 * (100.0 / 3.0);
			acc += num50 * (100.0 / 6.0);
			
			acc /= total;
			break;
		case 1:
			acc = (num100 * 0.5 + num300) * 300.0;
			acc /= (totalWithout50 * 300.0);
			acc *= 100;
			break;
		case 2:
			acc = (double)(num300 + num100 + num50) / (double)(total + countkatu);
			acc *= 100;
			break;
		case 3:
			acc = num50 * 50.0 + num100 * 100.0 + countkatu * 200.0 + (num300 + countgeki) * 300.0;
			acc /= (totalAll * 300.0);
			acc *= 100.0;
			break;
		}
		acc = Math.round(acc * 100) / 100.0;
		
		return acc;
	}

	public static String getMods(int value) {
		List<Mod> mods = Mod.generateMods();
		
		String result = "";
		for (Mod mod : mods) {
			if ((value & mod.bit) > 0) {
				result += mod.representation + ", ";
			}
		}
		if (result.equals(""))
			return "Nomod";
		
		return result.substring(0, result.length() - 2);
	}
	
	public static String getBeatmapId(String beatmapUrl) {
		try {
			if (!beatmapUrl.startsWith("http"))
				return null;
			if (!beatmapUrl.contains("://osu.ppy.sh/"))
				return null;
			beatmapUrl = beatmapUrl.replace('?', '&');
			if (beatmapUrl.contains("osu.ppy.sh/s/")) {
				String set = beatmapUrl.substring(beatmapUrl.lastIndexOf('/') + 1);
				return (String)new OsuAPI("get_beatmaps", "s=" + set + "&a=1", null).getObject("beatmap_id");
			} else if (beatmapUrl.contains("osu.ppy.sh/b/")) {
				return beatmapUrl.substring(beatmapUrl.lastIndexOf('/') + 1, beatmapUrl.lastIndexOf('&'));
			} else if (beatmapUrl.contains("osu.ppy.sh/p/")) {
				return beatmapUrl.substring(beatmapUrl.lastIndexOf("b=") + 2, beatmapUrl.lastIndexOf('&'));
			}
		} catch (Exception e) {
		}
		return null;
	}
	
	public static String[] splitString(String str, int chars) {
		String[] array = new String[(int)Math.ceil(str.length() / (double)chars)];
		
		String tmp = "";
		int counter = 0;
		int arrayCounter = 0;
		for (int i = 0; i < str.length(); i++) {
			tmp += str.charAt(i);
			if (++counter >= chars) {
				array[arrayCounter++] = tmp;
				tmp = "";
				counter = 0;
			}
		}
		
		return array;
	}
	
	public static File loopFolder(String dirPath, String keyword, FileSearchMethod method) {
		File dir = new File(dirPath);
		if (!dir.exists())
			return null;
		if (!dir.isDirectory())
			return null;
		
		for (File elem : dir.listFiles()) {
			switch (method) {
			case BEGINS_WITH:
				if (elem.getName().startsWith(keyword))
					return elem;
				break;
			case CONTAINS:
				if (elem.getName().contains(keyword))
					return elem;
				break;
			case ENDS_WITH:
				if (elem.getName().endsWith(keyword))
					return elem;
				break;
			default:
			case EXACT:
				if (elem.getName().equals(keyword))
					return elem;
				break;
			}
		}
		return null;
	}
	
	public static File[] getSubFolders(String dirPath) {
		File dir = new File(dirPath);
		if (!dir.exists())
			return new File[0];
		if (!dir.isDirectory())
			return new File[0];
		return dir.listFiles();
	}
	
	public static String listValues(String... strs) {
		if (strs.length % 2 != 0)
			return null;
		
		String[] keys = new String[strs.length / 2];
		String[] values = new String[strs.length / 2];
		
		for (int i = 0; i < strs.length; i++) {
			if (i % 2 == 0)
				keys[i / 2] = strs[i];
			else
				values[i / 2] = strs[i];
		}
		String spaces = "               ";
		
		String result = "```fix\n";
		for (int i = 0; i < keys.length; i++) {
			result += keys[i] + spaces.substring(keys[i].length()) + ": " + values[i] + "\n";
		}
		result += "```";
		
		return result;
	}
	
	public static void deleteFileOrFolder(final Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
				throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
			
			@Override public FileVisitResult visitFileFailed(final Path file, final IOException e) {
				return handleException(e);
			}
			
			private FileVisitResult handleException(final IOException e) {
				e.printStackTrace(); // replace with more robust error handling
				return FileVisitResult.TERMINATE;
			}
			
			@Override
			public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
				if (e != null)
					return handleException(e);
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	};
	
	public static <T> List<T> arrayToList(T[] array) {
		List<T> result = new ArrayList<T>();
		for (T elem : array) {
			result.add(elem);
		}
		return result;
	}
}