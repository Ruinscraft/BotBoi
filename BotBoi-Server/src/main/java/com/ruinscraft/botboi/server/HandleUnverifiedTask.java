package com.ruinscraft.botboi.server;

import java.util.Map;
import java.util.TimerTask;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.managers.GuildController;

public class HandleUnverifiedTask extends TimerTask {

	private final BotBoiServer botBoiServer;

	private final Guild guild;
	private final GuildController guildController;

	public HandleUnverifiedTask(BotBoiServer botBoiServer) {
		String guildId = botBoiServer.getSettings().getProperty("discord.guildId");

		this.botBoiServer = botBoiServer;
		this.guild = botBoiServer.getJDA().getGuildById(guildId);
		this.guildController = new GuildController(guild);
	}

	@Override
	public void run() {
		for (Map.Entry<String, String> unused : botBoiServer.getStorage().getWaiting().entrySet()) {
			String memberRoleId = botBoiServer.getSettings().getProperty("discord.memberRoleId");

			botBoiServer.getStorage().setWaiting(unused.getKey(), false);

			try {
				User user = botBoiServer.getJDA().getUserById(unused.getValue());
				Member member = guild.getMember(user);
				Role role = guild.getRoleById(memberRoleId);

				guildController.addSingleRoleToMember(member, role).queue();

				System.out.println("Verifying " + user.getName());

				user.openPrivateChannel().queue((channel) -> {
		            channel.sendMessage(botBoiServer.getSettings().getProperty("messages.verified")).queue();
		        });
			} catch (Exception e) {
				System.out.println("Failed to add role to member... did they leave the guild?");
			}
		}
	}

}
