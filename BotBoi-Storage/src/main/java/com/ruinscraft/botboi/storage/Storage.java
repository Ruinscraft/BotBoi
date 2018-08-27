package com.ruinscraft.botboi.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ruinscraft.botboi.storage.TokenInfo;

public interface Storage {

	boolean isSetup();

	void insertToken(String token, String discordId);

	void setWaiting(String token, boolean waiting);

	void setUsed(String token, boolean used);

	void setUUID(String token, UUID uuid);

	List<TokenInfo> getWaiting();

	void deleteUser(String discordId);

	boolean isUsed(String token);

	Map<String, UUID> getIDsWithUUIDs();

	String getUsername(UUID uuid);

	boolean hasPermission(UUID uuid, String permission);

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
