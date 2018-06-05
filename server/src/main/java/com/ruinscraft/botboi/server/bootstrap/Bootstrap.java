package com.ruinscraft.botboi.server.bootstrap;

import com.ruinscraft.botboi.server.BotBoiServer;

public class Bootstrap {

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Required args: <discord token> <key>");
			
			return;
		}
		
		String discordToken = args[0];
		String key = args[1];
		
		// start the server
		new BotBoiServer(discordToken, key).run();
	}
	
}
