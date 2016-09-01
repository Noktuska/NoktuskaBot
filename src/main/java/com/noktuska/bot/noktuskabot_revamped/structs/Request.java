package com.noktuska.bot.noktuskabot_revamped.structs;

import com.noktuska.bot.noktuskabot_revamped.Main;

import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

public class Request {

	private boolean confirmed = false;
	private int index;
	private String request;
	private String authorID;
	private IUser author;
	private Main main;
	
	public Request(String request, IUser author, int index, Main main) {
		this.request = request;
		this.main = main;
		this.author = author;
		this.index = index;
	}
	
	public Request(String request, String authorID, int index, Main main) {
		this.request = request;
		this.main = main;
		this.authorID = authorID;
		this.index = index;
	}
	
	public Request(String request, String authorID, int index, Main main, boolean confirmed) {
		this(request, authorID, index, main);
		this.confirmed = confirmed;
	}
	
	public boolean hasAuthor() {
		return (author != null);
	}
	
	public boolean getAuthor() {
		author = main.client.getUserByID(authorID);
		return hasAuthor();
	}
	
	public void confirm() {
		confirmed = true;
		try {
			main.client.getOrCreatePMChannel(author).sendMessage("Your request \"" + request + "\" was confirmed and will be implemented!");
		} catch (RateLimitException | MissingPermissionsException | DiscordException e) {
			main.console.log(e.getMessage());
		}
	}
	
	public void close(String reason) {
		try {
			if (!confirmed) {
				if (reason == null || reason.equals(""))
					main.client.getOrCreatePMChannel(author).sendMessage("Your request \"" + request + "\" was denied and closed! If you have questions about this, ask Noktuska for help!");
				else
					main.client.getOrCreatePMChannel(author).sendMessage("Your request \"" + request + "\" was denied and closed! Reason: \"" + reason + "\"");
			} else {
				main.client.getOrCreatePMChannel(author).sendMessage("Your request \"" + request + "\" was implemented and will be available as soon as the new version goes online! Thank you for you help improving NoktuskaBot!");
			}
		} catch (RateLimitException | MissingPermissionsException | DiscordException e) {
			main.console.log(e.getMessage());
		}
	}
	
	@Override
	public String toString() {
		return ("Index: " + index + "\nAuthor: " + author.getName() + "\nRequest: " + request + "\n");
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public String getRequest() {
		return request;
	}
	
	public String getAuthorID() {
		if (author == null)
			return authorID;
		else if (authorID == null)
			return author.getID();
		return "0";
	}
	
	public boolean isConfirmed() {
		return confirmed;
	}

}
