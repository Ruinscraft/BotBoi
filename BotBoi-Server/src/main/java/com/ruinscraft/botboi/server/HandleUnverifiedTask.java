package com.ruinscraft.botboi.server;

import java.util.TimerTask;

import com.ruinscraft.botboi.server.bootstrap.Bootstrap;
import com.ruinscraft.botboi.storage.TokenInfo;

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

		System.out.println("Guild: " + guild.getName() + " ID: " + guildId);
	}

	@Override
	public void run() {
		for (TokenInfo tokenInfo : botBoiServer.getStorage().getWaiting()) {
			String memberRoleId = botBoiServer.getSettings().getProperty("discord.memberRoleId");

			botBoiServer.getStorage().setWaiting(tokenInfo.getToken(), false);

			try {
				User user = botBoiServer.getJDA().getUserById(tokenInfo.getDiscordId());
				Member member = guild.getMember(user);
				Role role = guild.getRoleById(memberRoleId);

				boolean done = false;
				for (Role otherRole : member.getRoles()) {
					if (otherRole.equals(role)) {
						guildController.setNickname(member, tokenInfo.getMcUser()).queue();
						System.out.println("Updating " + user.getName() + " to " + tokenInfo.getMcUser());

						user.openPrivateChannel().queue((channel) -> {
							channel.sendMessage(botBoiServer.getSettings()
									.getProperty("messages.updatedname")).queue();
						});
						Bootstrap.updateName();
						done = true;
					}
				}
				if (done) {
					continue;
				}

				guildController.setNickname(member, tokenInfo.getMcUser()).queue();
				guildController.addSingleRoleToMember(member, role).queue();

				System.out.println("Verifying " + user.getName());

				user.openPrivateChannel().queue((channel) -> {
					channel.sendMessage(botBoiServer.getSettings().getProperty("messages.verified")).queue();
				});
				Bootstrap.confirmUser();
			} catch (Exception e) {
				System.out.println("Failed to add role to member... did they leave the guild?");
				e.printStackTrace();
			}
		}
	}

}
