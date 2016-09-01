package com.noktuska.bot.noktuskabot_revamped.structs;

import sx.blah.discord.handle.obj.IMessage;

public abstract class Command {

	public Command(String key) {
		this.key = key;
	}
	
	public String key = "unused";
	
	public abstract String execute(String[] args, IMessage msg, Server server);

}
