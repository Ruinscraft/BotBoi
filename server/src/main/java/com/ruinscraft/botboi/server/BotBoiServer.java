package com.ruinscraft.botboi.server;

public class BotBoiServer {

	private String discordToken;
	private String key;
	
	public BotBoiServer(String discordToken, String key) {
		this.discordToken = discordToken;
		this.key = key;
	}
	
	public String getDiscordToken() {
		return discordToken;
	}
	
	public String getKey() {
		return key;
	}
	
}
