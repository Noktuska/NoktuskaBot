package com.noktuska.bot.noktuskabot_revamped.structs.beatmap;

import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.structs.V2f;

public class SliderData {

	public char type;
	public List<V2f> points = new ArrayList<V2f>();
	public List<V2f> posAtMs = new ArrayList<V2f>();
	public int repetitions = 0;
	public double length = 0;
	
	public SliderData(char type, List<V2f> points, List<V2f> posAtMs, int repetitions, double length) {
		super();
		this.type = type;
		this.points = points;
		this.posAtMs = posAtMs;
		this.repetitions = repetitions;
		this.length = length;
	}
}
