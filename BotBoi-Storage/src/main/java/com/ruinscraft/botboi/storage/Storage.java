package com.ruinscraft.botboi.storage;

import java.util.Map;
import java.util.UUID;

public interface Storage {

	boolean isSetup();
	
	void insertToken(String token, String discordId);

	void setWaiting(String token, boolean waiting);
	
	void setUsed(String token, boolean used);
	
	Map<String, String> getWaiting();

	boolean isUsed(String token);
	
	void close();
	
	default boolean isWaiting(String token) {
		return getWaiting().containsKey(token);
	}
	
	default String generateToken() {
		return UUID.randomUUID().toString().substring(0, 7);
	}
	
}
