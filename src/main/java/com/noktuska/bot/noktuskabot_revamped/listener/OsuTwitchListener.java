package com.noktuska.bot.noktuskabot_revamped.listener;

import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.Main;
import com.noktuska.bot.noktuskabot_revamped.api.OsuAPI;
import com.noktuska.bot.noktuskabot_revamped.api.TwitchAPI;
import com.noktuska.bot.noktuskabot_revamped.inofficial.Inofficial;
import com.noktuska.bot.noktuskabot_revamped.structs.OsuPlayer;
import com.noktuska.bot.noktuskabot_revamped.structs.Server;
import com.noktuska.bot.noktuskabot_revamped.structs.TwitchStreamer;
import com.noktuska.bot.noktuskabot_revamped.utils.Func;

public class OsuTwitchListener implements Runnable {

	private Main main;
	private boolean running = true;
	
	public OsuTwitchListener(Main main) {
		this.main = main;
	}
	
	public void run() {
		long lastTime = System.currentTimeMillis();
		long inofficalLastTime = System.currentTimeMillis();
		
		while (running) {
			long now = System.currentTimeMillis();
			long delta = now - lastTime;
			
			long inofficialNow = System.currentTimeMillis();
			long inofficialDelta = inofficialNow - inofficalLastTime;
			
			for (Server elem : main.servers) {
				if (elem.announcements.size() > 0) {
					if (elem.messagesSince > 15) {
						long deltaAnn = now - elem.lastAnnouncement;
						if (deltaAnn >= 1800000) {
							elem.lastAnnouncement = now;
							for (String channel : elem.channelIds) {
								main.sendMessage(main.client.getChannelByID(channel), elem.announcements.get(elem.announcementCounter));
							}
							elem.announcementCounter++;
						}
					} else {
						elem.lastAnnouncement = now;
					}
				}
			}
			
			if (inofficialDelta > 1000) {
				inofficalLastTime = inofficialNow;
				
				Inofficial.tickAll();
			}
			
			if (delta > main.updateDelay) {
				lastTime = now;
				
				main.console.preciseLog("Fetching new pp and twitch data");
				
				for (Server elem : main.servers) {
					List<OsuPlayer> osuPlayer = elem.osuPlayer;
					List<TwitchStreamer> twitchStreamer = elem.streamer;
					
					try {
						for (int i = 0; i < osuPlayer.size(); i++) {
							OsuPlayer p = osuPlayer.get(i);
							
							for (int m = 0; m < 4; m++) {
								OsuAPI userData = new OsuAPI("get_user", "u=" + p.getName() + "&m=" + m, main.console);
								
								main.console.preciseLog(p.getName() + ": m = " + m);
								
								try {
									double pp = Double.parseDouble((String)userData.getObject("pp_raw"));
									
									if (pp > p.getPp()[m]) {
										main.console.log("PP increase for \"" + p.getName() + "\" (+" + (pp - p.getPp()[m]) + ") in m = " + m);
										
										OsuAPI recentData = new OsuAPI("get_user_recent", "u=" + p.getName() + "&m=" + m, main.console);
										
										String mods = Func.getMods(Integer.parseInt((String)recentData.getObject("enabled_mods")));
										if (mods.equals(""))
											mods = "No mods";
										
										String beatmapId = (String)recentData.getObject("beatmap_id");
										OsuAPI beatmapData = new OsuAPI("get_beatmaps", "b=" + beatmapId + "&m=" + m + "&a=1", main.console);
										OsuAPI scoreData = new OsuAPI("get_scores", "b=" + beatmapId + "&u=" + p.getName() + "&m=" + m, main.console);
										
										int num300 = Integer.parseInt((String)scoreData.getObject("count300"));
										int num100 = Integer.parseInt((String)scoreData.getObject("count100"));
										int num50 = Integer.parseInt((String)scoreData.getObject("count50"));
										int numMiss = Integer.parseInt((String)scoreData.getObject("countmiss"));
										int countkatu = Integer.parseInt((String)scoreData.getObject("countkatu"));
										int countgeki = Integer.parseInt((String)scoreData.getObject("countgeki"));
										double acc = Func.getAccuracy(countgeki, countkatu, num300, num100, num50, numMiss, m);
										
										main.console.log("Acc: " + acc);
										
										String artist = (String)beatmapData.getObject("artist");
										String title = (String)beatmapData.getObject("title");
										String creator = (String)beatmapData.getObject("creator");
										String diff = (String)beatmapData.getObject("difficultyrating");
										if (diff.length() > 4) {
											diff = diff.substring(0, 5);
										}
										String version = (String)beatmapData.getObject("version");
										String newPP = (String)scoreData.getObject("pp");
										
										double dpp = Double.parseDouble(newPP);
										
										if ((p.getPpBorder() != -1 && dpp >= p.getPpBorder()) || (dpp >= elem.ppBorder && p.getPpBorder() == -1)) {
											main.console.log("Fetched data for " + p.getName());
											
											for (int j = 0; j < elem.channelIds.size(); j++) {
												main.sendMessage(main.client.getChannelByID(elem.channelIds.get(j)), Func.listValues("Username", p.getName(),
														"Song", artist + " - " + title + " [" + version + "] (" + creator + ")",
														"Difficulty", diff + "*",
														"Mods", mods,
														"Accuracy", acc + "%",
														"PP", "" + newPP) + "\nDownload: <http://osu.ppy.sh/b/" + beatmapId + ">");
											}
										}
									}
									
									p.setPp(pp, m);
								} catch (Exception e) {
									main.console.preciseLog("ERROR: " + e.getMessage());
								}
							}
						}
						
						for (int i = 0; i < twitchStreamer.size(); i++) {
							TwitchStreamer p = twitchStreamer.get(i);
							
							TwitchAPI api = new TwitchAPI(p.getName(), main.console);
							int streamStatus = api.getStreamerStatus();
	
							main.console.preciseLog("Twitch streamer: " + p.getName() + " status: " + streamStatus);
							
							if (streamStatus == -1)
								for (String channel : elem.channelIds)
									main.sendMessage(main.client.getChannelByID(channel), p.getName() + " Does not exist, you should remove him from the list!");
							else if (streamStatus == 1 && !(p.isWasOnline())) {
								for (String channel : elem.channelIds)
									main.sendMessage(main.client.getChannelByID(channel), p.getName() + " just went live on twitch! http://www.twitch.tv/" + p.getName());
								p.setWasOnline(true);
							} else if (streamStatus != 1 && p.isWasOnline()) {
								main.console.log(p.getName() + " went offline (Twitch)");
								p.setWasOnline(false);
							}
						}
					} catch (Exception e) {
						main.console.log("WARN: " + e.getMessage());
					}
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				main.console.preciseLog("Thread.sleep interrupted");
			}
		}
	}

}
