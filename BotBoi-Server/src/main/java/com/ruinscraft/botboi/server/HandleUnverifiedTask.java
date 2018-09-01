package com.ruinscraft.botboi.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

	private Map<String, Role> permissions;

	private Collection<String> recentIDs;

	private Role memberRole;

	public HandleUnverifiedTask(BotBoiServer botBoiServer) {
		String guildId = botBoiServer.getSettings().getProperty("discord.guildId");

		this.botBoiServer = botBoiServer;
		this.guild = botBoiServer.getJDA().getGuildById(guildId);
		this.guildController = new GuildController(guild);
		this.recentIDs = new HashSet<>();
		this.memberRole = guild.getRoleById(botBoiServer.getSettings().getProperty("discord.memberRoleId"));

		Map<String, Role> permissions = new HashMap<>();
		String[] separatedPerms = botBoiServer.getSettings().getProperty("minecraft.permissions").split(";");
		for (String separatedPerm : separatedPerms) {
			String[] separatedPermAndRole = separatedPerm.split(",");
			try {
				permissions.put(separatedPermAndRole[0], guild.getRoleById(Long.valueOf(separatedPermAndRole[1])));
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new ArrayIndexOutOfBoundsException("Permissions were formatted incorrectly! perm,id;perm,id");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.permissions = permissions;

		System.out.println("Guild: " + guild.getName() + " ID: " + guildId);
	}

	@Override
	public void run() {
		for (TokenInfo tokenInfo : botBoiServer.getStorage().getWaiting()) {
			botBoiServer.getStorage().setWaiting(tokenInfo.getToken(), false);

			try {
				User user = botBoiServer.getJDA().getUserById(tokenInfo.getDiscordId());
				Member member = guild.getMember(user);

				String nickname = botBoiServer.getStorage().getUsername(tokenInfo.getUUID());

				boolean done = false;
				for (Role otherRole : member.getRoles()) {
					if (otherRole.equals(memberRole)) {
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
				}, 17000L);

				guildController.setNickname(member, nickname).queue();
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
		Set<Role> roles = new HashSet<>();
		roles.add(memberRole);

		for (Entry<String, Role> entry : this.permissions.entrySet()) {
			String perm = entry.getKey();
			Role role = entry.getValue();

			if (!botBoiServer.getStorage().hasPermission(uuid, perm)) continue;

			roles.add(role);

			List<String> groupsComplete = new ArrayList<>();
			List<String> groupsToCheck = new ArrayList<>();
			groupsToCheck.add(perm.replace("group.", ""));

			while (true) {
				for (int i = 0; i < groupsToCheck.size(); i++) {
					String group = groupsToCheck.get(i);

					for (String permission : botBoiServer.getStorage().getPermissionsFromGroup(group)) {
						if (permission.contains("group.") && 
								!groupsToCheck.contains(permission) && 
								!groupsComplete.contains(permission)) {
							groupsToCheck.add(0, permission.replace("group.", ""));
							i++;
							if (this.permissions.containsKey(permission)) {
								roles.add(this.permissions.get(permission));
							}
						}
					}

					groupsToCheck.remove(group);
					groupsComplete.add(group);
					i--;
				}

				if (groupsToCheck.size() == 0) break;
			}
		}

		if (roles.isEmpty()) return;

		List<String> roleNames = roles.stream().map(Role::getName).collect(Collectors.toList());

		List<String> updatedRoleNames = member.getRoles().stream().map(Role::getName).collect(Collectors.toList());
		for (int i = 0; i < roleNames.size(); i++) {
			String role = roleNames.get(i);
			for (String otherRole : updatedRoleNames) {
				if (role.equals(otherRole)) { 
					roleNames.remove(role);
					i--;
					break;
				}
			}
		}
		if (roleNames.isEmpty()) return;
		String joinedRoleNameList = String.join(", ", roleNames);

		member.getUser().openPrivateChannel().queue((channel) -> {
			String roleAdded = String.format(
					botBoiServer.getSettings().getProperty("messages.roleadded"), joinedRoleNameList);
			botBoiServer.logSendMessage(channel, roleAdded);
			channel.sendMessage(roleAdded).queue();
		});

		guildController.addRolesToMember(member, roles.toArray(new Role[0])).queue();
	}

}
