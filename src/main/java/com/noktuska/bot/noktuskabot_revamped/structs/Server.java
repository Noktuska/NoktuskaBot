package com.noktuska.bot.noktuskabot_revamped.structs;

import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.Main;
import com.noktuska.bot.noktuskabot_revamped.Reference;
import com.noktuska.bot.noktuskabot_revamped.api.BeatmapList;
import com.noktuska.bot.noktuskabot_revamped.api.OsuAPI;
import com.noktuska.bot.noktuskabot_revamped.api.OsuBeatmapDownloader;
import com.noktuska.bot.noktuskabot_revamped.structs.polls.Poll;

public class Server {
	
	public String serverID;
	private Main main;
	
	//Server specific
	public Poll currentPoll = null;

	public boolean isDownloading = false;
	public OsuBeatmapDownloader downloader = null;
	
	public BeatmapList downloadList = null;
	
	public List<String> channelIds = new ArrayList<String>();
	public List<String> admins = new ArrayList<String>();
	public List<String> banned = new ArrayList<String>();
	public List<String> muted = new ArrayList<String>();
	
	public List<TwitchStreamer> streamer = new ArrayList<>();
	public List<OsuPlayer> osuPlayer = new ArrayList<>();
	
	public List<Quote> quotes = new ArrayList<Quote>();
	
	public List<String> announcements = new ArrayList<String>();
	
	public int ppBorder = 0;
	
	public long messagesSince = 0;
	public long lastAnnouncement = System.currentTimeMillis();
	public int announcementCounter = 0;
	
	public Server(Main main, String serverID) {
		downloadList = new BeatmapList(main, 10);
		this.main = main;
		this.serverID = serverID;
	}
	
	public void initValue(String curSection, String line) throws Exception {
		if (curSection.equals("[TWITCH]")) {
			streamer.add(new TwitchStreamer(line));
		} else if (curSection.equals("[OSU]")) {
			String[] splits = line.split(",");
			double[] pp = new double[4];
			for (int m = 0; m < 4; m++) {
				OsuAPI data = new OsuAPI("get_user", "u=" + splits[0] + "&m=" + m, null);
				if (!data.isValid() || data.getObject("pp_raw") == null) {
					main.console.log("WARNING: " + splits[0] + " has invalid data for mode " + m + "!");
					pp[m] = 0;
				} else
					pp[m] = Double.parseDouble((String)data.getObject("pp_raw"));
			}
			int ppBorder = (splits.length > 1 ? Integer.parseInt(splits[1]) : -1);
			osuPlayer.add(new OsuPlayer(splits[0], pp, ppBorder));
		} else if (curSection.equals("[CHANNELS]")) {
			channelIds.add(line);
		} else if (curSection.equals("[QUOTES]")) {
			String[] splits = line.split(";");
			quotes.add(new Quote(splits[0], splits[1], splits[2]));
		} else if (curSection.equals("[VALUES]")) {
			String key = line.split(" ")[0];
			int value = Integer.parseInt(line.split(" ")[1]);
			if (key.equals("ppborder")) {
				ppBorder = value;
			} else {
				main.console.log("WARNING: Undefined key in savefile!");
			}
		} else if (curSection.equals("[BANNED]")) {
			banned.add(line);
		} else if (curSection.equals("[ADMINS]")) {
			admins.add(line);
		} else if (curSection.equals("[MUTED]")) {
			muted.add(line);
		} else if (curSection.equals("[ANNOUNCEMENTS]")) {
			announcements.add(line);
		} else {
			main.console.log("WARNING: Undefined section: " + curSection);
		}
	}
	
	public Permission getPermissionFor(String user) {
		if (user.equals(Reference.OWNER_ID))
			return Permission.Owner;
		if (admins.contains(user))
			return Permission.Admin;
		if (banned.contains(user))
			return Permission.Banned;
		return Permission.User;
	}

}
