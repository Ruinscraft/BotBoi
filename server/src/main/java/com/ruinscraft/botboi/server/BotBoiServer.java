package com.ruinscraft.botboi.server;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

public class BotBoiServer implements Runnable {

	private String discordToken;
	private String key;
	
	private static BotBoiServer instance;
	
	public static BotBoiServer getInstance() {
		return instance;
	}
	
	public BotBoiServer(String discordToken, String key) {
		this.discordToken = discordToken;
		this.key = key;
		
		instance = this;
	}
	
	public String getDiscordToken() {
		return discordToken;
	}
	
	public String getKey() {
		return key;
	}
	
	@Override
	public void run() {
		JDA jda = null;
		
		try {
			jda = new JDABuilder(AccountType.BOT).setToken(discordToken).buildAsync();
		} catch (LoginException e) {
			e.printStackTrace();
			
			System.out.println("Could not authenticate");
			
			return;
		}
		
		
		
	}
	
}
