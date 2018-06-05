package com.ruinscraft.botboi.server;

public class BotBoiServer {

	private String discordToken;
	private String salt;
	
	public BotBoiServer(String discordToken, String salt) {
		this.discordToken = discordToken;
		this.salt = salt;
	}
	
	public String getDiscordToken() {
		return discordToken;
	}
	
	public String getSalt() {
		return salt;
	}
	
}
