package com.ruinscraft.botboi.server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class BotBoiServer extends ListenerAdapter implements Runnable {

	private Properties settings;
	private Storage storage;
	private Map<String, Integer> names;

	private JDA jda;
	private final Timer timer;

	private static BotBoiServer instance;

	private long startTime = System.currentTimeMillis();
	private int usersConfirmed = 0;
	private int namesUpdated = 0;
	private int messagesChecked = 0;
	private int messagesSent = 0;

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
		log("Shutting down...");

		try {
			storage.close();
			jda.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

	public void reportStats() {
		long time = System.currentTimeMillis() - startTime;
		String days = String.valueOf((time / 86400000));
		String hours = String.valueOf(((time % 86400000) / 3600000));
		String minutes = String.valueOf((((time % 86400000) % 3600000) / 60000));
		String seconds = String.valueOf(((((time % 86400000) % 3600000) % 60000) / 1000));
		if (hours.length() == 1) {
			hours = "0" + hours;
		}
		if (minutes.length() == 1) {
			minutes = "0" + minutes;
		}
		if (seconds.length() == 1) {
			seconds = "0" + seconds;
		}
		log("Uptime: " + days + "d " + hours + ":" + minutes + ":" + seconds);
		System.out.println("Users confirmed: " + usersConfirmed);
		System.out.println("Names updated: " + namesUpdated);
		System.out.println("Inappropriate messages received: " + messagesChecked);
		System.out.println("Messages sent: " + messagesSent);
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

	public List<SearchWord> getSearchWords() {
		String wordsTogether = settings.getProperty("sheet.searchwords");
		List<SearchWord> searchWords = new ArrayList<>();

		while (wordsTogether.contains(";")) {
			String word = wordsTogether.substring(0, wordsTogether.indexOf(";") + 1);
			wordsTogether = wordsTogether.replace(word, "");
			word = word.replace(";", "").toLowerCase();

			SearchWord searchWord = SearchWord.fromFormattedList(word);
			searchWords.add(searchWord);
		}

		searchWords.add(SearchWord.fromFormattedList(wordsTogether));

		return searchWords;
	}

	public boolean senderIsSelf(User user) {
		return user.getId().equals(jda.getSelfUser().getId());
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		sendWelcomeMessage(event.getUser(), "messages.welcome");
	}

	public void sendWelcomeMessage(User user, String message) {
		String token = storage.generateToken();

		String welcomeMessage = String.format(settings.getProperty(message), token);

		user.openPrivateChannel().queue((channel) -> {
			sendMessage(channel, welcomeMessage);
			channel.sendMessage(welcomeMessage).queue();
		});

		storage.insertToken(token, user.getId());
	}

	@Override
	public void onMessageUpdate(MessageUpdateEvent event) {
		if (senderIsSelf(event.getAuthor())) return;
		if (!FilterUtils.isAppropriate(event.getMessage().getContentRaw(), 
				settings.getProperty("webpurify.key"))) {
			resolveInappropriateMessage(event);
		}
	}

	public void resolveInappropriateMessage(GenericMessageEvent event) {
		Message message = event.getChannel().getMessageById(event.getMessageId())
				.complete();
		checkMessage(message);
		if (event.getChannelType() == ChannelType.TEXT) {
			try {
				event.getChannel().deleteMessageById(event.getMessageId()).queue();
				String inappropriate = 
						String.format(settings.getProperty("webpurify.inappropriate"), 
								message.getContentDisplay());
				message.getAuthor().openPrivateChannel().queue((channel) -> {
					sendMessage(channel, inappropriate);
					channel.sendMessage(inappropriate).queue();
				});
			} catch (Exception e) { }
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (senderIsSelf(event.getAuthor())) return;
		String message = event.getMessage().getContentRaw();

		if (!FilterUtils.isAppropriate(message, settings.getProperty("webpurify.key"))) {
			resolveInappropriateMessage(event);
			return;
		}

		if (message.contains("!updatename")) {
			sendWelcomeMessage(event.getAuthor(), "messages.updatename");
			return;
		}

		if (message.contains("<@453668483528523776>")) {
			if (event.getGuild() == null) return;
			String response = MessageHandler.getMessage(message);
			response = MessageHandler.replacePlaceholders(response, event);
			final String finalResponse = response;

			MessageChannel channel = event.getChannel();

			new Timer().schedule(new TimerTask() {
				public void run() {
					channel.sendTyping().queue();
					new Timer().schedule(new TimerTask() {
						public void run() {
							sendMessage(channel, finalResponse);
							channel.sendMessage(finalResponse).queue();
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
			log("Could not authenticate with Discord.");
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

	public void log(String message) {
		String dateAndTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		dateAndTime = dateAndTime.replace("T", " ");
		if (dateAndTime.contains(".")) dateAndTime = dateAndTime.substring(0, dateAndTime.indexOf("."));
		String prefix = "[LOG " + dateAndTime + "] ";
		System.out.println(prefix + message);
	}

	public void confirmUser(String user) {
		log("Successfully verified " + user);
		usersConfirmed++;
	}

	public void updateName(String oldName, String newName) {
		log("Updated " + oldName + " to " + newName);
		namesUpdated++;
	}

	public void checkMessage(Message message) {
		String channelName = message.getChannel().getName();
		if (message.getChannelType().equals(ChannelType.TEXT)) channelName = "#" + channelName;
		if (message.getChannelType().equals(ChannelType.PRIVATE)) channelName = "@" + channelName;
		String channel = "[" + channelName + "]";
		String user = "[" + message.getAuthor().getName() + "]";
		String omittedMessage = message.getContentDisplay();
		if (omittedMessage.length() > 300) {
			omittedMessage = omittedMessage.substring(0, 300) + "   [...]";
		}
		log("Filtered for inappropriate language: " + channel + " " + user + " " + omittedMessage);
		messagesChecked++;
	}

	public void sendMessage(MessageChannel channel, String message) {
		String channelName = channel.getName();
		if (channel.getType().equals(ChannelType.TEXT)) channelName = "#" + channelName;
		if (channel.getType().equals(ChannelType.PRIVATE)) channelName = "@" + channelName;
		String channelNameFull = "[" + channelName + "]";
		String omittedMessage = message;
		if (omittedMessage.length() > 300) {
			omittedMessage = omittedMessage.substring(0, 300) + "   [...]";
		}
		log(channelNameFull + " " + omittedMessage);
		messagesSent++;
	}

}
