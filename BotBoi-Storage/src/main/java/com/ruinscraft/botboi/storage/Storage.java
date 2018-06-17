package com.ruinscraft.botboi.storage;

import java.util.List;
import java.util.UUID;

import com.ruinscraft.botboi.storage.MySqlStorage.TokenInfo;

public interface Storage {

	boolean isSetup();
	
	void insertToken(String token, String discordId);

	void setWaiting(String token, boolean waiting);

	void setUsed(String token, boolean used);

	void setUsername(String token, String username);

	List<TokenInfo> getWaiting();

	boolean isUsed(String token);

	void close();

	default boolean isWaiting(String token) {
		for (TokenInfo tokenInfo : getWaiting()) {
			if (tokenInfo.getToken().equals(token)) {
				return true;
			}
		}
		return false;
	}

	default String generateToken() {
		return UUID.randomUUID().toString().substring(0, 7);
	}

}
