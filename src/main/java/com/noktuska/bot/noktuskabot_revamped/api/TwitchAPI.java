package com.noktuska.bot.noktuskabot_revamped.api;

import org.json.JSONObject;

import com.noktuska.bot.noktuskabot_revamped.Console;
import com.noktuska.bot.noktuskabot_revamped.Reference;
import com.noktuska.bot.noktuskabot_revamped.utils.Func;

public class TwitchAPI {

	private JSONObject data = null;
	
	public TwitchAPI(String username, Console logger) {
		String tmpData = "";
		
		tmpData = Func.readUrl("https://api.twitch.tv/kraken/streams/" + username + "?client_id=" + Reference.TWITCH_CLIENT_ID, null);
		
		if (tmpData == null || tmpData.equals("") || tmpData.length() < 10) {
			logger.log("WARN: " + username + " returned unsuccessful!");
			return;
		}
		
		data = new JSONObject(tmpData);
	}
	
	public boolean isValid() {
		if (data == null)
			return false;
		return (!data.equals(JSONObject.NULL));
	}
	
	public int getStreamerStatus() {
		if (data.has("error"))
			return -1;
		
		if (getObject("stream") == null)
			return 0;
		
		return 1;
	}
	
	public JSONObject getStreamObject() {
		if (getStreamerStatus() != 1)
			return null;
		
		return data.getJSONObject("stream");
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
			throw new Exception("Value is null!");
		
		return result;
	}
	
}
