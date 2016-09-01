package com.noktuska.bot.noktuskabot_revamped.structs.beatmap;

import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.api.ppcalculator.PpCalculator;
import com.noktuska.bot.noktuskabot_revamped.structs.V2f;
import com.noktuska.bot.noktuskabot_revamped.structs.beatmap.HitObject.Type;

public class Beatmap {

	private final float
			od0ms = 79.5f,
			od10ms = 19.5f,
			ar0ms = 1800f,
			ar5ms = 1200f,
			ar10ms = 450f;
	private final float
			odMsStep = 6f,
			arMsStep1 = 120f,
			arMsStep2 = 150f;
	
	private String artist;
	private String title;
	private String creator;
	private String version;
	private double stars = 1337;
	private double hp = 1337;
	private double cs = 1337;
	private double od = 1337;
	private double ar = 1337;
	private double sv = 1337;
	private double tickRate = 1;
	private double stackLeniency = 0;
	private int maxCombo = 0;
	private int formatVersion = 0;
	private int mode = 0;
	
	private int numCircles = 0;
	private int numSliders = 0;
	private int numSpinners = 0;
	
	public static final int MAX_TIMING_POINTS = 0xFFFF;
	private int numTimingPoints = 0;
	private List<TimingPoint> timingPoints = new ArrayList<TimingPoint>();
	
	public static final int MAX_OBJECTS = MAX_TIMING_POINTS;
	private int numObjects = 0;
	private List<HitObject> hitObjects = new ArrayList<HitObject>();
	
	public double aimValue;
	public double speedValue;
	public double accValue;
	public double ppValue;
	
	public Beatmap(String artist, String title, String creator, String version, double hp2, double cs2, double od2, double ar2, int maxCombo) {
		this.artist = artist;
		this.title = title;
		this.creator = creator;
		this.version = version;
		this.hp = hp2;
		this.cs = cs2;
		this.od = od2;
		this.ar = ar2;
		this.maxCombo = maxCombo;
	}
	
	public void parse(List<String> data) {
		String curSection = "";
		for (String line : data) {
			if (line.startsWith("[")) {
				curSection = line;
				continue;
			}
			
			if (line.equals(""))
				continue;
			
			if (curSection.equals("")) {
				if (line.contains("osu file format v")) {
					formatVersion = Integer.parseInt(line.substring("osu file format v".length()));
				}
			} else if (curSection.equals("[General]")) {
				if (line.startsWith("StackLeniency:")) {
					stackLeniency = Double.parseDouble(line.substring("StackLeniency:".length()).trim());
				}
				if (line.startsWith("Mode:")) {
					mode = Integer.parseInt(line.substring("Mode:".length()).trim());
				}
			} else if (curSection.equals("[Difficulty]")) {
				if (line.startsWith("SliderMultiplier:")) {
					sv = Double.parseDouble(line.substring("SliderMultiplier:".length()).trim());
				}
				if (line.startsWith("SliderTickRate:")) {
					tickRate = Double.parseDouble(line.substring("SliderTickRate:".length()).trim());
				}
			} else if (curSection.equals("[TimingPoints]")) {
				String[] args = line.split(",");
				int time = Integer.parseInt(args[0]);
				double msPerBeat = Double.parseDouble(args[1]);
				boolean inherit = false;
				if (args.length > 6)
					inherit = !(args[6].equals("1"));
				timingPoints.add(new TimingPoint(time, msPerBeat, inherit));
				numTimingPoints++;
			} else if (curSection.equals("[HitObjects]")) {
				String[] args = line.split(",");
				int x = Integer.parseInt(args[0]);
				int y = Integer.parseInt(args[1]);
				int time = Integer.parseInt(args[2]);
				int endTime = time;
				int type = Integer.parseInt(args[3]);
				Type objectType = Type.CIRCLE;
				char sliderType = 'B';
				List<V2f> pointList = null;
				int repetitions = 1;
				double length = 0;
				if ((type & 2) > 0) {
					pointList = new ArrayList<V2f>();
					pointList.add(new V2f(x, y));
					sliderType = args[5].charAt(0);
					String[] points = args[5].substring(2).split("\\|");
					for (String s : points) {
						int px = Integer.parseInt(s.split(":")[0]);
						int py = Integer.parseInt(s.split(":")[1]);
						pointList.add(new V2f(px, py));
					}
					repetitions = Integer.parseInt(args[6]);
					length = Double.parseDouble(args[7]);
					objectType = Type.SLIDER;
					TimingPoint tp = timing(time);
					TimingPoint parent = parentTiming(tp);
					
					double svMultiplier = 1;
					if (tp.isInherit() && tp.getMsPerBeat() < 0) {
						svMultiplier = (-100.0 / tp.getMsPerBeat());
					}
					
					double pxPerBeat = sv * 100.0 * svMultiplier;
					double numBeats = (length * repetitions) / pxPerBeat;
					int duration = (int)Math.ceil(numBeats * parent.getMsPerBeat());
					endTime = time + duration;
					numSliders++;
				} else if ((type & 8) > 0) {
					endTime = Integer.parseInt(args[5]);
					objectType = Type.SPINNER;
					numSpinners++;
				} else {
					numCircles++;
				}
				hitObjects.add(new HitObject(new V2f(x, y), time, objectType, endTime, new SliderData(sliderType, pointList, new ArrayList<V2f>(), repetitions, length)));
				numObjects++;
			}
		}
	}
	
