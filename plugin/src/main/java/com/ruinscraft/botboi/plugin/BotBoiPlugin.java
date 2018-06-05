package com.ruinscraft.botboi.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class BotBoiPlugin extends JavaPlugin {
	
	@Override
	public void onEnable() {
		String key = getConfig().getString("key");
		String discordLink = getConfig().getString("discord_link");
		
		getCommand("discord").setExecutor(new DiscordCommand(key, discordLink));
	}
	
}
