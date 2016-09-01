package com.noktuska.bot.noktuskabot_revamped.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.Main;
import com.noktuska.bot.noktuskabot_revamped.api.OsuBeatmapDownloader.Status;
import com.noktuska.bot.noktuskabot_revamped.structs.Server;
import com.noktuska.bot.noktuskabot_revamped.utils.Func;
import com.noktuska.bot.noktuskabot_revamped.utils.Unzip;

public class BeatmapList {

	private int maxFolders;
	private Main main;
	private List<String> openFolders = new ArrayList<String>();
	
	public BeatmapList(Main main, int maxFolders) {
		this.main = main;
		this.maxFolders = maxFolders;
	}
	
	public String createRequest(String beatmapUrl, Server server) throws Exception {
		main.console.log("Download request: " + beatmapUrl);
		
		String beatmapId = Func.getBeatmapId(beatmapUrl);
		if (beatmapId == null)
			throw new Exception("Invalid beatmap link!");
		
		OsuAPI beatmap = new OsuAPI("get_beatmaps", "b=" + beatmapId + "&a=1", main.console);
		if (!beatmap.isValid())
			throw new Exception("Beatmap has invalid data!");
		
		String finalPath = "res" + File.separator + beatmapId;
		
		File folder = new File(finalPath + " Dir");
		if (!folder.exists()) {
			main.console.log("Folder doesn't exist");
			
			if (server.isDownloading)
				throw new Exception("Download is already running!");
			
			server.isDownloading = true;
			server.downloader = new OsuBeatmapDownloader("http://bloodcat.com/osu/s/" + (String)beatmap.getObject("beatmapset_id"), finalPath + ".osz", main.console);
			server.downloader.start();
			while (server.downloader.getStatus() != Status.COMPLETE) {
				if (server.downloader.getStatus() == Status.ERROR) {
					main.console.log("Received error");
					server.downloader.cancel();
					server.isDownloading = false;
					server.downloader = null;
					throw new Exception("Couldn't download the beatmap!");
				}
			}
			main.console.log("Download successful");
			server.isDownloading = false;
			server.downloader = null;
			Thread.sleep(2000);
			try {
				main.console.log("Unzipping");
				Unzip.unzip(finalPath + ".osz", finalPath + " Dir");
			} catch (IOException e) {
				main.console.log(e.getMessage());
				throw new Exception("Error while unzipping the .osz file");
			}
			main.console.log("Deleting osz");
			File osz = new File(finalPath + ".osz");
			if (osz.exists())
				osz.delete();
		}
		
		addFolder(folder.getPath());
		return folder.getPath();
	}
	
	public void removeFolder(int index) {
		File folder = new File(openFolders.get(index));
		if (folder.exists() && folder.isDirectory()) {
			try {
				Func.deleteFileOrFolder(folder.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		openFolders.remove(index);
	}
	
	public void removeFolder(String path) {
		for (int i = 0; i < openFolders.size(); i++) {
			if (path.equals(openFolders.get(i))) {
				removeFolder(i);
			}
		}
	}
	
	public int size() {
		return openFolders.size();
	}
	
	public String get(int index) {
		return openFolders.get(index);
	}
	
	private void addFolder(String path) {
		openFolders.add(path);
		if (openFolders.size() > maxFolders) {
			removeFolder(0);
		}
	}

}
