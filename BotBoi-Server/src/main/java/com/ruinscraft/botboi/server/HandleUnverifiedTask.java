package com.ruinscraft.botboi.server;

import java.util.Map.Entry;
import java.util.TimerTask;

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

				String nickname = botBoiServer.getStorage().getUsername(tokenInfo.getUUID());

				boolean done = false;
				for (Role otherRole : member.getRoles()) {
					if (otherRole.equals(role)) {
						String oldName = member.getEffectiveName();
						guildController.setNickname(member, nickname).queue();

						user.openPrivateChannel().queue((channel) -> {
							String message = botBoiServer.getSettings()
									.getProperty("messages.updatedname");
							BotBoiServer.getInstance().logSendMessage(channel, message);
							channel.sendMessage(message).queue();
						});
						BotBoiServer.getInstance().logUpdateName(oldName, nickname);
						done = true;
					}
				}
				if (done) {
					continue;
				}

				guildController.setNickname(member, nickname).queue();
				guildController.addSingleRoleToMember(member, role).queue();

				for (Entry<String, Long> entry : botBoiServer.getPermissions().entrySet()) {
					if (botBoiServer.getStorage().hasPermission(tokenInfo.getUUID(), entry.getKey())) {
						guildController.addRolesToMember(member, guild.getRoleById(entry.getValue())).queue();
					}
				}

				user.openPrivateChannel().queue((channel) -> {
					String verified = botBoiServer.getSettings().getProperty("messages.verified");
					BotBoiServer.getInstance().logSendMessage(channel, verified);
					channel.sendMessage(verified).queue();
				});
				BotBoiServer.getInstance().logConfirmUser(member.getEffectiveName());
			} catch (Exception e) {
				System.out.println("Failed to add role to member... did they leave the guild?");
				e.printStackTrace();
			}
		}
	}

}
