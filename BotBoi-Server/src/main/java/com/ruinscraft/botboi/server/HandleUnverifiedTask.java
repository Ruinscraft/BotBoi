package com.ruinscraft.botboi.server;

import com.ruinscraft.botboi.storage.TokenInfo;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class HandleUnverifiedTask extends TimerTask {

    private final BotBoiServer botBoiServer;

    private Role memberRole;

    private Map<String, Role> permissions;
    private Collection<String> recentIDs;

    public HandleUnverifiedTask(BotBoiServer botBoiServer) {
        String guildId = botBoiServer.getSettings().getProperty("discord.guildId");

        this.botBoiServer = botBoiServer;
        this.memberRole = botBoiServer.getGuild().getRoleById(botBoiServer.getSettings().getProperty("discord.memberRoleId"));

        Map<String, Role> permissions = new HashMap<>();
        String[] separatedPerms = botBoiServer.getSettings().getProperty("minecraft.permissions").split(";");
        for (String separatedPerm : separatedPerms) {
            String[] separatedPermAndRole = separatedPerm.split(",");
            try {
                permissions.put(separatedPermAndRole[0], botBoiServer.getGuild().getRoleById(Long.valueOf(separatedPermAndRole[1])));
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new ArrayIndexOutOfBoundsException("Permissions were formatted incorrectly! perm,id;perm,id");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.permissions = permissions;

        this.recentIDs = new HashSet<>();

        System.out.println("Guild: " + botBoiServer.getGuild().getName() + " ID: " + guildId);
    }

    @Override
    public void run() {
        for (TokenInfo tokenInfo : botBoiServer.getStorage().getWaiting()) {
        	System.out.println("Found " + tokenInfo.getUUID());
            botBoiServer.getStorage().setWaiting(tokenInfo.getToken(), false);

            try {
                User user = botBoiServer.getJDA().getUserById(tokenInfo.getDiscordId());
                Member member = botBoiServer.getGuild().getMember(user);

                String nickname = botBoiServer.getStorage().getUsername(tokenInfo.getUUID());

                if (recentIDs.contains(tokenInfo.getDiscordId())) continue;
                recentIDs.add(tokenInfo.getDiscordId());
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        recentIDs.remove(tokenInfo.getDiscordId());
                    }
                }, 17000L);

                member.modifyNickname(nickname).queue();
                updateMemberRoles(member, tokenInfo.getUUID());

                user.openPrivateChannel().queue((channel) -> {
                    String verified = botBoiServer.getSettings().getProperty("messages.verified");
                    BotBoiServer.getInstance().sendMessage(channel, verified);
                });
                BotBoiServer.getInstance().logConfirmUser(member.getEffectiveName());
            } catch (Exception e) {
                System.out.println("Failed to add role to member... did they leave the guild?");
                e.printStackTrace();
            }
        }

        for (Entry<String, UUID> entry : botBoiServer.getStorage().getIDsWithUUIDs().entrySet()) {
            if (recentIDs.contains(entry.getKey())) continue;
            if (botBoiServer.getGuild().getMemberById(entry.getKey()) == null) {
                botBoiServer.getStorage().deleteUser(entry.getKey());
                continue;
            }
            updateMemberRoles(entry.getKey(), entry.getValue());
        }
    }

    private void updateMemberRoles(String memberID, UUID uuid) {
        updateMemberRoles(botBoiServer.getGuild().getMemberById(memberID), uuid);
    }

    private void updateMemberRoles(Member member, UUID uuid) {
        List<Role> roles = new ArrayList<>();
        roles.add(memberRole);

        String latestUser = botBoiServer.getStorage().getUsername(uuid);
        String current = member.getEffectiveName();
        if (!latestUser.toLowerCase().equals(current.toLowerCase())) {
            member.modifyNickname(latestUser).queue();
            botBoiServer.logUpdateName(current, latestUser);
        }

        for (Entry<String, Role> entry : this.permissions.entrySet()) {
            String perm = entry.getKey();
            Role role = entry.getValue();

            if (!botBoiServer.getStorage().hasPermission(uuid, perm)) {
                if (member.getRoles().contains(role)) {
                    Collection removeRoles = new ArrayList<>();
                    removeRoles.add(role);
                    member.getGuild().modifyMemberRoles(member, null, removeRoles);
                }
                continue;
            }

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

        for (int i = 0; i < roles.size(); i++) {
            Role role = roles.get(i);
            for (Role otherRole : member.getRoles()) {
                if (role.getId().equals(otherRole.getId())) {
                    roles.remove(role);
                    i--;
                    break;
                }
            }
        }
        if (roles.isEmpty()) return;

        String joinedRoleNameList = String.join(", ", roles.stream().map(Role::getName).collect(Collectors.toList()));

        member.getUser().openPrivateChannel().queue((channel) -> {
            String roleAdded = String.format(
                    botBoiServer.getSettings().getProperty("messages.roleadded"), joinedRoleNameList);
            botBoiServer.sendMessage(channel, roleAdded);
        });
        botBoiServer.getGuild().modifyMemberRoles(member, roles, null).queue();
    }

}
