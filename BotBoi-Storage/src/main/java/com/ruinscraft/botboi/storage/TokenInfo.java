package com.ruinscraft.botboi.storage;

import java.util.UUID;

public class TokenInfo {

	private String token;
	private String discord_id;
	private UUID uuid;

	public TokenInfo(String token, String discord_id, UUID uuid) {
		this.token = token;
		this.discord_id = discord_id;
		this.uuid = uuid;
	}

	public String getToken() {
		return token;
	}

	public String getDiscordId() {
		return discord_id;
	}

	public UUID getUUID() {
		return uuid;
	}

}