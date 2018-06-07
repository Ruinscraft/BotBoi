package com.ruinscraft.botboi.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordCommand implements CommandExecutor {

	private String discordLink;
	
	public DiscordCommand(String discordLink) {
		this.discordLink = discordLink;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}
		
		Player player = (Player) sender;
		
		if (args.length > 1) {
			String inputKey = args[0];
			
			if (BotBoiPlugin.getInstance().getStorage().isUnverified(inputKey)) {
				// success
			} else {
				player.sendMessage("Key already used or does not exist.");
			}
		} else {
			player.sendMessage("Join the discord with: " + discordLink);
			player.sendMessage("Authenticate with /discord <key>");
		}
		
		return true;
	}
	
}
