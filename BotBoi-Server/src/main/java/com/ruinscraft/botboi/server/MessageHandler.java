package com.ruinscraft.botboi.server;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;

import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class MessageHandler {

	private static Map<String, List<String>> allMessages = new HashMap<>();
	private static Map<String, List<String>> messages = new HashMap<>();

	public static Map<String, List<String>> getAllMessages() {
		return allMessages;
	}

	public static void addMessage(String searchWord, String message) {
		List<String> existingMessages = messages.get(searchWord);
		if (existingMessages == null) {
			messages.put(searchWord, new ArrayList<>());
		}
		existingMessages = messages.get(searchWord);
		existingMessages.add(message);
		messages.put(searchWord, existingMessages);
	}

	public static void addMessages(String searchWord, Collection<String> messagesSet) {
		List<String> existingMessages = messages.get(searchWord);
		if (existingMessages == null) {
			messages.put(searchWord, new ArrayList<>());
		}
		existingMessages = messages.get(searchWord);
		existingMessages.addAll(messagesSet);
		messages.put(searchWord, existingMessages);
	}

	public static void loadEntries(List<String> entries) {
		getMessagesFromGoogle(entries);

		for (Entry<String, List<String>> entry : allMessages.entrySet()) {
			String searchWord = entry.getKey();
			List<String> messages = entry.getValue();
			System.out.println("Adding '" + searchWord + 
					"' search-word with " + messages.size() 
					+ " messages");
			List<String> newList = new ArrayList<>();
			newList.addAll(entry.getValue());
			addMessages(entry.getKey(), newList);
		}
	}

	public static Role getBestRole(Member member) {
		Role bestRole = member.getGuild().getPublicRole();
		for (Role role : member.getRoles()) {
			if (role.compareTo(bestRole) > 0) {
				bestRole = role;
			}
		}
		return bestRole;
	}

	public static String replacePlaceholders(String message, MessageReceivedEvent event) {
		User user = event.getAuthor();
		if (message.contains("{user}")) {
			message = message.replace("{user}", user.getName());
		}
		if (message.contains("{user-rank}")) {
			message = message.replace("{user-rank}", 
					getBestRole(event.getMessage().getGuild().getMember(user)).getName());
		}
		if (message.contains("{message}")) {
			message = message.replace("{message}", event.getMessage().getContentRaw());
		}
		if (message.contains("{online-count}")) {
			Guild guild = event.getMessage().getGuild();
			int amountOnlineOrIdle = 0;
			for (Member member : guild.getMembers()) {
				if (member.getOnlineStatus().equals(OnlineStatus.ONLINE) ||
						member.getOnlineStatus().equals(OnlineStatus.IDLE)) {
					amountOnlineOrIdle++;
				}
			}
			message = message.replace("{online-count}", String.valueOf(amountOnlineOrIdle));
		}
		if (message.contains("{staff-random}")) {
			Guild guild = event.getMessage().getGuild();
			Role botRole = getBestRole(guild.getMember(event.getJDA().getSelfUser()));
			List<Member> staff = new ArrayList<>();
			for (Member member : guild.getMembers()) {
				if (!member.getOnlineStatus().equals(OnlineStatus.ONLINE) &&
						!member.getOnlineStatus().equals(OnlineStatus.IDLE)) {
					continue;
				}
				if (getBestRole(member).compareTo(botRole) > 0) {
					staff.add(member);
				}
			}
			Member chosen = staff.get((int) (Math.random() * (staff.size() - 1)));
			String name = chosen.getNickname();
			if (name == null) {
				name = chosen.getEffectiveName();
			}
			message = message.replace("{staff-random}", name);
		}
		while (message.contains("{add-reaction:")) {
			String getReaction = message.substring(
					message.indexOf("{add-reaction:"), message.indexOf("}"));
			getReaction = getReaction.replace("{add-reaction:", "").replace("}", "");
			message = message.replace("{add-reaction:" + getReaction + "}", "");
			if (getReaction.contains("<:")) {
				Guild guild = event.getGuild();
				getReaction = getReaction.replace("<:", "");
				getReaction = getReaction.substring(
						getReaction.indexOf(":") + 1, getReaction.length() - 1);
				event.getMessage().addReaction(guild.getEmoteById(getReaction)).queue();
			} else {
				event.getMessage().addReaction(getReaction).queue();
			}
		}
		return message;
	}

	public static void getMessagesFromGoogle(Collection<String> lookFors) {
		SpreadsheetService service = new SpreadsheetService("Sheet1");

		for (String lookFor : lookFors) {
			allMessages.put(lookFor, new ArrayList<>());
		}

		try {
			String sheetUrl = BotBoiServer.getInstance().getSettings().getProperty("sheet.url");
			URL url = new URL(sheetUrl);

			ListFeed lf = service.getFeed(url, ListFeed.class);
			for (ListEntry le : lf.getEntries()) {
				CustomElementCollection cec = le.getCustomElements();
				for (String lookFor : lookFors) {
					String removedSpaces = lookFor.replace(" ", "");
					String message = cec.getValue(removedSpaces);
					if (message == null) {
						continue;
					}
					allMessages.get(lookFor).add(message);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getMessage(String sent) {
		for (Entry<String, List<String>> entry : messages.entrySet()) {
			String lookFor = entry.getKey();
			if (lookFor.equals("else")) {
				continue;
			}
			if (sent.toLowerCase().contains(lookFor)) {
				return returnNewMessage(lookFor);
			}
		}
		return returnNewMessage("else");
	}

	private static String returnNewMessage(String lookFor) {
		String newMessage = null;
		for (String string : messages.get(lookFor)) {
			newMessage = string;
			break;
		}
		if (newMessage == null) {
			addNewMessages(lookFor);
			for (String string : messages.get(lookFor)) {
				newMessage = string;
				break;
			}
		}
		messages.get(lookFor).remove(newMessage);
		return newMessage;
	}

	public static void addNewMessages(String lookFor) {
		List<String> newMessages = new ArrayList<>(allMessages.get(lookFor));
		Collections.shuffle(newMessages);
		messages.put(lookFor, newMessages);
	}

}
