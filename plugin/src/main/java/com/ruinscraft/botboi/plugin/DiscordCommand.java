package com.ruinscraft.botboi.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordCommand implements CommandExecutor {

	private String key;
	
	public DiscordCommand(String key) {
		this.key = key;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (!(sender instanceof Player)) {
			return false;
		}
		
		Player player = (Player) sender;
		
		player.sendMessage("Join the discord with: discord.gg/something");
		player.sendMessage("Your key is: " + key);
		
		return true;
	}
	
}
