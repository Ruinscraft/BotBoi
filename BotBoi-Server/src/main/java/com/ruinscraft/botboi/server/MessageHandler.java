package com.ruinscraft.botboi.server;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

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

	private static Map<String, Queue<String>> messages = new HashMap<>();

	public static Map<String, Queue<String>> getMessages() {
		return messages;
	}

	public static void loadEntries(Set<String> entries) {
		for (String entry : entries) {
			MessageHandler.addNewMessages(entry);
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
				if (getBestRole(member).compareTo(botRole) > 0) {
					staff.add(member);
				}
			}
			Member chosen = staff.get((int) (Math.random() * (staff.size() - 1)));
			message = message.replace("{staff-random}", chosen.getNickname());
		}
		if (message.contains("{add-reaction:")) {
			Guild guild = event.getMessage().getGuild();
			String getReaction = message.substring(
					message.indexOf("{add-reaction:"), message.indexOf("}"));
			getReaction = getReaction.replace("{add-reaction:", "").replace("}", "");
			event.getMessage().addReaction(guild.getEmoteById(getReaction)).queue();
			message = message.replace("{add-reaction:" + getReaction + "}", "");
		}
		return message;
	}

	public static Queue<String> getMessagesFromGoogle(String lookFor) {
		Queue<String> messages = new LinkedList<>();

		SpreadsheetService service = new SpreadsheetService("Sheet1");

		try {
			String sheetUrl = BotBoiServer.getInstance().getSettings().getProperty("sheet.url");
			URL url = new URL(sheetUrl);

			ListFeed lf = service.getFeed(url, ListFeed.class);
			for (ListEntry le : lf.getEntries()) {
				CustomElementCollection cec = le.getCustomElements();
				messages.add(cec.getValue(lookFor));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return messages;
	}

	public static String getMessage(String sent) {
		for (Entry<String, Queue<String>> entry : messages.entrySet()) {
			String lookFor = entry.getKey();
			if (lookFor.equals("else")) {
				continue;
			}
			if (sent.toLowerCase().contains(lookFor)) {
				Queue<String> toSendBack = entry.getValue();
				while (toSendBack.poll() == null) {
					addNewMessages(lookFor);
				}
				return messages.get(lookFor).poll();
			}
		}
		while (messages.get("else") == null || messages.get("else").poll() == null) {
			addNewMessages("else");
		}
		return messages.get("else").poll();
	}

	public static void addNewMessages(String lookFor) {
		Queue<String> newMessages = getMessagesFromGoogle(lookFor);
		Collections.shuffle((List<?>) newMessages);
		messages.put(lookFor, newMessages);
	}

}