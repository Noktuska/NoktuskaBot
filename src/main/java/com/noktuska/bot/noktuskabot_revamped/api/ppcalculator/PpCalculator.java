package com.noktuska.bot.noktuskabot_revamped.api.ppcalculator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.Main;
import com.noktuska.bot.noktuskabot_revamped.api.OsuAPI;
import com.noktuska.bot.noktuskabot_revamped.structs.Server;
import com.noktuska.bot.noktuskabot_revamped.structs.beatmap.Beatmap;
import com.noktuska.bot.noktuskabot_revamped.structs.beatmap.DObj;
import com.noktuska.bot.noktuskabot_revamped.utils.Func;

public class PpCalculator {

	public static final long
			nomod = 0,
			nf = 1 << 0,
			ez = 1 << 1,
			hd = 1 << 3,
			hr = 1 << 4,
			dt = 1 << 6,
			ht = 1 << 8,
			nc = 1 << 9,
			fl = 1 << 10,
			so = 1 << 12,
			speedChanging = dt | ht | nc,
			mapChanging = hr | ez | speedChanging;
	
	private static DObj[] objects;
	
	private static long getModValue(String mod) {
		switch (mod.toLowerCase()) {
		case "nf": return nf;
		case "ez": return ez;
		case "hd": return hd;
		case "hr": return hr;
		case "dt": return dt;
		case "ht": return ht;
		case "nc": return nc;
		case "fl": return fl;
		case "so": return so;
		case "nomod":
		default: return nomod;
		}
	}
	
	private static int numObjects = 0;
	
	public static Beatmap calcPp(Main main, String[] args, Server server) throws Exception {
		
		Beatmap result;
		
		if (args.length < 1) {
			throw new Exception("Invalid number of arguments");
		}
		
		String beatmapId = Func.getBeatmapId(args[0]);
		if (beatmapId == null)
			throw new Exception("Couldn't get a valid beatmap ID");
		
		OsuAPI beatmap = new OsuAPI("get_beatmaps", "b=" + beatmapId + "&a=1", null);
		if (!beatmap.isValid())
			throw new Exception("Beatmap has invalid data");
		
		double stars = Double.parseDouble((String)beatmap.getObject("difficultyrating"));
		
		String artist = (String)beatmap.getObject("artist");
		String title = (String)beatmap.getObject("title");
		String version = (String)beatmap.getObject("version");
		String creator = (String)beatmap.getObject("creator");
		
		double hp = Double.parseDouble((String)beatmap.getObject("diff_drain"));
		double cs = Double.parseDouble((String)beatmap.getObject("diff_size"));
		double od = Double.parseDouble((String)beatmap.getObject("diff_overall"));
		double ar = Double.parseDouble((String)beatmap.getObject("diff_approach"));
		
		double acc = 100.0f;
		long mods = nomod;
		String modstring = "";
		int combo = Integer.parseInt((String)beatmap.getObject("max_combo"));
		int misses = 0;
		boolean v1 = true;
		
		result = new Beatmap(artist, title, creator, version, hp, cs, od, ar, combo);
		result.setStars(stars);
		
		for (String s : args) {
			if (s.endsWith("%")) {
				acc = Double.parseDouble(s.substring(0, s.length() - 1));
			} else if (s.endsWith("x")) {
				combo = Integer.parseInt(s.substring(0, s.length() - 1));
			} else if (s.endsWith("m")) {
				misses = Integer.parseInt(s.substring(0, s.length() - 1));
			} else if (s.startsWith("scorev")) {
				v1 = (s.charAt(s.length() - 1) == 1);
			} else if (s.startsWith("+")) {
				modstring = s.substring(1);
				String[] modStringSplits = Func.splitString(modstring, 2);
				for (String mod : modStringSplits) {
					mods |= getModValue(mod);
				}
			}
		}
		
		//Download beatmap for deeper analysis
		String folder = server.downloadList.createRequest(args[0], server);
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(folder + File.separator + artist + " - " + title + " (" + creator + ") [" + version + "].osu"));
			
