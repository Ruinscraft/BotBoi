package com.ruinscraft.botboi.plugin;

import org.bukkit.plugin.java.JavaPlugin;

import com.ruinscraft.botboi.storage.MySqlStorage;
import com.ruinscraft.botboi.storage.Storage;

public class BotBoiPlugin extends JavaPlugin {

	private Storage storage;

	private static BotBoiPlugin instance;

	public static BotBoiPlugin getInstance() {
		return instance;
	}

	@Override
	public void onEnable() {
		instance = this;

		saveDefaultConfig();

		this.storage = new MySqlStorage(
				getConfig().getString("storage.mysql.host"),
				Integer.parseInt(getConfig().getString("storage.mysql.port")),
				getConfig().getString("storage.mysql.database"),
				getConfig().getString("storage.mysql.username"),
				getConfig().getString("storage.mysql.password"),
				getConfig().getString("storage.mysql.table"));

		String discordLink = getConfig().getString("discord_link");

		getCommand("discord").setExecutor(new DiscordCommand(discordLink));
	}

	@Override
	public void onDisable() {
		storage.close();
	}

	public Storage getStorage() {
		return storage;
	}

}
