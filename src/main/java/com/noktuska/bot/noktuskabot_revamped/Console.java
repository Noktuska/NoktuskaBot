package com.noktuska.bot.noktuskabot_revamped;

import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.structs.Server;
import com.noktuska.main.Pair;

public class Console {

	private Main main;
	
	private int maxLogAmount = 100;
	private List<Pair> logs = new ArrayList<Pair>();
	private List<Pair> newMessages = new ArrayList<Pair>();
	
	private boolean precLog = false;
	
	public Console(Main main) {
		this.main = main;
	}
	
	public void log(String msg) {
		long ms = System.currentTimeMillis();
		logs.add(new Pair("" + ms, msg));
		newMessages.add(new Pair("" + ms, msg));
		if (logs.size() > maxLogAmount) {
			logs.remove(0);
		}
		if (newMessages.size() > maxLogAmount) {
			newMessages.remove(0);
		}
	}
	
	public void preciseLog(String msg) {
		if (precLog)
			log(msg);
	}
	
	public void logWithoutResend(String msg) {
		logs.add(new Pair("" + System.currentTimeMillis(), msg));
		if (logs.size() > maxLogAmount) {
			logs.remove(0);
		}
	}
	
	public void setMaxLogAmount(int value) {
		maxLogAmount = value;
	}

	public List<Pair> getLogs() {
		return logs;
	}
	
	public List<Pair> getNewLogs() {
		return newMessages;
	}
	
	public void clearNewLogs() {
		newMessages.clear();
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
	
	public void compile(String command) {
		String[] args = getArgs(command);
		
		try {
			if (command.startsWith("save")) {
				if (args.length > 0)
					throw new Exception("Invalid number of arguments!");
				main.save();
				
			} else if (command.startsWith("send")) {
				if (args.length != 1)
					throw new Exception("Invalid number of arguments!");
				for (Server elem : main.servers) {
					for (int i = 0; i < elem.channelIds.size(); i++) {
						main.sendMessage(main.client.getChannelByID(elem.channelIds.get(i)), args[0]);
					}
				}
				
			} else if (command.startsWith("setvalue")) {
				if (args.length != 2)
					throw new Exception("Invalid number of arguments!");
				if (args[0].equals("update")) {
					main.updateDelay = Integer.parseInt(args[1]);
				} else if (args[0].equals("maxlogs")) {
					maxLogAmount = Integer.parseInt(args[1]);
				} else {
					throw new Exception("Invalid argument: " + args[0]);
				}
				
			} else if (command.startsWith("preclog")) {
				precLog ^= true;
				log("precLogging set to " + precLog);
				
			} else {
				throw new Exception("Unknown command: " + command);
			}
		} catch (Exception e) {
			log("ERROR: " + e.getMessage());
		}
	}
	
}
