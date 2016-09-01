package com.noktuska.bot.noktuskabot_revamped.structs.curves;

import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.structs.V2f;

public class CircularArc extends Curve {

	private double len;
	private Circle c;
	
	public CircularArc() {
	}

	public void init(List<V2f> pts, double length) {
		super.init(pts, 3);
		len = length;
		
		V2f p1 = pts.get(0);
		V2f p2 = pts.get(1);
		V2f p3 = pts.get(2);
		
		double D = 2.0 * (p1.x * (p2.y - p3.y) + p2.x * (p3.y - p1.y) + p3.x * (p1.y - p2.y));
		
		D += 0.00001;
		
		double Ux = ((p1.x * p1.x + p1.y * p1.y) *
				(p2.y - p3.y) + (p2.x * p2.x + p2.y * p2.y) *
				(p3.y - p1.y) + (p3.x * p3.x + p3.y * p3.y) * (p1.y - p2.y)) / D;
		
		double Uy = ((p1.x * p1.x + p1.y * p1.y) *
				(p3.x - p2.x) + (p2.x * p2.x + p2.y * p2.y) *
				(p1.x - p3.x) + (p3.x * p3.x + p3.y * p3.y) * (p2.x - p1.x)) / D;
		
		V2f radius = new V2f(Ux - p1.x, Uy - p1.y);
		
		c = new Circle(new V2f(Ux, Uy), radius.len());
	}
	
	private boolean isCounterClockwise(V2f a, V2f b, V2f c) {
		V2f diff1 = new V2f(b.x - a.x, b.y - a.y);
		V2f diff2 = new V2f(c.x - a.x, c.y - a.y);
		return diff1.y * diff2.x > diff1.x * diff2.y;
	}
	
	private V2f rotate(V2f c, V2f p, double radians) {
		double _cos = Math.cos(radians);
		double _sin = Math.sin(radians);
		V2f d = new V2f(p.x - c.x, p.y - c.y);
		return new V2f(_cos * d.x - _sin * d.y + c.x, _sin * d.x + _cos * d.y + c.y);
	}
	
	@Override
	public V2f at(double t) {
		t = Math.min(1.0, Math.max(0.0, t));
		
		double radians = (t * len) / c.r;
		if (isCounterClockwise(points.get(0), points.get(1), points.get(2))) {
			radians *= -1;
		}
		
		return rotate(c.c, points.get(0), radians);
	}

}
