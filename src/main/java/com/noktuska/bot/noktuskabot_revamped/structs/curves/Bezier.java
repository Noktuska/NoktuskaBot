package com.noktuska.bot.noktuskabot_revamped.structs.curves;

import com.noktuska.bot.noktuskabot_revamped.structs.V2f;

public class Bezier extends Curve {

	@Override
	public V2f at(double t) {
		V2f res = new V2f(0);
		
		t = Math.min(1.0, Math.max(0.0, t));
		int n = points.size() - 1;
		for (int i = 0; i < points.size(); i++) {
			double multiplier = binominialCoeddicient(i, n) * Math.pow(1.0 - t, n - i) * Math.pow(t, i);
			res.x += points.get(i).x * multiplier;
			res.y += points.get(i).y * multiplier;
		}
		
		return res;
	}
	
	private int binominialCoeddicient(int p, int n) {
		if (p < 0 || p > n) {
			return 0;
		}
		
		p = Math.min(p, n - p);
		int out = 1;
		for (int i = 1; i < p + 1; i++) {
			out = out * (n - p + i) / i;
		}
		
		return out;
	}

}
