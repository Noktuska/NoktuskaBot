package com.noktuska.bot.noktuskabot_revamped.structs;

public class OsuPlayer {

	private String name;
	private double[] pp;
	private int ppBorder = -1;
	
	public OsuPlayer(String name, double[] pp, int ppBorder) {
		this.name = name;
		this.pp = pp;
		this.ppBorder = ppBorder;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double[] getPp() {
		return pp;
	}
	
	public int getPpBorder() {
		return ppBorder;
	}

	public void setPp(double[] pp) {
		this.pp = pp;
	}
	
	public void setPp(double pp, int index) {
		this.pp[index] = pp;
	}
	
	public void setPpBorder(int ppBorder) {
		this.ppBorder = ppBorder;
	}

}
