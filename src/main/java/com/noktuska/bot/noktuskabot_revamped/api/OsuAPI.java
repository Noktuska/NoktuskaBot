package com.noktuska.bot.noktuskabot_revamped.api;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.noktuska.bot.noktuskabot_revamped.Console;
import com.noktuska.bot.noktuskabot_revamped.Reference;
import com.noktuska.bot.noktuskabot_revamped.utils.Func;

public class OsuAPI {

	private JSONArray array = null;
	private JSONObject data = null;
	
	public OsuAPI(String type, String keys, Console logger) {
		String tmpData = "";
		
		tmpData = Func.readUrl("https://osu.ppy.sh/api/" + type + "?k=" + Reference.OSU_API_KEY + "&" + keys, logger);
		
		if (tmpData.equals("") || tmpData.length() < 10) {
			logger.log("WARN: " + type + " : " + keys + " returned unsuccessful!");
			return;
		}
		
		array = new JSONArray(tmpData);
		
		if (array.length() > 0)
			data = array.getJSONObject(0);
	}
	
	public boolean isValid() {
		if (data == null)
			return false;
		return (!data.equals(JSONObject.NULL));
	}
	
	public List<JSONObject> split() {
		List<JSONObject> result = new ArrayList<>(array.length());
		
		for (Object elem : array) {
			if (elem instanceof JSONObject)
				result.add((JSONObject)elem);
		}
		
		return result;
	}
	
	public Object getObject(String key) {
		try {
			return tryGetObject(key);
		} catch (Exception e) {
			return null;
		}
	}
	
	public Object tryGetObject(String key) throws Exception {
		Object result = data.get(key);
		
		if (result.equals(JSONObject.NULL))
			throw new Exception("The value is null");
		
		return result;
	}

}