			List<String> data = new ArrayList<String>();
			String line = "";
			while ((line = br.readLine()) != null) {
				data.add(line);
			}
			
			br.close();
			
			result.parse(data);
		} catch (Exception e) {
			main.console.log(e.getMessage());
			throw new Exception("Error while reading through the beatmap");
		}
		
		result.applyMods(mods);
		
		final float playfieldWidth = 512f;
		
		double aim, speed;
		
		//Aim and speed calc
		if (result.getMode() != 0)
			throw new Exception("Currently only standard is supported!");
		
		double circleRadius = (playfieldWidth / 16f) * (1f - 0.7f * (result.getCs() - 5f) / 5f);
		
		numObjects = result.getNumObjects();
		
		objects = new DObj[result.getNumObjects()];
		for (int i = 0; i < result.getNumObjects(); i++) {
			objects[i] = new DObj();
			objects[i].init(result.getHitObjects().get(i), circleRadius);
		}
		
		DObj prev = objects[0];
		for (int i = 1; i < result.getNumObjects(); i++) {
			DObj o = objects[i];
			
			o.calculateStrains(prev);
			
			//V2f endpos = o.ho.at(o.ho.getEndTime() - o.ho.getTime());
			
			prev = o;
		}
		
		aim = calculateDifficulty(DObj.AIM);
		speed = calculateDifficulty(DObj.SPEED);
		aim = Math.sqrt(aim) * 0.0675;
		speed = Math.sqrt(speed) * 0.0675;
		
		aim = Math.round(aim * 100.0) / 100.0;
		speed = Math.round(speed * 100.0) / 100.0;
		result.aimValue = aim;
		result.speedValue = speed;
		
		stars = aim + speed + Math.abs(speed - aim) * 0.5;
		stars = Math.round(stars * 100.0) / 100.0;
		result.setStars(stars);
		
		acc = Math.max(0.0, Math.min(100.0, acc));
		int c300 = result.getNumObjects() - misses, c100 = 0;
		
		double epsilon = Func.getAccuracy(0, 0, c300, 0, 0, misses, 0) / 100.0 - Func.getAccuracy(0, 0, c300 - 1, 1, 0, misses, 0) / 100.0;
		
		epsilon *= 50.0;
		
		double closestAcc;
		while ((closestAcc = Math.abs(Func.getAccuracy(0, 0, c300, c100, 0, misses, 0)) - acc) >= epsilon && closestAcc < acc) {
			c300--;
			c100++;
		}
		
		if (c300 == 0xFFFF)
			c300 = result.getNumObjects() - c100 - misses;
		if (combo == 0xFFFF)
			combo = result.getMaxCombo();
		
		if (result.getMaxCombo() <= 0)
			throw new Exception("Seems like the map hasn't got any objects?");
		
		int totalHits = c300 + c100 + misses;
		if (totalHits != result.getNumObjects())
			throw new Exception("Maximum combo differs from calculated combo. Map seems to be corrupted!");
		
		double newAcc = Func.getAccuracy(0, 0, c300, c100, 0, misses, 0) / 100.0;
		double aimValue = baseStrain(aim);
		
		double totalHitsOver2k = (double)totalHits / 2000.0;
		double lengthBonus = 0.95 + 0.4 * Math.min(1.0, totalHitsOver2k) + (totalHits > 2000 ? Math.log10(totalHitsOver2k) * 0.5 : 0.0);
		double missPenalty = Math.pow(0.97, misses);
		double comboBreak = Math.pow((double)combo, 0.8) / Math.pow((double)result.getMaxCombo(), 0.8);
		
		aimValue *= lengthBonus;
		aimValue *= missPenalty;
		aimValue *= comboBreak;
		
		double arBonus = 1.0;
		if (result.getAr() > 10.33) {
			arBonus += 0.45 * (result.getAr() - 10.33);
		} else if (result.getAr() < 8) {
			double lowArBonus = 0.01 * (8.0 - result.getAr());
			if ((mods & hd) > 0) {
				lowArBonus *= 2.0;
			}
			arBonus += lowArBonus;
		}
		
		aimValue *= arBonus;
		
		if ((mods & hd) > 0)
			aimValue *= 1.18;
		if ((mods & fl) > 0)
			aimValue *= 1.45 * lengthBonus;
		
		double accBonus = 0.5 + newAcc / 2.0;
		double odBonus = 0.98 + Math.pow(result.getOd(), 2) / 2500.0;
		
		aimValue *= accBonus;
		aimValue *= odBonus;
		
		//Speed value
		double speedValue = baseStrain(speed);
		
		speedValue *= lengthBonus;
		speedValue *= missPenalty;
		speedValue *= comboBreak;
		speedValue *= accBonus;
		speedValue *= odBonus;
		
		double realAcc = 0.0;
		
		int circles = result.getNumCircles();
		if (!v1) {
			circles = totalHits;
			realAcc = newAcc;
		} else {
			if (circles > 0) {
				realAcc = ((c300 - (totalHits - circles)) * 300.0 + c100 * 100.0) / (circles * 300);
			}
			realAcc = Math.max(0.0, realAcc);
		}
		
		double accValue = Math.pow(1.52163, result.getOd()) * Math.pow(realAcc, 24.0) * 2.83;
		accValue *= Math.min(1.15, Math.pow(circles / 1000.0, 0.3));
		
		if ((mods & hd) > 0)
			accValue *= 1.02;
		if ((mods & fl) > 0)
			accValue *= 1.02;
		
		result.accValue = accValue;
		
		double finalMultiplier = 1.12;
		
		if ((mods & nf) > 0)
			finalMultiplier *= 0.9;
		if ((mods & so) > 0)
			finalMultiplier *= 0.95;
		
		double pp = Math.pow(Math.pow(aimValue, 1.1) + Math.pow(speedValue, 1.1) + Math.pow(accValue, 1.1), 1.0 / 1.1) * finalMultiplier;
		
		result.ppValue = pp;
		
		return result;
	}
	
	private static double baseStrain(double strain) {
		return Math.pow(5.0 * Math.max(1.0, strain / 0.0675) - 4.0, 3.0) / 100000.0;
	}
	
	private static final int STRAIN_STEP = 400;
	private static final double DECAY_WEIGHT = 0.9;
	
	private static double calculateDifficulty(int type) {
		List<Double> highestStrains = new ArrayList<Double>();
		int intervalEnd = STRAIN_STEP;
		double maxStrain = 0.0;
		
		DObj prev = null;
		for (int i = 0; i < numObjects; i++) {
			DObj o = objects[i];
			
			while (o.getHo().getTime() > intervalEnd) {
				highestStrains.add(maxStrain);
				
				if (prev == null) {
					maxStrain = 0.0;
				} else {
					double decay = Math.pow(DObj.DECAY_BASE[type], (intervalEnd - prev.getHo().getTime()) / 1000.0);
					maxStrain = prev.getStrains()[type] * decay;
				}
				
				intervalEnd += STRAIN_STEP;
			}
			
			maxStrain = Math.max(maxStrain, o.getStrains()[type]);
			prev = o;
		}
		
		double difficulty = 0.0;
		double weight = 1.0;
		
		highestStrains.sort(new Comparator<Double>() {
			@Override
			public int compare(Double v1, Double v2) {
				return v1 < v2 ? 1 : (v1 > v2 ? -1 : 0);
			}
		});
		
		for (double strain : highestStrains) {
			difficulty += weight * strain;
			weight *= DECAY_WEIGHT;
		}
		
		return difficulty;
	}
}