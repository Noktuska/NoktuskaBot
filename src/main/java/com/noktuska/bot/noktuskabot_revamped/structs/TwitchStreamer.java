package com.noktuska.bot.noktuskabot_revamped.structs;

public class TwitchStreamer {

	private String name;
	private boolean wasOnline = false;
	
	public TwitchStreamer(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isWasOnline() {
		return wasOnline;
	}

	public void setWasOnline(boolean wasOnline) {
		this.wasOnline = wasOnline;
	}
	
}
