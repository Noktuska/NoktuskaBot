package com.noktuska.bot.noktuskabot_revamped.listener;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.json.JSONObject;

import com.noktuska.bot.noktuskabot_revamped.Main;
import com.noktuska.bot.noktuskabot_revamped.Reference;
import com.noktuska.bot.noktuskabot_revamped.api.OsuAPI;
import com.noktuska.bot.noktuskabot_revamped.api.ppcalculator.PpCalculator;
import com.noktuska.bot.noktuskabot_revamped.structs.Command;
import com.noktuska.bot.noktuskabot_revamped.structs.OsuPlayer;
import com.noktuska.bot.noktuskabot_revamped.structs.Permission;
import com.noktuska.bot.noktuskabot_revamped.structs.Quote;
import com.noktuska.bot.noktuskabot_revamped.structs.Request;
import com.noktuska.bot.noktuskabot_revamped.structs.Server;
import com.noktuska.bot.noktuskabot_revamped.structs.TwitchStreamer;
import com.noktuska.bot.noktuskabot_revamped.structs.beatmap.Beatmap;
import com.noktuska.bot.noktuskabot_revamped.structs.polls.Poll;
import com.noktuska.bot.noktuskabot_revamped.structs.polls.PollAnswer;
import com.noktuska.bot.noktuskabot_revamped.utils.Func;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.DiscordDisconnectedEvent;
import sx.blah.discord.handle.impl.events.MentionEvent;
//import sx.blah.discord.handle.impl.events.MessageDeleteEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
//import sx.blah.discord.handle.obj.IMessage.Attachment;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

public class DiscordListener {
	
	private Main main;
	private List<Command> commands = new ArrayList<Command>();
	
	public DiscordListener(Main main) {
		this.main = main;
		
		createCommands();
	}
	
	@SuppressWarnings("unused")
	private String getMode(int mode) {
		switch (mode) {
		default:
		case 0:
			return "osu";
		case 1:
			return "taiko";
		case 2:
			return "ctb";
		case 3:
			return "mania";
		}
	}
	private int getMode(String mode) {
		switch (mode.toLowerCase()) {
		default:
		case "osu":
			return 0;
		case "taiko":
			return 1;
		case "catch the beat":
		case "ctb":
			return 2;
		case "mania":
			return 3;
		}
	}
	
	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		//Add myself as an administrator
		for (Server elem : main.servers) {
			if (!elem.admins.contains(Reference.OWNER_ID))
				elem.admins.add(Reference.OWNER_ID);
			if (main.newUpdate) {
				for (String channel : elem.channelIds) {
					main.sendMessage(main.client.getChannelByID(channel), "Running new Version " + Main.VERSION_NUMBER);
				}
				main.save();
			}
		}
		for (Request elem : main.requests) {
			if (!elem.hasAuthor())
				elem.getAuthor();
		}
		
