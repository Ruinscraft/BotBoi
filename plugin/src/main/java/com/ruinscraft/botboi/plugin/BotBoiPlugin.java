package com.ruinscraft.botboi.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class BotBoiPlugin extends JavaPlugin {
	
	@Override
	public void onEnable() {
		String salt = getConfig().getString("salt");
		
		getCommand("discord").setExecutor(new DiscordCommand(salt));
	}
	
}
