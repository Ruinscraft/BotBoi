package com.ruinscraft.botboi.server;

import java.util.Properties;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class BotBoiServer extends ListenerAdapter implements Runnable {

	private Properties settings;
	
	private static BotBoiServer instance;
	
	public static BotBoiServer getInstance() {
		return instance;
	}
	
	public BotBoiServer(Properties settings) {
		instance = this;
		
		this.settings = settings;
	}
	
	public Properties getSettings() {
		return settings;
	}
	
	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		Member guildMember = event.getMember();
		User discordUser = guildMember.getUser();

		discordUser.openPrivateChannel().queue((channel) -> {
            channel.sendMessage("").queue();
        });
	}
	
	@Override
	public void run() {
		JDA jda = null;
		
		try {
			jda = new JDABuilder(AccountType.BOT).setToken(settings.getProperty("discord.token")).buildBlocking();
			
			jda.addEventListener(this);
			jda.getPresence().setStatus(OnlineStatus.ONLINE);
			jda.getPresence().setGame(Game.playing("mc.ruinscraft.com"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
