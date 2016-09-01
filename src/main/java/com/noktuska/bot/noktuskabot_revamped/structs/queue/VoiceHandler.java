package com.noktuska.bot.noktuskabot_revamped.structs.queue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.Main;
import com.noktuska.bot.noktuskabot_revamped.structs.Server;
import com.noktuska.bot.noktuskabot_revamped.utils.Func;
import com.noktuska.bot.noktuskabot_revamped.utils.Func.FileSearchMethod;

import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.MissingPermissionsException;

public class VoiceHandler {
	
	private Main main;
	private List<Queue> queues = new ArrayList<Queue>();
	
	public VoiceHandler(Main main) {
		this.main = main;
	}
	
	public void switchVoiceChannel(final IVoiceChannel newChannel) {
		IUser ourUser = main.client.getOurUser();
		
		for (IVoiceChannel elem : ourUser.getConnectedVoiceChannels()) {
			elem.leave();
		}
		
		try {
			if (newChannel != null)
				newChannel.join();
		} catch (MissingPermissionsException e) {
			main.console.log(e.getMessage());
		}
	}
	
	public void queueSong(final String beatmapUrl, final Server server) {
		if (main.client.getOurUser().getConnectedVoiceChannels().isEmpty() || server.isDownloading)
			return;
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String artist = "Unknown Artist";
					String title = "Unknown Title";
					String folder = server.downloadList.createRequest(beatmapUrl, server);
					File data = Func.loopFolder(folder, ".osu", FileSearchMethod.ENDS_WITH);
					File mp3 = null;
					String line;
					BufferedReader br = new BufferedReader(new FileReader(data));
					int counter = 0;
					while ((line = br.readLine()) != null) {
						if (line.startsWith("AudioFilename:")) {
							String name = line.split(":")[1].trim();
							mp3 = Func.loopFolder(folder, name, FileSearchMethod.EXACT);
							counter++;
						}
						if (line.startsWith("Title:")) {
							title = line.split(":")[1].trim();
							counter++;
						}
						if (line.startsWith("Artist:")) {
							artist = line.split(":")[1].trim();
							counter++;
						}
						if (counter >= 3)
							break;
					}
					br.close();
					if (mp3 == null)
						throw new Exception("The beatmap doesn't contain any soundfile!");
					queues.add(new Queue(artist, title, mp3.getAbsolutePath()));
					//main.client.getOurUser().getVoiceChannel().get().getAudioChannel().queueFile(mp3);
				} catch (Exception e) {
					main.console.log("Error: " + e.getMessage());
				}
			}
		}).start();
	}
	
	public List<Queue> getQueues() {
		return queues;
	}
	
}