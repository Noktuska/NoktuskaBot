package com.noktuska.bot.noktuskabot_revamped;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.inofficial.Inofficial;
import com.noktuska.bot.noktuskabot_revamped.listener.DiscordListener;
import com.noktuska.bot.noktuskabot_revamped.listener.OsuTwitchListener;
import com.noktuska.bot.noktuskabot_revamped.rmi.RmiServer;
import com.noktuska.bot.noktuskabot_revamped.structs.Request;
import com.noktuska.bot.noktuskabot_revamped.structs.Quote;
import com.noktuska.bot.noktuskabot_revamped.structs.Server;
import com.noktuska.bot.noktuskabot_revamped.structs.queue.VoiceHandler;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

public class Main {
	
	public static final String VERSION_NUMBER = "1.1.0";
	
	public Thread osuTwitchThread;
	private RmiServer rmiServer;
	
	public IDiscordClient client;

	public Console console = new Console(this);
	
	public VoiceHandler voiceHandler = new VoiceHandler(this);
	
	public List<String> todoList = new ArrayList<String>();
	public List<Request> requests = new ArrayList<Request>();
	
	public List<Server> servers = new ArrayList<Server>();
	
	public String adminChannel;
	
	public int updateDelay = 60000;
	
	public boolean newUpdate = false;
	
	public boolean osuListener = true;
	public boolean twitchListener = true;
	
	public Main() {
		console.log("Starting NOKTUSKABOT...");
		console.log("Initialize inofficial functions");
		Inofficial.init(this);
		
		console.log("Trying to read savefile...");
		readSaveFile();
		
		console.log("Connecting to discord...");
		client = getClient(Reference.TOKEN, true);
		
		console.log("Registering listener...");
        EventDispatcher dispatcher = client.getDispatcher();
        dispatcher.registerListener(new DiscordListener(this));
        dispatcher.registerListener(voiceHandler);
        
        console.log("Registering Osu and Twitch Thread...");
        osuTwitchThread = new Thread(new OsuTwitchListener(this));
        osuTwitchThread.start();
        
        console.log("Registering RMI server...");
        try {
			rmiServer = new RmiServer(this);
	        rmiServer.register();
	        console.log("Registered successfully!");
		} catch (RemoteException e) {
			e.printStackTrace();
			console.log("Registering failed: " + e.getMessage());
		}
        
        console.log("Initialization finished!");
	}
	
	public void sendMessage(IChannel channel, String msg) {
		try {
			channel.sendMessage(msg);
		} catch (RateLimitException | MissingPermissionsException | DiscordException e) {
			console.log(e.getMessage());
		}
	}
	
	public void sendFile(IChannel channel, File file, String msg) {
		try {
			channel.sendFile(file, msg);
		} catch (RateLimitException | IOException | MissingPermissionsException | DiscordException e) {
			console.log(e.getMessage());
		}
	}
	
	public void handleReply(IMessage message, String msg) {
		try {
			message.reply(msg);
		} catch (RateLimitException | MissingPermissionsException | DiscordException e) {
			console.log(e.getMessage());
		}
	}
	
	public void readSaveFile() {
		try {
			FileReader fr = new FileReader(Reference.SAVE_PATH);
			BufferedReader br = new BufferedReader(fr);
			
			Server curServer = null;
			String curSection = "";
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("[")) {
					curSection = line;
					continue;
				}
				
				if (line.startsWith("{")) {
					servers.add(new Server(this, line.substring(1, line.length() - 1)));
					curServer = servers.get(servers.size() - 1);
					continue;
				}
				
				if (line.startsWith("Version number ")) {
					String newVersion = line.substring("Version number ".length());
					if (!VERSION_NUMBER.equals(newVersion)) {
						newUpdate = true;
					}
					continue;
				}
				
				if (curSection.equals("[TODO]")) {
					todoList.add(line);
				} else if (curSection.equals("[REQUEST]")) {
					String[] splits = line.split(";");
					requests.add(new Request(splits[0], splits[1], requests.size(), this, Boolean.parseBoolean(splits[2])));
				} else if (curSection.equals("[ADMINCHANNEL]")) {
					adminChannel = line;
				} else {
					curServer.initValue(curSection, line);
				}
			}
			
			br.close();
			
			console.log("Savefile read successfully!");
			
			save();
		} catch (Exception e) {
			console.log("Error while reading savefile: " + e.getMessage());
		}
	}
	
	public void save() {
		console.log("Trying to save...");
		
		String content = "Version number " + VERSION_NUMBER + "\n";
		for (Server server : servers) {
			content += "{" + server.serverID + "}\n";
			content += "[TWITCH]\n";
			for (int i = 0; i < server.streamer.size(); i++) {
				content += server.streamer.get(i).getName() + "\n";
			}
			content += "[OSU]\n";
			for (int i = 0; i < server.osuPlayer.size(); i++) {
				content += server.osuPlayer.get(i).getName() + "," + server.osuPlayer.get(i).getPpBorder() + "\n";
			}
			content += "[CHANNELS]\n";
			for (int i = 0; i < server.channelIds.size(); i++) {
				content += server.channelIds.get(i) + "\n";
			}
			content += "[QUOTES]\n";
			for (Quote q : server.quotes) {
				content += q.getKeyword() + ";" + q.getAuthor() + ";" + q.getQuote().replace(';', ' ') + "\n";
			}
			content += "[VALUES]\n";
			//content += "update " + this.updateDelay + "\n";
			content += "ppborder " + server.ppBorder + "\n";
			content += "[BANNED]\n";
			for (String s : server.banned) {
				content += s + "\n";
			}
			content += "[ADMINS]\n";
			for (String s : server.admins) {
				content += s + "\n";
			}
			content += "[MUTED]\n";
			for (String s : server.muted) {
				content += s + "\n";
			}
			content += "[ANNOUNCEMENTS]\n";
			for (String s : server.announcements) {
				content += s + "\n";
			}
		}
		content += "[TODO]\n";
		for (String s : todoList) {
			content += s + "\n";
		}
		content += "[REQUEST]\n";
		for (Request p : requests) {
			content += p.getRequest().replace(';', ' ') + ";" + p.getAuthorID() + ";" + p.isConfirmed() + "\n";
		}
		content += "[ADMINCHANNEL]\n";
		content += adminChannel + "\n";
		
		writeFile("NoktuskaBotData.vds", content);
		
		console.log("[ OK ]   Saving complete!");
	}
	
	public void writeFile(String location, String content) {
		try {
			FileWriter fw = new FileWriter(location);
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write(content);
			
			bw.close();
		} catch (IOException e) {
			console.log("ERROR: while writing file: " + e.getMessage());
		}
	}
	
	public Server getServer(String id) {
		for (Server elem : servers) {
			if (elem.serverID.equals(id))
				return elem;
		}
		return null;
	}
	
    public static void main(String[] args) {
    	new Main();
    }
    
    public static IDiscordClient getClient(String token, boolean login) {
    	try {
	    	ClientBuilder clientBuilder = new ClientBuilder();
	    	clientBuilder.withToken(token);
	    	if (login) {
	    		return clientBuilder.login();
	    	} else {
	    		return clientBuilder.build();
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return null;
    }
    
}