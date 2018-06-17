package com.ruinscraft.botboi.storage;

public class TokenInfo {

	private String token;
	private String discord_id;
	private String mc_user;

	public TokenInfo(String token, String discord_id, String mc_user) {
		this.token = token;
		this.discord_id = discord_id;
		this.mc_user = mc_user;
	}

	public String getToken() {
		return token;
	}

	public String getDiscordId() {
		return discord_id;
	}

	public String getMcUser() {
		return mc_user;
	}

}