package com.ruinscraft.botboi.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;

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

	private Collection<String> recentIDs;

	public HandleUnverifiedTask(BotBoiServer botBoiServer) {
		String guildId = botBoiServer.getSettings().getProperty("discord.guildId");

		this.botBoiServer = botBoiServer;
		this.guild = botBoiServer.getJDA().getGuildById(guildId);
		this.guildController = new GuildController(guild);
		this.recentIDs = new HashSet<>();

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
				if (done) continue;

				if (recentIDs.contains(tokenInfo.getDiscordId())) continue;
				recentIDs.add(tokenInfo.getDiscordId());
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						recentIDs.remove(tokenInfo.getDiscordId());
					}
				}, 9000L);

				guildController.setNickname(member, nickname).queue();
				guildController.addSingleRoleToMember(member, role).queue();

				updateMemberRoles(member, tokenInfo.getUUID());

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

		for (Entry<String, UUID> entry : botBoiServer.getStorage().getIDsWithUUIDs().entrySet()) {
			if (recentIDs.contains(entry.getKey())) continue;
			if (guild.getMemberById(entry.getKey()) == null) {
				botBoiServer.getStorage().deleteUser(entry.getKey());
				continue;
			}
			updateMemberRoles(entry.getKey(), entry.getValue());
		}
	}

	private void updateMemberRoles(String memberID, UUID uuid) {
		updateMemberRoles(guild.getMemberById(memberID), uuid);
	}

	private void updateMemberRoles(Member member, UUID uuid) {
		List<Role> roles = new ArrayList<>();

		for (Entry<String, Long> entry : botBoiServer.getPermissions().entrySet()) {
			Role role = guild.getRoleById(entry.getValue());

			if (botBoiServer.getStorage().hasPermission(uuid, entry.getKey()) && !member.getRoles().contains(role)) {
				roles.add(role);
			}
		}

		if (roles.isEmpty()) return;

		List<String> roleNames = roles.stream().map(Role::getName).collect(Collectors.toList());
		String joinedRoleNameList = String.join(",", roleNames);

		member.getUser().openPrivateChannel().queue((channel) -> {
			String roleAdded = String.format(
					botBoiServer.getSettings().getProperty("messages.roleadded"), joinedRoleNameList);
			botBoiServer.logSendMessage(channel, roleAdded);
			channel.sendMessage(roleAdded).queue();
		});

		guildController.addRolesToMember(member, roles.toArray(new Role[0])).queue();
	}

}
