package com.ruinscraft.botboi.server;

import com.ruinscraft.botboi.server.util.FilterUtils;
import com.ruinscraft.botboi.server.util.LoggerPrintStream;
import com.ruinscraft.botboi.server.util.MessageHandler;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.ContextException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BotBoiServer extends ListenerAdapter implements Runnable {

    private static BotBoiServer instance;
    private final ScheduledExecutorService scheduler;
    private Properties settings;
    private Map<String, Integer> names;
    private List<String> badWords;
    private JDA jda;
    private Guild guild;
    private long startTime = System.currentTimeMillis();
    private int usersConfirmed = 0;
    private int namesUpdated = 0;
    private int messagesChecked = 0;
    private int messagesSent = 0;

    public BotBoiServer(Properties settings, Map<String, Integer> names) {
        instance = this;

        System.setOut(new LoggerPrintStream(System.out));

        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        this.settings = settings;
        this.names = names;

        MessageHandler.loadEntries(getSearchWords());

        badWords = Arrays.asList(settings.getProperty("messages.badwords").split(","));
    }

    public static BotBoiServer getInstance() {
        return instance;
    }

    public synchronized void shutdown() {
        System.out.println("Shutting down...");

        try {
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
        System.out.println("Uptime: " + days + "d " + hours + ":" + minutes + ":" + seconds);
        System.out.println("Users confirmed: " + usersConfirmed);
        System.out.println("Names updated: " + namesUpdated);
        System.out.println("Inappropriate messages received: " + messagesChecked);
        System.out.println("Messages sent: " + messagesSent);
    }

    public Properties getSettings() {
        return settings;
    }

    public Map<String, Integer> getNames() {
        return names;
    }

    public JDA getJDA() {
        return jda;
    }

    public Guild getGuild() {
        return guild;
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
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (senderIsSelf(event.getAuthor())) return;
        if (FilterUtils.isBadMessage(this.badWords, event.getMessage().getContentRaw())) {
            resolveInappropriateMessage(event);
        }
    }

    public void resolveInappropriateMessage(GenericMessageEvent event) {
        Message message = event.getChannel().retrieveMessageById(event.getMessageId())
                .complete();
        logCheckMessage(message);

        if (event.getChannelType() == ChannelType.TEXT) {
            try {
                event.getChannel().deleteMessageById(event.getMessageId()).queue();
                String inappropriate =
                        String.format(settings.getProperty("messages.inappropriate"),
                                message.getContentDisplay());
                message.getAuthor().openPrivateChannel().queue((channel) -> {
                    this.sendMessage(channel, inappropriate);
                });
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (senderIsSelf(event.getAuthor())) return;
        if (event == null || event.getChannelType() != ChannelType.TEXT) return;
        String message = event.getMessage().getContentRaw();

        if (event.getGuild() != null &&
                FilterUtils.isBadMessage(this.badWords, message)) {
            resolveInappropriateMessage(event);
            return;
        }
        Objects.requireNonNull(message);

        if (message.contains("!guessname")) {
            message = message.replace("!guessname ", "");
            if (message.contains(" ") || message.contains("!guessname")) return;
            this.sendMessage(event.getChannel(), MessageHandler.getBestName(message));
            return;
        }

        if (message.contains("<@453668483528523776>") || message.contains("<@!453668483528523776>")) {
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
                            BotBoiServer.this.sendMessage(channel, finalResponse);
                        }
                    }, (int) ((finalResponse.length() * 75) * (1 + Math.random())));
                }
            }, (int) (Math.random() * 3000));
        }
    }

    @Override
    public void run() {
        try {
            jda = JDABuilder.createDefault(settings.getProperty("discord.token"))
                    .enableIntents(GatewayIntent.GUILD_MEMBERS).build().awaitReady();
            jda = new JDABuilder(AccountType.BOT)
                    .setToken(settings.getProperty("discord.token")).build().awaitReady();
        } catch (Exception e) {
            System.out.println("Could not authenticate with Discord.");
            return;
        }

        jda.addEventListener(this);
        jda.getPresence().setStatus(OnlineStatus.ONLINE);
        jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing(settings.getProperty("messages.playing")));

        this.guild = jda.getGuildById(settings.getProperty("discord.guildId"));
    }

    public void logConfirmUser(String user) {
        System.out.println("Successfully verified " + user);
        usersConfirmed++;
    }

    public void logUpdateName(String oldName, String newName) {
        System.out.println("Updated " + oldName + " to " + newName);
        namesUpdated++;
    }

    public void logCheckMessage(Message message) {
        String channelName = message.getChannel().getName();
        if (message.getChannelType().equals(ChannelType.TEXT)) channelName = "#" + channelName;
        if (message.getChannelType().equals(ChannelType.PRIVATE)) channelName = "@" + channelName;

        String channel = "[" + channelName + "]";
        String user = "[@" + message.getAuthor().getName() + "]";

        String omittedMessage = message.getContentDisplay();
        if (omittedMessage.length() > 300) {
            omittedMessage = omittedMessage.substring(0, 300) + "   [...]";
        }

        System.out.println("Filtered for inappropriate language: "
                + channel + " " + user + " " + omittedMessage);
        messagesChecked++;
    }

    public void sendMessage(MessageChannel channel, String message) {
        channel.sendMessage(message).queue();
        String channelName = channel.getName();
        if (channel.getType().equals(ChannelType.TEXT)) channelName = "#" + channelName;
        if (channel.getType().equals(ChannelType.PRIVATE)) channelName = "@" + channelName;

        String channelNameFull = "[" + channelName + "]";

        String omittedMessage = message;
        if (omittedMessage.length() > 300) {
            omittedMessage = omittedMessage.substring(0, 300) + "   [...]";
        }

        System.out.println(channelNameFull + " " + omittedMessage);
        messagesSent++;
    }

}