		main.console.log("Discord listener ready!");
	}
	
	@EventSubscriber
	public void onMentionEvent(MentionEvent event) {
		IMessage msg = event.getMessage();
		IChannel channel = msg.getChannel();
		String content = msg.getContent();
		
		Server server = main.getServer(msg.getGuild().getID());
		if (server == null)
			return;
		if (!server.channelIds.contains(channel.getID()))
			return;
		
		//Logging messages would be spying other channels
		//main.console.log(author.getName() + ": " + content);
		
		String cmd = content.substring(("<@" + main.client.getOurUser().getID() + "> ").length());
		String[] uargs = getArgs(cmd);
		
		for (Command elem : commands) {
			if (cmd.toLowerCase().startsWith(elem.key)) {
				main.console.log("Command found: " + elem.key);
				try {
					String reply = elem.execute(uargs, msg, server);
					if (reply != null && !reply.equals(""))
						main.sendMessage(channel, reply);
				} catch (Exception e) {
					main.sendMessage(channel, "Are you trying to crash me? Some evil invalid data came out of that request!");
				}
				break;
			}
		}
	}
	
	@EventSubscriber
	public void onMessageReceivedEvent(MessageReceivedEvent event) {
		IMessage msg = event.getMessage();
		IUser author = msg.getAuthor();
		IChannel channel = msg.getChannel();
		String content = msg.getContent();
		Server server = main.getServer(msg.getGuild().getID());
		
		//For joining leaving channels
		if (content.equals("!join")) {
			if (msg.getGuild().getOwnerID().equals(author.getID()) || author.getID().equals(Reference.OWNER_ID)) {
				if (server == null) {
					server = new Server(main, msg.getGuild().getID());
					main.servers.add(server);
				}
				if (!server.channelIds.contains(channel.getID())) {
					main.sendMessage(channel, "Hello there, I'm NoktuskaBot and here to serve you with your desires!");
					server.channelIds.add(channel.getID());
				}
				if (!server.admins.contains(author.getID()))
					server.admins.add(author.getID());
				main.save();
			}
			return;
		}
		if (content.equals("!leave")) {
			if (msg.getGuild().getOwnerID().equals(author.getID()) || author.getID().equals(Reference.OWNER_ID)) {
				if (server != null) {
					main.sendMessage(channel, "Had a nice time here. See ya!");
					server.channelIds.remove(channel.getID());
					if (server.channelIds.size() == 0) {
						main.servers.remove(server);
					}
					main.save();
				}
			}
			return;
		}
		
		if (server == null)
			return;

		server.messagesSince++;
		
		if (!server.channelIds.contains(channel.getID()) || server.muted.size() == 0)
			return;
		
		//Logging messages would be spying
		//main.console.log("Received: " + content);
		
		if (server.muted.contains(author.getID())) {
			try {
				msg.delete();
			} catch (RateLimitException | MissingPermissionsException | DiscordException e) {
				main.console.log(e.getMessage());
			}
		}
	}
	
	@EventSubscriber
	public void onDisconnect(DiscordDisconnectedEvent event) {
		main.console.log("Disconnected from discord!");
		main.console.log("Reason: " + event.getReason().name());
	}
	
	private String[] getArgs(String msg) {
		String cpy = new String(msg);
		cpy = cpy.trim();
		if (cpy.indexOf(' ') == -1)
			return new String[0];
		
		cpy = cpy.substring(cpy.indexOf(' ') + 1);
		
		if (!cpy.contains("\""))
			return cpy.split(" ");
		
		String tmp = "";
		List<String> result = new ArrayList<String>();
		boolean openBracket = false;
		for (int i = 0; i < cpy.length(); i++) {
			char c = cpy.charAt(i);
			
			if (c == '"') {
				openBracket = !openBracket;
				continue;
			}
			
			if (c == ' ' && !openBracket) {
				result.add(tmp);
				tmp = "";
			} else {
				tmp += c;
			}
		}
		result.add(tmp);
		
		return result.toArray(new String[result.size()]);
	}
	
	private boolean appliesForRole(Server server, IUser user, Permission permission) {
		if (permission == Permission.Owner) {
			if (user.getID().equals(Reference.OWNER_ID))
				return true;
			return false;
		} else if (permission == Permission.Admin) {
			for (String s : server.admins) {
				if (s.equals(user.getID()))
					return true;
			}
			return false;
		} else if (permission == Permission.User) {
			for (String s : server.banned) {
				if (s.equals(user.getID()))
					return false;
			}
			return true;
		}
		return true;
	}
	
	private void createCommands() {
		commands.add(new Command("stats") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (args.length != 2)
					return ("Invalid number of arguments!");
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("Noktuska told me to not listen to you :c");
				
				OsuAPI stats = new OsuAPI("get_user", "u=" + args[0] + "&m=" + getMode(args[1]), null);
				
				if (!stats.isValid())
					return ("I couldn't fetch any data!");
				
				String name = (String)stats.getObject("username");
				String rank = (String)stats.getObject("pp_rank");
				String pp = (String)stats.getObject("pp_raw");
				String lvl = (String)stats.getObject("level");
				
				String result = Func.listValues("Username", name,
						"Level", lvl,
						"Rank", rank,
						"PP", pp);
				
				return result;
			}
		});
		commands.add(new Command("best") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (args.length != 2)
					return ("Invalid number of arguments!");
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("Noktuska told me to not listen to you :c");
				
				OsuAPI userBest = new OsuAPI("get_user_best", "u=" + args[0] + "&m=" + getMode(args[1]), null);
				
				if (!userBest.isValid())
					return ("I couldn't fetch any data!");
				
				String rank = (String)userBest.getObject("rank");
				String pp = (String)userBest.getObject("pp");
				String beatmapId = (String)userBest.getObject("beatmap_id");
				String mods = Func.getMods(Integer.parseInt((String)userBest.getObject("enabled_mods")));
				
				OsuAPI beatmapData = new OsuAPI("get_beatmaps", "b=" + beatmapId + "&m=" + getMode(args[1]) + "&a=1", null);
				
				if (!beatmapData.isValid())
					return ("I couldn't fetch any data!");

				OsuAPI scoreData = new OsuAPI("get_scores", "b=" + beatmapId + "&u=" + args[0] + "&m=" + getMode(args[1]), main.console);
				
				if (!scoreData.isValid())
					return ("I couldn't fetch any data!");
				
				int num300 = Integer.parseInt((String)scoreData.getObject("count300"));
				int num100 = Integer.parseInt((String)scoreData.getObject("count100"));
				int num50 = Integer.parseInt((String)scoreData.getObject("count50"));
				int numMiss = Integer.parseInt((String)scoreData.getObject("countmiss"));
				int countkatu = Integer.parseInt((String)scoreData.getObject("countkatu"));
				int countgeki = Integer.parseInt((String)scoreData.getObject("countgeki"));
				double acc = Func.getAccuracy(countgeki, countkatu, num300, num100, num50, numMiss, getMode(args[1]));
				
				String artist = (String)beatmapData.getObject("artist");
				String title = (String)beatmapData.getObject("title");
				String creator = (String)beatmapData.getObject("creator");
				String version = (String)beatmapData.getObject("version");
				String diff = (String)beatmapData.getObject("difficultyrating");
				String bpm = (String)beatmapData.getObject("bpm");
				
				if (diff.length() > 4)
					diff = diff.substring(0, 5);
				
				String result = Func.listValues("Username", args[0],
						"Song", artist + " - " + title + " [" + version + "] (" + creator + ")",
						"BPM", bpm,
						"Difficulty", diff + "*",
						"Mods", mods,
						"Rank", rank,
						"Accuracy", acc + "%",
						"PP", pp);
				result += "\nMaplink: <http://osu.ppy.sh/b/" + beatmapId + ">";
				
				return result;
			}
		});
		commands.add(new Command("recent") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("You have no permission to access this command!");
				if (args.length != 2)
					return ("Invalid number of arguments!");
				
				OsuAPI recent = new OsuAPI("get_user_recent", "u=" + args[0] + "&m=" + getMode(args[1]) + "&limit=50", main.console);
				if (!recent.isValid())
					return ("Whoops, something went wrong!");
				
				JSONObject recentObject = null;
				
				List<JSONObject> splits = recent.split();
				for (JSONObject api : splits) {
					if (!api.getString("rank").equals("F")) {
						recentObject = api;
						break;
					}
				}
				
				String beatmapId = recentObject.getString("beatmap_id");
				OsuAPI beatmap = new OsuAPI("get_beatmaps", "b=" + beatmapId + "&a=1&m=" + getMode(args[1]), main.console);
				
				if (!beatmap.isValid())
					return ("Whoops, something went wrong!");
				
				String artist = (String)beatmap.getObject("artist");
				String title = (String)beatmap.getObject("title");
				String creator = (String)beatmap.getObject("creator");
				String version = (String)beatmap.getObject("version");
				double diff = Double.parseDouble((String)beatmap.getObject("difficultyrating"));
				diff = Math.round(diff * 100.0) / 100.0;
				int mods = Integer.parseInt(recentObject.getString("enabled_mods"));
				String sMods = Func.getMods(mods);
				
				OsuAPI score = new OsuAPI("get_scores", "b=" + beatmapId + "&u=" + args[0] + "&m=" + getMode(args[1]), main.console);
				if (!score.isValid())
					return ("Whoops, something went wrong!");
				
				String scoreValue = recentObject.getString("score");
				int count300 = Integer.parseInt((String)score.getObject("count300"));
				int count100 = Integer.parseInt((String)score.getObject("count100"));
				int count50 = Integer.parseInt((String)score.getObject("count50"));
				int countmiss = Integer.parseInt((String)score.getObject("countmiss"));
				int countkatu = Integer.parseInt((String)score.getObject("countkatu"));
				int countgeki = Integer.parseInt((String)score.getObject("countgeki"));
				double acc = Func.getAccuracy(countgeki, countkatu, count300, count100, count50, countmiss, getMode(args[1]));
				acc = Math.round(acc * 100.0) / 100.0;
				String pp = (String)score.getObject("pp");
				
				return Func.listValues("Username", args[0],
						"Song", artist + " - " + title + " [" + version + "] (" + creator + ")",
						"Mods", sMods,
						"Accuracy", acc + "%",
						"Score", scoreValue,
						"PP", pp);
			}
		});
		commands.add(new Command("roll") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("Noktuska told me to not listen to you :c");
				Random r = new Random();
				if (args.length == 0) {
					int val = r.nextInt(100) + 1;
					return (msg.getAuthor().getName() + " rolled a " + val);
				} else if (args.length == 1) {
					int border = 100;
					try {
						border = Integer.parseInt(args[0]);
					} catch (Exception e) { }
					int val = r.nextInt(border) + 1;
					return (msg.getAuthor().getName() + " rolled a " + val);
				} else
					return ("Invalid number of arguments!");
			}
		});
		commands.add(new Command("list") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("Noktuska told me to not listen to you :c");
				if (args.length == 0) {
					String result = "```fix\n[OSU]\n";
					String spaces = "               ";
					for (int i = 0; i < server.osuPlayer.size(); i++) {
						OsuPlayer elem = server.osuPlayer.get(i);
						String spaceSub = spaces.substring(Math.min(elem.getName().length(), spaces.length()));
						result += elem.getName() + spaceSub + " | PP Border: " + (elem.getPpBorder() == -1 ? "None" : elem.getPpBorder()) + "\n";
					}
					result += "\n[TWITCH]\n";
					for (int i = 0; i < server.streamer.size(); i++) {
						result += server.streamer.get(i).getName() + "\n";
					}
					result += "```";
					return (result);
				}
				if (args.length != 3)
					return ("Invalid number of arguments!");
				if (!(args[0].equals("add") || args[0].equals("remove") || args[0].equals("addf")) || !(args[2].equals("osu") || args[2].equals("twitch")))
					return ("Invalid arguments!");
				if (args[2].equals("osu")) {
					if (args[0].equals("add") || args[0].equals("addf")) {
						for (int i = 0; i < server.osuPlayer.size(); i++) {
							if (server.osuPlayer.get(i).getName().toLowerCase().equals(args[1].toLowerCase())) {
								return ("Person is already on the list!");
							}
						}
						double[] pp = new double[4];
						boolean warn = false;
						for (int i = 0; i < 4; i++) {
							OsuAPI userData = new OsuAPI("get_user", "u=" + args[1] + "&m=" + i, null);
							if (!userData.isValid()) {
								if (args[0].equals("addf")) {
									warn = true;
									pp[i] = 0;
									continue;
								} else {
									return ("Couldn't get any data from that user!");
								}
							}
							pp[i] = Double.parseDouble((String)userData.getObject("pp_raw"));
						}
						server.osuPlayer.add(new OsuPlayer(args[1], pp, -1));
						main.save();
						if (warn)
							return ("Warning: This person contains invalid data! Added anyways (addf)");
						return ("I added that person to the list!");
					} else {
						for (int i = 0; i < server.osuPlayer.size(); i++) {
							if (server.osuPlayer.get(i).getName().toLowerCase().equals(args[1].toLowerCase())) {
								server.osuPlayer.remove(i);
								main.save();
								return ("I removed that person from the list");
							}
						}
						return ("It seems like the person isn't even on the list");
					}
				} else {
					if (args[0].equals("add")) {
						for (int i = 0; i < server.streamer.size(); i++) {
							if (server.streamer.get(i).getName().toLowerCase().equals(args[1].toLowerCase())) {
								return ("Person is already on the list");
							}
						}
						server.streamer.add(new TwitchStreamer(args[1]));
						main.save();
						return ("I added that person to the list");
					} else {
						for (int i = 0; i < server.streamer.size(); i++) {
							if (server.streamer.get(i).getName().toLowerCase().equals(args[1].toLowerCase())) {
								server.streamer.remove(i);
								main.save();
								return ("I removed that person from the list!");
							}
						}
						return ("It seems like the person isn't even on the list");
					}
				}
			}
		});
		commands.add(new Command("setborder") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (args.length != 1 && args.length != 2)
					return ("Invalid number of arguments");
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("Noktuska told me to not listen to you :c");
				try {
					if (args.length == 1) {
						server.ppBorder = Integer.parseInt(args[0]);
						main.save();
						return ("Only scores with " + server.ppBorder + " pp or more will be displayed!");
					} else {
						for (OsuPlayer elem : server.osuPlayer) {
							if (elem.getName().toLowerCase().equals(args[0].toLowerCase())) {
								elem.setPpBorder(Integer.parseInt(args[1]));
								main.save();
								return ("User `" + elem.getName() + "` has now a pp border of " + elem.getPpBorder());
							}
						}
						return "User `" + args[0] + "` not found...";
					}
				} catch (Exception e) { }
				return (args[0] + " is not a valid number...");
			}
		});
		commands.add(new Command("command") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				return ("If the link expires for some reason, blame Noktuska and not me: http://pastebin.com/cJQpD8hp");
			}
		});
		commands.add(new Command("quote") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("Noktuska told me to not listen to you :c");
				if (args.length == 1) {
					for (Quote q : server.quotes) {
						if (q.getKeyword().toLowerCase().equals(args[0].toLowerCase())) {
							return (q.toString());
						}
					}
				} else if (args.length == 2) {
					if (args[0].equals("remove")) {
						for (Quote q : server.quotes) {
							if (q.getKeyword().toLowerCase().equals(args[1].toLowerCase())) {
								server.quotes.remove(q);
								main.save();
								return ("Quote deleted!");
							}
						}
					}
				} else if (args.length == 4) {
					if (args[0].equals("add")) {
						for (Quote q : server.quotes) {
							if (q.getKeyword().toLowerCase().equals(args[1].toLowerCase())) {
								return ("Quote already exists!");
							}
						}
						server.quotes.add(new Quote(args[1], args[3], args[2]));
						main.save();
						return ("Quote added!");
					} else if (args[0].equals("edit")) {
						for (Quote q : server.quotes) {
							if (q.getKeyword().toLowerCase().equals(args[1].toLowerCase())) {
								q.modify(args[1], args[3], args[2]);
								main.save();
								return ("Quote edited!");
							}
						}
					}
				} else if (args.length == 0) {
					String result = "```Those quotes are available:\n";
					for (Quote q : server.quotes) {
						result += q.getKeyword() + "\n";
					}
					result += "```";
					return (result);
				}
				return ("Arguments could not be interpreted or there is an invalid number of them!");
			}
		});
		commands.add(new Command("join") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("You have no permission to access this command!");
				if (msg.getAuthor().getConnectedVoiceChannels().size() == 0)
					return ("You are in no voice channel I could join!");
				main.voiceHandler.switchVoiceChannel(msg.getAuthor().getConnectedVoiceChannels().get(0));
				return null;
			}
		});
		commands.add(new Command("leave") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("You have no permission to access this command!");
				if (main.client.getOurUser().getConnectedVoiceChannels().size() > 0)
					main.voiceHandler.switchVoiceChannel(null);
				else
					return ("I'm not even in a voice channel...");
				return null;
			}
		});
		commands.add(new Command("queue") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				return ("Currently I'm not able to play songs. I'm sorry!");
				
				/*if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("You have no permission to access this command!");
				if (args.length == 0) {
					String res = "```";
					for (Queue elem : main.voiceHandler.getQueues()) {
						res += elem.toString() + "\n";
					}
					res = res.substring(0, res.length() - 1);
					res += "```";
					return (res);
				}
				if (args.length != 1)
					return ("Invalid number of arguments!");
				main.voiceHandler.queueSong(args[0]);*/
			}
		});
		/*commands.add(new Command("volume") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("You have no permission to access this command!");
				if (args.length != 1)
					return ("Invalid number of arguments!");
				if (main.client.getOurUser().getConnectedVoiceChannels().isEmpty())
					return ("I'm not even in a voice channel...");
				try {
					main.client.getOurUser().getConnectedVoiceChannels());
				} catch (Exception e) {
					return (args[0] + " is not a valid number...");
				}
				return null;
			}
		});*/
		/*commands.add(new Command("skip") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.Admin))
					return ("You have no permission to access this command!");
				if (main.client.getOurUser().getConnectedVoiceChannels().size() > 0)
					try {
						main.client.getOurUser().getConnectedVoiceChannels().get(0).getAudioChannel().skip();
					} catch (DiscordException e) {
						return (e.getMessage());
					}
				return null;
			}
		});*/
		commands.add(new Command("mute") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.Admin))
					return ("You have no permission to access this command!");
				if (args.length != 1)
					return ("Invalid number of arguments!");
				for (IUser u : msg.getGuild().getUsers()) {
					if (u.getName().toLowerCase().equals(args[0].toLowerCase())) {
						if (!server.muted.contains(u.getID())) {
							server.muted.add(u.getID());
							main.save();
							return ("User " + u.getName() + " got muted!");
						} else {
							return ("User " + u.getName() + " is already muted!");
						}
					}
				}
				return ("User with the name \"" + args[0] + "\" could not be found.");
			}
		});
		commands.add(new Command("unmute") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.Admin))
					return ("You have no permission to access this command!");
				if (args.length != 1)
					return ("Invalid number of arguments!");
				for (IUser u : msg.getGuild().getUsers()) {
					if (u.getName().toLowerCase().equals(args[0].toLowerCase())) {
						if (server.muted.contains(u.getID())) {
							server.muted.remove(u.getID());
							main.save();
							return ("User " + u.getName() + " got unmuted!");
						} else {
							return ("User " + u.getName() + " is already unmuted!");
						}
					}
				}
				return ("User with the name \"" + args[0] + "\" could not be found!");
			}
		});
		commands.add(new Command("setrights") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.Admin))
					return ("You have no permission to access this command!");
				if (args.length != 2)
					return ("Invalid number of arguments!");
				IUser user = null;
				for (IUser u : msg.getGuild().getUsers()) {
					if (u.getName().toLowerCase().equals(args[0].toLowerCase())) {
						user = u;
						break;
					}
				}
				if (user == null)
					return ("No user found with the name " + args[0]);
				switch (args[1]) {
				case "admin":
					if (!server.admins.contains(user.getID())) {
						server.admins.add(user.getID());
						return (user.getName() + " has now access to all my commands!");
					}
					if (server.banned.contains(user.getID())) {
						server.banned.remove(user.getID());
						return (user.getName() + " has been unbanned!");
					}
					break;
				case "user":
					if (server.admins.contains(user.getID())) {
						server.admins.remove(user.getID());
						return ("The permissions of " + user.getName() + " have been restricted!");
					}
					if (server.banned.contains(user.getID())) {
						server.banned.remove(user.getID());
						return ("The permissions of " + user.getName() + " have been restored!");
					}
					break;
				case "banned":
					if (!server.banned.contains(user.getID())) {
						server.banned.add(user.getID());
						return (user.getName() + " has no more access to NoktuskaBot!");
					}
					if (server.admins.contains(user.getID())) {
						server.admins.remove(user.getID());
					}
				}
				main.save();
				return null;
			}
		});
		commands.add(new Command("poll") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (args.length == 0) {
					return (server.currentPoll == null ? "There is currently no poll!" : server.currentPoll.toString());
				} else if (args[0].equals("create")) {
					if (!appliesForRole(server, msg.getAuthor(), Permission.Admin))
						return ("You have no permission to access this command!");
					if (args.length < 4)
						return ("Invalid number of arguments!");
					if (server.currentPoll != null)
						return ("There is already a poll running!");
					PollAnswer[] answers = new PollAnswer[args.length - 2];
					for (int i = 2; i < args.length; i++) {
						answers[i - 2] = new PollAnswer(args[i]);
					}
					server.currentPoll = new Poll(args[1], answers);
					return (server.currentPoll.toString());
				} else if (args[0].equals("vote")) {
					if (!appliesForRole(server, msg.getAuthor(), Permission.User))
						return ("You have no permission to access this command!");
					if (args.length < 2)
						return ("Invalid number of arguments!");
					if (server.currentPoll == null)
						return ("There is no poll running!");
					try {
						server.currentPoll.tryVote(msg.getAuthor(), Integer.parseInt(args[1]) - 1);
					} catch (Exception e) {
						return (args[1] + " is not a valid number...");
					}
					return (msg.getAuthor().getName() + " voted for answer " + args[1]);
				} else if (args[0].equals("close")) {
					if (!appliesForRole(server, msg.getAuthor(), Permission.Admin))
						return ("You have no permission to access this command!");
					if (server.currentPoll == null)
						return ("There is no poll running!");
					server.currentPoll = null;
					return ("Poll closed!");
				} else if (args[0].equals("result")) {
					if (!appliesForRole(server, msg.getAuthor(), Permission.Admin))
						return ("You have no permission to access this command!");
					if (server.currentPoll == null)
						return ("There is no poll running!");
					boolean close = true;
					if (args.length == 2)
						try {
							close = Boolean.parseBoolean(args[1]);
						} catch (Exception e) {}
					BufferedImage resultImage = server.currentPoll.drawResults();
					File file = new File("res" + File.separator + "tmpImage.png");
					try {
						ImageIO.write(resultImage, "png", file);
					} catch (IOException e) {
						return (e.getMessage());
					}
					main.sendFile(msg.getChannel(), file, server.currentPoll.getResults());
					if (close)
						server.currentPoll = null;
					return (null);
				}
				return ("Arguments could not be interpreted or there is an invalid number of them!");
			}
		});
		commands.add(new Command("ping") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				return ("Pong");
			}
		});
		commands.add(new Command("pp") {
			@Override
			public String execute(String[] args, IMessage msg, final Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("You have no permission to access this command!");
				if (args.length < 1)
					return ("Invalid number of arguments!");
				final String[] argsCopy = args.clone();
				final IChannel channelCopy = msg.getChannel().copy();
				new Thread(new Runnable() {
					@Override
					public void run() {
						Beatmap b = null;
						try {
							b = PpCalculator.calcPp(main, argsCopy, server);
						} catch (Exception e) {
							main.sendMessage(channelCopy, "Error: " + e.getMessage());
							return;
						}
						if (b != null)
							main.sendMessage(channelCopy, Func.listValues(
									"Title", b.getTitle(),
									"Artist", b.getArtist(),
									"Creator", b.getCreator(),
									"Aim Value", "" + b.aimValue,
									"Speed Value", "" + b.speedValue,
									"Accuracy Value", "" + b.accValue,
									"Estimated PP", "" + b.ppValue));
						else
							main.sendMessage(channelCopy, "Something weird happen. But I don't know what...");
					}
				}).start();
				return null;
			}
		});
		commands.add(new Command("progress") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!server.isDownloading)
					return ("There is no download currently...");
				float progress = Math.round(server.downloader.getProgress() * 100f) / 100f;
				String result = "Progress: " + progress + "%\n";
				result += "Speed: " + server.downloader.getDownloadSpeed() + "\n";
				result += "Time remaining: " + server.downloader.getTimeRemaining() + "\n";
				return (result);
			}
		});
		commands.add(new Command("todo") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.Owner))
					return ("You don't have permission to access this command!");
				if (args.length == 0) {
					String result = "```\n";
					for (int i = 0; i < main.todoList.size(); i++) {
						result += i + ": " + main.todoList.get(i) + "\n";
					}
					result += "```";
					return result;
				} else if (args.length == 2 && args[1].equals("done")) {
					try {
						int index = Integer.parseInt(args[1]);
						String tmpCopy = main.todoList.get(index);
						main.todoList.remove(index);
						main.save();
						return tmpCopy;
					} catch (Exception e) {
						return e.getMessage();
					}
				}
				String todo = Func.concatArray(args, " ");
				main.todoList.add(todo);
				main.save();
				return ("I added that to the TODO list!");
			}
		});
		commands.add(new Command("request") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (args.length == 0) {
					if (!appliesForRole(server, msg.getAuthor(), Permission.Owner))
						return ("You don't have permission to access this command!");
					String result = "```\n";
					for (Request elem : main.requests) {
						result += elem.toString() + "\n";
					}
					result += "```";
					return result;
				} else if (args.length == 2) {
					if (args[0].toLowerCase().equals("request")) {
						if (!appliesForRole(server, msg.getAuthor(), Permission.User))
							return ("You don't have permission to access this command!");
						main.requests.add(new Request(args[1], msg.getAuthor(), main.requests.size(), main));
						IChannel adminChannel = main.client.getChannelByID(main.adminChannel);
						if (adminChannel != null)
							main.sendMessage(adminChannel, "New request: `" + args[1] + "` by `" + msg.getAuthor().getName() + "`");
						main.save();
						return ("Request added!");
					} else if (args[0].toLowerCase().equals("confirm")) {
						if (!appliesForRole(server, msg.getAuthor(), Permission.Owner))
							return ("You don't have permission to access this command!");
						try {
							int index = Integer.parseInt(args[1]);
							if (!main.requests.get(index).hasAuthor()) {
								if (!main.requests.get(index).getAuthor()) {
									return ("User with the ID \"" + main.requests.get(index).getAuthorID() + "\" could not be found!");
								}
							}
							main.requests.get(index).confirm();
							main.save();
							return ("Request \"" + main.requests.get(index).getRequest() + "\" confirmed!");
						} catch (Exception e) {
							return e.getMessage();
						}
					}
				}
				if (args.length >= 2) {
					if (!appliesForRole(server, msg.getAuthor(), Permission.Owner))
						return ("You don't have permission to access this command!");
					if (args[0].toLowerCase().equals("close")) {
						try {
							int index = Integer.parseInt(args[1]);
							Request elem = main.requests.get(index);
							if (!elem.hasAuthor()) {
								if (!elem.getAuthor()) {
									return ("User with the ID \"" + main.requests.get(index).getAuthorID() + "\" could not be found!");
								}
							}
							elem.close(args.length > 2 ? args[2] : null);
							main.requests.remove(elem);
							main.save();
							return ("Request closed!");
						} catch (Exception e) {
							return e.getMessage();
						}
					}
				}
				return ("Arguments could not be interpreted or there is an invalid number of them!");
			}
		});
		commands.add(new Command("announcement") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.Admin))
					return ("You don't have permission to access this command!");
				if (args.length != 2)
					return ("Invalid number of arguments!");
				if (args[0].toLowerCase().equals("add")) {
					server.announcements.add(args[1]);
					main.save();
					return ("Announcement added!");
				} else if (args[0].toLowerCase().equals("remove")) {
					for (String elem : server.announcements) {
						if (elem.toLowerCase().contains(args[1].toLowerCase())) {
							server.announcements.remove(elem);
							main.save();
							return ("Announcement: `" + elem + "` got removed!");
						}
					}
					return ("No announcement contained that line...");
				}
				return ("Arguments could not be interpreted or there is an invalid number of them!");
			}
		});
		commands.add(new Command("status") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("You don't have permission to access this command!");
				return Func.listValues("OsuTwitchThread", main.osuTwitchThread.isAlive() ? "Running" : "Offline");
			}
		});
		commands.add(new Command("restart") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.Owner))
					return ("You don't have permission to access this command!");
				if (main.osuTwitchThread.isAlive())
					return ("OsuTwitchThread is still running!");
				main.osuTwitchThread = new Thread(new OsuTwitchListener(main));
				main.osuTwitchThread.start();
				return ("OsuTwitchThread restarted successfully!");
			}
		});
		commands.add(new Command("whoislive") {
			@Override
			public String execute(String[] args, IMessage msg, Server server) {
				if (!appliesForRole(server, msg.getAuthor(), Permission.User))
					return ("You don't have permission to access this command!");
				String result = "";
				for (TwitchStreamer elem : server.streamer) {
					if (elem.isWasOnline()) {
						result += elem.getName() + ": <http://www.twitch.tv/" + elem.getName() + ">\n";
					}
				}
				return result;
			}
		});
	}
	
}