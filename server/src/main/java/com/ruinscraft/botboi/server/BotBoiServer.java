package com.ruinscraft.botboi.server;

public class BotBoiServer {

	private String salt;
	
	public BotBoiServer(String args[]) {
		if (args.length < 1) {
			throw new IllegalArgumentException("Salt is required.");
		}
		
		this.salt = args[0];
	}
	
	public String getSalt() {
		return salt;
	}
	
}
