package com.ruinscraft.botboi.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.ruinscraft.botboi.storage.MySqlStorage;
import com.ruinscraft.botboi.storage.Storage;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class BotBoiServer extends ListenerAdapter implements Runnable {

	private Properties settings;
	private Storage storage;
	private Map<String, Integer> names;

	private JDA jda;
	private final Timer timer;

	private static BotBoiServer instance;

	public static BotBoiServer getInstance() {
		return instance;
	}

	public BotBoiServer(Properties settings, Map<String, Integer> names) {
		instance = this;

		this.timer = new Timer();

		this.settings = settings;
		this.names = names;
		this.storage = new MySqlStorage(
				settings.getProperty("storage.mysql.host"),
				Integer.parseInt(settings.getProperty("storage.mysql.port")),
				settings.getProperty("storage.mysql.database"),
				settings.getProperty("storage.mysql.username"),
				settings.getProperty("storage.mysql.password"),
				settings.getProperty("storage.mysql.table"));

		MessageHandler.loadEntries(getSearchWords());
	}

	public synchronized void shutdown() {
		System.out.println("Shutting down...");

		try {
			storage.close();
			jda.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

	public Properties getSettings() {
		return settings;
	}

	public Storage getStorage() {
		return storage;
	}

	public Map<String, Integer> getNames() {
		return names;
	}

	public JDA getJDA() {
		return jda;
	}

	public List<String> getSearchWords() {
		String wordsTogether = settings.getProperty("sheet.searchwords");
		List<String> wordsSeparated = new ArrayList<>();
		while (wordsTogether.contains(";")) {
			String word = wordsTogether.substring(0, wordsTogether.indexOf(";") + 1);
			wordsTogether = wordsTogether.replace(word, "");
			word = word.replace(";", "").toLowerCase();
			wordsSeparated.add(word);
		}
		wordsSeparated.add(wordsTogether);
		return wordsSeparated;
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		sendWelcomeMessage(event.getUser(), "messages.welcome");
	}

	public void sendWelcomeMessage(User user, String message) {
		String token = storage.generateToken();

		String welcomeMessage = String.format(settings.getProperty(message), token);

		user.openPrivateChannel().queue((channel) -> {
			channel.sendMessage(welcomeMessage).queue();
		});

		storage.insertToken(token, user.getId());
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		String message = event.getMessage().getContentRaw();
		if (!FilterUtils.isAppropriate(message, settings.getProperty("webpurify.key"))) {
			if (event.getChannelType() == ChannelType.TEXT) {
				try {
					event.getChannel().deleteMessageById(event.getMessageId()).queue();
					String inappropriate = 
							String.format(settings.getProperty("webpurify.inappropriate"), 
									event.getMessage().getContentDisplay());
					event.getAuthor().openPrivateChannel().queue((channel) -> {
						channel.sendMessage(inappropriate).queue();
					});
				} catch (Exception e) { }
			}
			return;
		}
		if (message.contains("!updatename")) {
			sendWelcomeMessage(event.getAuthor(), "messages.updatename");
			return;
		}
		if (message.contains("<@453668483528523776>")) {
			if (event.getAuthor().getId().equals(jda.getSelfUser().getId())) {
				return;
			}
			String response = MessageHandler.getMessage(message);
			response = MessageHandler.replacePlaceholders(response, event);
			final String finalResponse = response;

			new Timer().schedule(new TimerTask() {
				public void run() {
					event.getChannel().sendTyping().queue();
					new Timer().schedule(new TimerTask() {
						public void run() {
							event.getChannel().sendMessage(finalResponse).queue();
						}
					}, (int) ((finalResponse.length() * 75) * (1 + Math.random())));
				}
			}, (int) (Math.random() * 3000));
		}
	}

	@Override
	public void run() {
		try {
			jda = new JDABuilder(AccountType.BOT).setToken(settings.getProperty("discord.token")).buildBlocking();
		} catch (Exception e) {
			System.out.println("Could not authenticate with Discord.");
			return;
		}

		if (!storage.isSetup()) {
			return;
		}

		jda.addEventListener(this);
		jda.getPresence().setStatus(OnlineStatus.ONLINE);
		jda.getPresence().setGame(Game.playing(settings.getProperty("messages.playing")));

		timer.scheduleAtFixedRate(new HandleUnverifiedTask(this), 0, TimeUnit.SECONDS.toMillis(5));
	}

}
