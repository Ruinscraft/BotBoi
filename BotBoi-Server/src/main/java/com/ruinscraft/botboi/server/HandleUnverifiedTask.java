package com.ruinscraft.botboi.server;

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
		for (String unverifiedUser : botBoiServer.getStorage().getUnverified()) {
			String memberRoleId = botBoiServer.getSettings().getProperty("discord.memberRoleId");
			
			User user = botBoiServer.getJDA().getUserById(unverifiedUser);
			Member member = guild.getMember(user);
			Role role = guild.getRoleById(memberRoleId);
			
			System.out.println("Verifying " + user.getName());
			
			guildController.addSingleRoleToMember(member, role).queue();
		}
	}
	
}
