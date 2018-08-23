package com.ruinscraft.botboi.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordCommand implements CommandExecutor {

	private static final ChatColor MAIN_COLOR = ChatColor.BLUE;

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

		BotBoiPlugin.getInstance().getServer().getScheduler().runTaskAsynchronously(BotBoiPlugin.getInstance(), () -> {
			if (args.length > 0) {
				String token = args[0];

				if (!BotBoiPlugin.getInstance().getStorage().isUsed(token)) {
					player.sendMessage(MAIN_COLOR + "Your key has been verified.");

					BotBoiPlugin.getInstance().getStorage().setUUID(token, player.getUniqueId());
					BotBoiPlugin.getInstance().getStorage().setUsed(token, true);
					BotBoiPlugin.getInstance().getStorage().setWaiting(token, true);
				} else {
					player.sendMessage(MAIN_COLOR + "Key already used or does not exist.");
				}
			} else {
				player.sendMessage(MAIN_COLOR + "Join the discord with: " + discordLink);
				player.sendMessage(MAIN_COLOR + "Authenticate with /discord <key>");
			}
		});

		return true;
	}

}
