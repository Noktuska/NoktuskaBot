package com.noktuska.bot.noktuskabot_revamped.structs.beatmap;

import com.noktuska.bot.noktuskabot_revamped.structs.V2f;

public class HitObject {

	public enum Type {
		INVALID,
		CIRCLE,
		SPINNER,
		SLIDER
	}
	
	private V2f pos = new V2f(0);
	private int time = 0;
	private Type objectType = Type.INVALID;
	private int endTime = 0;
	private SliderData slider;
	
	public HitObject(V2f pos, int time, Type objectType, int endTime, SliderData slider) {
		super();
		this.pos = pos;
		this.time = time;
		this.objectType = objectType;
		this.endTime = endTime;
		this.slider = slider;
	}

	public V2f at(int ms) {
		if (objectType != Type.SLIDER)
			return pos;
		
		return SliderCalc.sliderAt(this, ms);
	}
	
	public V2f getPos() {
		return pos;
	}

	public void setPos(V2f pos) {
		this.pos = pos;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public Type getObjectType() {
		return objectType;
	}

	public void setObjectType(Type objectType) {
		this.objectType = objectType;
	}

	public int getEndTime() {
		return endTime;
	}

	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}

	public SliderData getSlider() {
		return slider;
	}

	public void setSlider(SliderData slider) {
		this.slider = slider;
	}

}
