package com.noktuska.bot.noktuskabot_revamped.structs.beatmap;

import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.structs.V2f;
import com.noktuska.bot.noktuskabot_revamped.structs.curves.Bezier;
import com.noktuska.bot.noktuskabot_revamped.structs.curves.Catmull;
import com.noktuska.bot.noktuskabot_revamped.structs.curves.CircularArc;
import com.noktuska.bot.noktuskabot_revamped.structs.curves.Curve;

public class SliderCalc {

	public static V2f sliderAt(HitObject ho, int ms) {
		int duration = ho.getEndTime() - ho.getTime();
		SliderData sl = ho.getSlider();
		
		ms = Math.max(0, Math.min(ms, duration));
		float t = (float)ms / duration;
		
		int oneRepetition = duration / sl.repetitions;
		boolean invert = false;
		while (ms > oneRepetition) {
			ms -= oneRepetition;
			invert ^= true;
		}
		if (invert) {
			ms = oneRepetition - ms;
		}
		
		do {
			switch (sl.type) {
			case 'L':
				if (sl.points.size() < 2) {
					return new V2f(0);
				}
				if (sl.points.size() > 2) {
					List<V2f> tmp = new ArrayList<V2f>(sl.points);
					
					sl.points.clear();
					for (int i = 0; i < tmp.size() * 2; i++) {
						sl.points.add(tmp.get(i / 2));
					}
					
					sl.points.remove(sl.points.size() - 1);
					sl.type = 'B';
					continue;
				}
				return ptOnLine(sl.points.get(0), sl.points.get(1), t);
			case 'P':
				if (sl.points.size() < 2) {
					return new V2f(0);
				}
				if (sl.points.size() != 3) {
					sl.type = 'B';
					continue;
				}
			case 'C':
			case 'B':
				if (sl.points.size() < 2) {
					return new V2f(0);
				}
				if (sl.posAtMs.size() > 0) {
					return sl.posAtMs.get(ms);
				}
				
				double pxPerMs = (sl.length * sl.repetitions) / (double)duration;
				List<V2f> positions = new ArrayList<V2f>();
				
				if (sl.type != 'B') {
					Curve c = null;
					CircularArc arc;
					Catmull cat;
					
					switch (sl.type) {
					case 'C':
						cat = new Catmull();
						cat.init(sl.points, sl.points.size());
						c = cat;
						break;
					case 'P':
						arc = new CircularArc();
						arc.init(sl.points, sl.length);
						c = arc;
						break;
					}
					
					c.compute(positions);
					precomputeSlider(sl, positions, pxPerMs);
					positions.clear();
				} else {
					Bezier bez = new Bezier();
					int lastSegment = 0;
					
					for (int j = 0; j < sl.points.size(); j++) {
						if (j == 0)
							continue;
						
						boolean last = (j == sl.points.size() - 1);
						
						V2f tmp = new V2f(sl.points.get(j).x - sl.points.get(j - 1).x, sl.points.get(j).y - sl.points.get(j - 1).y);
						if (tmp.len() > 0.0001f && !last)
							continue;
						
						if (j == 1 && !last) {
							lastSegment = 1;
							continue;
						}
						
						if (last) {
							j++;
						}
						
						bez.init(sl.points.subList(lastSegment, sl.points.size()), j - lastSegment);
						lastSegment = j;
						
						bez.compute(positions);
						precomputeSlider(sl, positions, pxPerMs);
					}
				}
			}
			
			while (sl.posAtMs.size() < (int)duration)
				sl.posAtMs.add(sl.posAtMs.get(sl.posAtMs.size() - 1));
			
			return sl.posAtMs.get((int)ms);
		} while (false);
		
		return new V2f(0);
	}
	
	private static void precomputeSlider(SliderData sl, List<V2f> positions, double pxPerMs) {
		double totalDistance = 0;
		
		for (int k = 1; k < positions.size(); k++) {
			totalDistance += new V2f(positions.get(k).x - positions.get(k - 1).x, positions.get(k).y - positions.get(k - 1).y).len();
			if (totalDistance >= pxPerMs) {
				sl.posAtMs.add(positions.get(k));
				totalDistance -= pxPerMs;
			}
		}
	}
	
	private static V2f ptOnLine(V2f p1, V2f p2, double t) {
		t = Math.min(1.0, Math.max(0.0, t));
		double len = new V2f(p2.x - p1.x, p2.y - p1.y).len();
		double n = len - len * t;
		return new V2f((p1.x * n + p2.x * len * t) / len, (p1.y * n + p2.y * len * t) / len);
	}

}
