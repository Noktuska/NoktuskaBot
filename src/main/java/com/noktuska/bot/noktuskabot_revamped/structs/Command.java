package com.noktuska.bot.noktuskabot_revamped.structs;

import com.noktuska.bot.noktuskabot_revamped.Reference;

import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

public abstract class Command {

	private Permission permission;
	
	public Command(String key, Permission permission) {
		this.key = key;
		this.permission = permission;
	}
	
	public boolean isPermission(IUser user, Server server) {
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
	
	public String key = "unused";
	
	public abstract String execute(String[] args, IMessage msg, Server server);

}
