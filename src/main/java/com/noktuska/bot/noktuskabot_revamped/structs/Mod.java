package com.noktuska.bot.noktuskabot_revamped.structs;

import java.util.ArrayList;
import java.util.List;

public class Mod {
	public String representation;
	public long bit;
	
	public Mod(String representation, long bit) {
		this.representation = representation;
		this.bit = bit;
	}
	
	public static List<Mod> generateMods() {
		List<Mod> result = new ArrayList<Mod>();
		result.add(new Mod("NF", 1 << 0));
		result.add(new Mod("EZ", 1 << 1));
		result.add(new Mod("NoVideo", 1 << 2));
		result.add(new Mod("HD", 1 << 3));
		result.add(new Mod("HR", 1 << 4));
		result.add(new Mod("SD", 1 << 5));
		result.add(new Mod("DT", 1 << 6));
		result.add(new Mod("RX", 1 << 7));
		result.add(new Mod("HT", 1 << 8));
		result.add(new Mod("NC", 1 << 9));
		result.add(new Mod("FL", 1 << 10));
		result.add(new Mod("AUTO", 1 << 11));
		result.add(new Mod("SO", 1 << 12));
		result.add(new Mod("AP", 1 << 13));
		result.add(new Mod("PF", 1 << 14));
		result.add(new Mod("K4", 1 << 15));
		result.add(new Mod("K5", 1 << 16));
		result.add(new Mod("K6", 1 << 17));
		result.add(new Mod("K7", 1 << 18));
		result.add(new Mod("K8", 1 << 19));
		result.add(new Mod("FadeIn", 1 << 20));
		result.add(new Mod("Random", 1 << 21));
		result.add(new Mod("LastMod", 1 << 22));
		result.add(new Mod("K9", 1 << 24));
		result.add(new Mod("K10", 1 << 25));
		result.add(new Mod("K1", 1 << 26));
		result.add(new Mod("K3", 1 << 27));
		result.add(new Mod("K2", 1 << 28));
		return result;
	}
}
