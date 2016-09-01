package com.noktuska.bot.noktuskabot_revamped.structs.curves;

import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.structs.V2f;

public abstract class Curve {
	
	protected List<V2f> points = new ArrayList<V2f>();
	
	public void init(List<V2f> pts, int npts) {
		this.points.clear();
		this.points.addAll(pts);
	}
	
	public abstract V2f at(double t);
	
	public void compute(List<V2f> dst) {
		double step = 0.0005 / points.size();
		for (double d = 0.0; d < 1.0 + step; d += step) {
			dst.add(at(d));
		}
	}
	
}