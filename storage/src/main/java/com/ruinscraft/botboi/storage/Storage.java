package com.ruinscraft.botboi.storage;

import java.util.Set;
import java.util.UUID;

public interface Storage {

	void insertKey(String key, String discordId, long time);
	
	void insertKey(String key, String discordId, UUID mojangUUID, long time);

	void updateKey(String key, String discordId, UUID mojangUUID);
	
	Set<String> getUnverified();
	
	default boolean isVerified(String discordId) {
		return !getUnverified().contains(discordId);
	}
	
	default String generateKey() {
		return UUID.randomUUID().toString().substring(0, 7);
	}
	
}
