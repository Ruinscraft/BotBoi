package com.ruinscraft.botboi.crypto;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.UUID;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class Crypto {

	private static String oneTimePad(boolean encrypt, String text, String key) {
		String otp = "";
		
		try {
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByName("JavaScript");
			
			engine.eval(Files.newBufferedReader(Paths.get("src/main/js/otp.js"), StandardCharsets.UTF_8));

			Invocable inv = (Invocable) engine;
			
			int encrMethod = 1;
			
			if (!encrypt) {
				encrMethod = -1;
			}
			
			Object o = inv.invokeFunction("OneTimePad", encrMethod, text, key);
			
			otp = o.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return otp;
	}
	
	public static String encrypt(String key, String text) {
		if (key.length() < text.length()) {
			throw new InvalidParameterException("key must be at least as long as text");
		}
		
		return oneTimePad(true, text, key);
	}

	public static String decrypt(String key, String text) {
		if (key.length() < text.length()) {
			throw new InvalidParameterException("key must be at least as long as text");
		}
		
		return oneTimePad(false, text, key);
	}
	
	public static String encrypt(String key, UUID uuid) {
		String uuidSub = uuid.toString().substring(0, 5);
		
		uuidSub = "d" + uuidSub + "c";
		
		String uuidSubRepl = "";
		
		for (int i = 0; i < uuidSub.length(); i++) {
			
			char c = uuidSub.charAt(i);
			
			if (Character.isDigit(c)) {
				int charVal = Character.getNumericValue(c);
				if (charVal < 9) {
					uuidSubRepl += charVal + 1;
				} else {
					uuidSubRepl += charVal - 1;
				}
			} else {
				uuidSubRepl += c;
			}
			
		}
		
		return encrypt(key, uuidSubRepl);
	}

}
