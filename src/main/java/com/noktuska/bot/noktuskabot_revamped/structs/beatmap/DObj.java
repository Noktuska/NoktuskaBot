package com.noktuska.bot.noktuskabot_revamped.structs.beatmap;

import com.noktuska.bot.noktuskabot_revamped.structs.V2f;
import com.noktuska.bot.noktuskabot_revamped.structs.beatmap.HitObject.Type;

public class DObj {
	
	public static final double[] DECAY_BASE = { 0.3, 0.15 };
	public static final double ALMOST_DIAMETER = 90;
	public static final double STREAM_SPACING = 110;
	public static final double SINGLE_SPACING = 125;
	public static final double[] WEIGHT_SCALING = { 1400, 26.26 };
	public static final int SLIDER_STEP = 10;
	public static final double CIRCLESIZE_BUFF_TRESHOLD = 30;
	public static final int SPEED = 0;
	public static final int AIM = 1;
	
	private double[] strains = { 1, 1 };
	private HitObject ho;
	
	private V2f normStart;
	private V2f normEnd;
	
	private double lazyLen1st = 0;
	private double lazyLenRest = 0;
	
	public void init(HitObject baseObject, double radius) {
		this.ho = baseObject;
		
		double scalingFactor = 52.0 / radius;
		
		if (radius < CIRCLESIZE_BUFF_TRESHOLD) {
			scalingFactor *= Math.min(1.1, 1 + (CIRCLESIZE_BUFF_TRESHOLD - radius) * 0.02);
		}
		
		normStart = new V2f(ho.getPos().x * scalingFactor, ho.getPos().y * scalingFactor);
		
		if (ho.getObjectType() != Type.SLIDER) {
			normEnd = new V2f(normStart);
			return;
		}
		
		SliderData sl = ho.getSlider();
		int repetitionLen = (ho.getEndTime() - ho.getTime()) / sl.repetitions;
		
		V2f cursor = new V2f(ho.getPos());
		double followCirlceRad = radius * 3;
		
		for (int t = SLIDER_STEP; t < repetitionLen; t += SLIDER_STEP) {
			V2f p = ho.at(t);
			
			V2f d = new V2f(p.x - cursor.x, p.y - cursor.y);
			double dist = d.len();
			
			if (dist <= followCirlceRad)
				continue;
			
			d = d.norm();
			dist -= followCirlceRad;
			cursor.x += d.x * dist;
			cursor.y += d.y * dist;
			lazyLen1st += dist;
		}
		
		lazyLen1st *= scalingFactor;
		
		lazyLen1st = 0.0;
		
		if (sl.repetitions % 2 == 1) {
			normEnd = new V2f(cursor.x * scalingFactor, cursor.y * scalingFactor);
		}
		
		if (sl.repetitions < 2)
			return;
		
		for (int t = repetitionLen + SLIDER_STEP; t < repetitionLen * 2; t += SLIDER_STEP) {
			V2f p = ho.at(t);
			
			V2f d = new V2f(p.x - cursor.x, p.y - cursor.y);
			double dist = d.len();
			
			if (dist <= followCirlceRad)
				continue;
			
			d = d.norm();
			dist -= followCirlceRad;
			cursor.x += d.x * dist;
			cursor.y += d.y * dist;
			lazyLenRest += dist;
		}
		
		lazyLenRest *= scalingFactor;
		
		lazyLenRest = 0.0;
		
		normEnd = new V2f(cursor.x * scalingFactor, cursor.y * scalingFactor);
	}
	
	public void calculateStrains(DObj prev) {
		calculateStrain(prev, SPEED);
		calculateStrain(prev, AIM);
	}
	
	private void calculateStrain(DObj prev, int type) {
		double res = 0.0;
		int timeElapsed = ho.getTime() - prev.ho.getTime();
		double decay = Math.pow(DECAY_BASE[type], timeElapsed / 1000.0);
		double scaling = WEIGHT_SCALING[type];
		
		switch (ho.getObjectType()) {
		case CIRCLE:
			res = spacingWeight(distance(prev), type) * scaling;
			break;
		case SLIDER:
			int reps = prev.ho.getSlider().repetitions - 1;
			
			switch (type) {
			case SPEED:
				res = spacingWeight(prev.lazyLen1st + prev.lazyLenRest * reps + distance(prev), type) * scaling;
				break;
			case AIM:
				res = (spacingWeight(prev.lazyLen1st, type) + spacingWeight(prev.lazyLenRest, type) * reps + spacingWeight(distance(prev), type)) * scaling;
				break;
			}
			break;
		case SPINNER:
		case INVALID:
		default:
			break;
		}
		
		res /= Math.max(timeElapsed, (int)50);
		strains[type] = prev.strains[type] * decay + res;
	}
	
	private double spacingWeight(double distance, int diffType) {
		switch (diffType) {
		case SPEED:
			if (distance > SINGLE_SPACING) {
				return 2.5;
			} else if (distance > STREAM_SPACING) {
				return 1.6 + 0.9 * (distance - STREAM_SPACING) / (SINGLE_SPACING - STREAM_SPACING);
			} else if (distance > ALMOST_DIAMETER) {
				return 1.2 + 0.4 * (distance - ALMOST_DIAMETER) / (STREAM_SPACING - ALMOST_DIAMETER);
			} else if (distance > ALMOST_DIAMETER / 2.0) {
				return 0.95 + 0.25 * (distance - ALMOST_DIAMETER / 2.0) / (ALMOST_DIAMETER / 2.0);
			}
			return 0.95;
		case AIM:
			return Math.pow(distance, 0.99);
		default:
			return 0.0;
		}
	}
	
	private double distance(DObj prev) {
		return new V2f(normStart.x - prev.normEnd.x, normStart.y - prev.normEnd.y).len();
	}
	
	public HitObject getHo() {
		return ho;
	}
	
	public double[] getStrains() {
		return strains;
	}
	
}
