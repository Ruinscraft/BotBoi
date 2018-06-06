package com.ruinscraft.botboi.storage;

import java.util.UUID;

public interface Storage {

	void insertKey(String key, String discordId, long time);
	
	void insertKey(String key, String discordId, UUID mojangUUID, long time);
	
	UUID getLinkedAccount(String discordId);
	
	default boolean isVerified(String discordId) {
		return getLinkedAccount(discordId) != null;
	}
	
	default String generateKey() {
		return UUID.randomUUID().toString().substring(0, 7);
	}
	
}
