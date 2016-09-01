package com.noktuska.bot.noktuskabot_revamped.structs;

public class V2f {

	public double x = 0;
	public double y = 0;
	
	public V2f() {}
	public V2f(double x, double y) {
		this.x = x;
		this.y = y;
	}
	public V2f(double v) {
		this(v, v);
	}
	public V2f(V2f pos) {
		this.x = pos.x;
		this.y = pos.y;
	}
	
	public double len() {
		return Math.sqrt(x * x + y * y);
	}
	
	public V2f norm() {
		double l = len();
		x /= l;
		y /= l;
		return this;
	}
	
	@Override
	public String toString() {
		return "(" + x + " " + y + ")";
	}

}
