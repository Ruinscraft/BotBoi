package com.ruinscraft.botboi.server.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;

public class NetUtils {

	public static boolean isOpen(String address) {
		try (Socket socket = new Socket(address, 80)) {
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static String encodeUrl(String url) {
		try {
			return URLEncoder.encode(url, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getResponse(String urlString) {
		URL url;
		BufferedReader br;
		try {
			String content = "";
			url = new URL(urlString);
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("GET");
			httpURLConnection.setConnectTimeout(15000);
			httpURLConnection.setReadTimeout(15000);
			httpURLConnection.setInstanceFollowRedirects(false);
			httpURLConnection.setAllowUserInteraction(false);
			br = new BufferedReader(
					new InputStreamReader(httpURLConnection.getInputStream()));
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				content = content + inputLine;
			}
			br.close();
			return content;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			url = null;
			br = null;
		}
		return null;
	}

	public static String generateWebPurifyUrl(String message, String apiKey) {
		message = encodeUrl(message);
		return "http://api1.webpurify.com/services/rest/?api_key=" + apiKey
				+ "&method=webpurify.live.check&format=json&text=" + message;
	}

}