	private TimingPoint timing(int time) {
		for (int i = numTimingPoints - 1; i >= 0; i--) {
			TimingPoint cur = timingPoints.get(i);
			
			if (cur.getTime() <= time) {
				return cur;
			}
		}
		return timingPoints.get(0);
	}
	
	private TimingPoint parentTiming(TimingPoint t) {
		if (!t.isInherit())
			return t;
		
		for (int i = numTimingPoints - 1; i >= 0; i--) {
			TimingPoint cur = timingPoints.get(i);
			
			if (cur.getTime() <= t.getTime() && !cur.isInherit()) {
				return cur;
			}
		}
		
		return null;
	}
	
	public void applyMods(long mods) {
		if ((mods & PpCalculator.mapChanging) == 0)
			return;
		
		//Speed
		float speed = 1f;
		if ((mods & PpCalculator.dt) != 0 || (mods & PpCalculator.nc) != 0)
			speed *= 1.5f;
		if ((mods & PpCalculator.ht) != 0)
			speed *= 0.75f;
		
		//Overall Difficulty
		float odMultiplier = 1f;
		if ((mods & PpCalculator.hr) != 0)
			odMultiplier = 1.4f;
		if ((mods & PpCalculator.ez) != 0)
			odMultiplier = 0.5f;
		
		od *= odMultiplier;
		double odms = od0ms - odMsStep * od;
		
		//Approach Rate
		float arMultiplier = 1f;
		if ((mods & PpCalculator.hr) != 0)
			arMultiplier = 1.4f;
		if ((mods & PpCalculator.ez) != 0)
			arMultiplier = 0.5f;
		
		ar *= arMultiplier;
		double arms = ar <= 5 ? (ar0ms - arMsStep1 * ar) : (ar5ms - arMsStep2 * (ar - 5));
		
		//Circle Size
		float csMultiplier = 1f;
		if ((mods & PpCalculator.hr) != 0)
			csMultiplier = 1.3f;
		if ((mods & PpCalculator.ez) != 0)
			csMultiplier = 0.5f;
		
		//Stats must be capped to 0-10 before HT/DT
		odms = Math.min(od0ms, Math.max(od10ms, odms));
		arms = Math.min(ar0ms, Math.max(ar10ms, arms));
		
		//Apply speed change
		odms /= speed;
		arms /= speed;
		
		//Convert OD and AR back into their stat form
		od = (od0ms - odms) / odMsStep;
		ar = ar <= 5f ? ((ar0ms - arms) / arMsStep1) : (5f + (ar5ms - arms) / arMsStep2);
		
		cs *= csMultiplier;
		cs = Math.max(0f, Math.min(10f, cs));
		
		if ((mods & PpCalculator.speedChanging) == 0) {
			return;
		}
		
		for (int i = 0; i < numTimingPoints; i++) {
			TimingPoint tp = timingPoints.get(i);
			tp.setTime((int)(tp.getTime() / speed));
			if (!tp.isInherit()) {
				tp.setMsPerBeat(tp.getMsPerBeat() / speed);
			}
		}
		
		for (int i = 0; i < numObjects; i++) {
			HitObject o = hitObjects.get(i);
			o.setTime((int)(o.getTime() / speed));
			o.setEndTime((int)(o.getEndTime() / speed));
		}
	}
	
	public void setStars(double stars) {
		this.stars = stars;
	}
	
	public String getArtist() {
		return artist;
	}

	public String getTitle() {
		return title;
	}

	public String getCreator() {
		return creator;
	}

	public String getVersion() {
		return version;
	}

	public double getHp() {
		return hp;
	}

	public double getCs() {
		return cs;
	}

	public double getOd() {
		return od;
	}

	public double getAr() {
		return ar;
	}

	public double getSv() {
		return sv;
	}

	public double getTickRate() {
		return tickRate;
	}

	public double getStackLeniency() {
		return stackLeniency;
	}

	public int getMaxCombo() {
		return maxCombo;
	}

	public int getFormatVersion() {
		return formatVersion;
	}

	public int getMode() {
		return mode;
	}

	public int getNumCircles() {
		return numCircles;
	}

	public int getNumSliders() {
		return numSliders;
	}

	public int getNumSpinners() {
		return numSpinners;
	}

	public int getNumTimingPoints() {
		return numTimingPoints;
	}

	public List<TimingPoint> getTimingPoints() {
		return timingPoints;
	}

	public int getNumObjects() {
		return numObjects;
	}

	public List<HitObject> getHitObjects() {
		return hitObjects;
	}
	
	public double getStars() {
		return stars;
	}
}