package com.noktuska.bot.noktuskabot_revamped.structs.curves;

import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.structs.V2f;

public class Catmull extends Curve {

	@Override
	public V2f at(double t) {
		t = Math.min(1.0, Math.max(0.0, t));
		t *= points.size();
		int i = (int)t;
		return computeSinglePoint(i, t);
	}
	
	private V2f computeSinglePoint(int i, double t) {
		t = Math.min(1.0, Math.max(0.0, t));
		
		V2f v1 = (i >= 1 ? points.get(i - 1) : points.get(i));
		V2f v2 = points.get(i);
		V2f v3 = i + 1 < points.size() ? points.get(i + 1) : new V2f(v2.x * 2.0 - v1.x, v2.y * 2.0 - v1.y);
		V2f v4 = i + 2 < points.size() ? points.get(i + 2) : new V2f(v3.x * 2.0 - v2.x, v3.y * 2.0 - v2.y);
		V2f pt = new V2f(0);
		pt.x += (v1.x * (-1.0) + v2.x * 3.0 - v3.x * 3.0 + v4.x) * t * t * t;
		pt.y += (v1.y * (-1.0) + v2.y * 3.0 - v3.y * 3.0 + v4.y) * t * t * t;
		pt.x += (v1.x * 2.0 - v2.x * 5.0 + v3.x * 4.0 - v4.x) * t * t;
		pt.y += (v1.y * 2.0 - v2.y * 5.0 + v3.y * 4.0 - v4.y) * t * t;
		pt.x += (v1.x * (-1.0) + v3.x) * t;
		pt.y += (v1.y * (-1.0) + v3.y) * t;
		pt.x += v2.x * 2.0;
		pt.y += v2.y * 2.0;
		pt.x /= 2.0;
		pt.y /= 2.0;
		
		return pt;
	}
	
	@Override
	public void compute(List<V2f> dst) {
		for (int i = 0; i < points.size() - 1; i++) {
			for (double t = 0; t < 1.0 + 0.0005; t += 0.0005) {
				dst.add(computeSinglePoint(i, t));
			}
		}
	}

}
