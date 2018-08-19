package com.ruinscraft.botboi.server;

import org.json.JSONObject;

import com.google.common.base.CharMatcher;

public class FilterUtils {

	public static boolean isASCII(String string) {
		return CharMatcher.ASCII.matchesAllOf(string);
	}

	public static boolean isAppropriate(String string, String webpurifyApiKey) {
		try {
			if (string.isEmpty()) {
				return true;
			}
			if (!NetUtils.isOpen("api1.webpurify.com")) {
				return true;
			}
			String url = NetUtils.generateWebPurifyUrl(string, webpurifyApiKey);
			String response = NetUtils.getResponse(url);
			JSONObject json = new JSONObject(response).getJSONObject("rsp");
			if (json.getInt("found") > 0) {
				return false;
			}
			return true;
		} catch (Exception e) {
			BotBoiServer.getInstance().log("Error contacting webpurify.");
		}
		return true;
	}

}
