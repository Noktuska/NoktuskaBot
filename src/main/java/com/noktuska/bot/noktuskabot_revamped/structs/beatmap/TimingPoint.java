package com.noktuska.bot.noktuskabot_revamped.structs.beatmap;

public class TimingPoint {
	
	private int time;
	private double msPerBeat;
	private boolean inherit;
	
	public TimingPoint(int time, double msPerBeat, boolean inherit) {
		this.time = time;
		this.msPerBeat = msPerBeat;
		this.inherit = inherit;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public double getMsPerBeat() {
		return msPerBeat;
	}

	public void setMsPerBeat(double msPerBeat) {
		this.msPerBeat = msPerBeat;
	}

	public boolean isInherit() {
		return inherit;
	}

	public void setInherit(boolean inherit) {
		this.inherit = inherit;
	}
	
}
