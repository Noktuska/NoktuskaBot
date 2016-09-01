package com.noktuska.bot.noktuskabot_revamped.structs.queue;

import java.io.File;

public class Queue {

	public String artist;
	public String title;
	
	public String filePath;
	
	public Queue(String artist, String title, String filePath) {
		super();
		this.artist = artist;
		this.title = title;
		this.filePath = filePath;
	}
	
	public void delete() {
		File dir = new File(filePath.substring(0, filePath.lastIndexOf(File.separatorChar)));
		if (dir.exists() && dir.isDirectory()) {
			dir.delete();
		}
	}
	
	@Override
	public String toString() {
		return artist + " - " + title;
	}

}
