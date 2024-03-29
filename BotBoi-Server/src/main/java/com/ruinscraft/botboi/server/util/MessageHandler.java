package com.ruinscraft.botboi.server.util;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.ruinscraft.botboi.server.BotBoiServer;
import com.ruinscraft.botboi.server.SearchWord;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

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
        Collections.shuffle(existingMessages);
        messages.put(searchWord, existingMessages);
    }

    public static void loadEntries(Collection<SearchWord> entries) {
        getMessagesFromGoogle(entries);

        for (Entry<String, List<String>> entry : allMessages.entrySet()) {
            String searchWord = entry.getKey();
            List<String> messages = entry.getValue();
            if (messages.size() == 0) {
                System.out.println("Could not add '" + searchWord + "': " +
                        "search-word had no messages in its column.");
                continue;
            } else {
                System.out.println("Adding '" + searchWord +
                        "' search-word with " + messages.size()
                        + " messages");
            }
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

    public static String getBestName(String username) {
        if (username.length() < 6) {
            return username;
        }
        username = username.replaceAll("[0-9]", " ");
        Map<String, Integer> names = BotBoiServer.getInstance().getNames();
        String lowerCaseUsername = username.toLowerCase();
        String bestName = null;
        int bestFrequency = 0;
        for (Entry<String, Integer> entry : names.entrySet()) {
            String name = entry.getKey();
            if (lowerCaseUsername.contains(name.toLowerCase())) {
                int frequency = entry.getValue();

                // check if first letter is capitalized
                String originalName = username.substring(
                        lowerCaseUsername.indexOf(name.toLowerCase()),
                        lowerCaseUsername.indexOf(name.toLowerCase()) + name.length());
                if (!originalName.substring(0, 1)
                        .equals(originalName.substring(0, 1).toLowerCase())) {
                    frequency = frequency * 5;
                }
                if (username.endsWith(originalName)) {
                	frequency = frequency * 10;
                } else {
                	String character = username.substring(username.indexOf(originalName) + originalName.length(), 
            				username.indexOf(originalName) + originalName.length() + 1);
                	if (character.toUpperCase().equals(character)) {
                		frequency = frequency * 10;
                	}
                }
                if (frequency > 5) {
                	System.out.println(name + " " + frequency);
                }
                if (bestName == null) {
                    bestName = name;
                    bestFrequency = frequency;
                    continue;
                }
                if (frequency > bestFrequency) {
                    bestName = name;
                    bestFrequency = frequency;
                }
            }
        }
        // bunch of junk
        if (bestFrequency < 400 || bestName == null) {
        	if (username.substring(0, 3).contains(" ")) {
        		return username.substring(0, 1).toUpperCase() + 
                        username.substring(1, username.length()).toLowerCase().replace("_", " ").replace(" ", "");
        	}
        	if (username.contains(" ")) {
                username = username.substring(0, username.indexOf(" ")).replace(" ", "");
                username = username.substring(0, 1).toUpperCase() + username.substring(1, username.length());
                if (username.replaceAll("[A-Z]", "").length() < username.length() * 3 / 4) {
                    username = username.substring(0, 1) + 
                            username.substring(1, username.length()).toLowerCase();
                } else {
                    username = username.substring(0, 1) + 
                            username.substring(1, username.length()).replaceAll("[A-Z]", " ");
                }
                if (username.contains(" ")) {
                    username = username.substring(0, username.indexOf(" "));
                }
                return username.replace(" ", "");
            }
        	if (username.replaceAll("[A-Z]", "").length() < username.length() * 3 / 4) {
                username = username.substring(0, 1).toUpperCase() + 
                            username.substring(1, username.length()).toLowerCase().replace("_", " ");
            } else {
                username = username.replace("_", "").substring(0, 1).toUpperCase() + 
                        username.substring(1, username.length()).replaceAll("[A-Z]", " ").replace("_", " ");
            }
            if (username.contains(" ") && username.indexOf(" ") > 3) {
                username = username.substring(0, username.indexOf(" "));
            }

            return username.replace(" ", "");
        }
        return bestName;
    }

    public static String substringUserID(String given) {
        return given.replace("<", "").replace(">", "").replace("@", "").replace("!", "");
    }

    public static String replacePlaceholders(String message, MessageReceivedEvent event) {
        Member member = event.getMember();
        if (message.contains("{user}")) {
            String name = member.getNickname();
            if (name == null) {
                name = member.getUser().getName();
            }
            if (name.toLowerCase().contains("botboi")) {
                name = "name copier";
            }
            message = message.replace("{user}", name);
        }
        if (message.contains("{user-rank}")) {
            message = message.replace("{user-rank}",
                    getBestRole(member).getName());
        }
        if (message.contains("{message}")) {
            message = message.replace("{message}", event.getMessage().getContentRaw());
        }
        if (message.contains("{real-name}")) {
            String name = member.getNickname();
            if (name == null) {
                name = member.getUser().getName();
            }
            String realName = getBestName(name);
            if (name.toLowerCase().contains("botboi")) {
                realName = "name copier";
            }
            message = message.replace("{real-name}", realName);
        }
        while (message.contains("{real-name-address}")) {
            String sent = event.getMessage().getContentRaw();
            if (!sent.contains("address:")) {
                break;
            }
            sent = sent.substring(sent.indexOf(":") + 1, sent.length());
            message = message.replace("{real-name-address}", getBestName(sent));
        }
        if (message.contains("{online-count}")) {
            Guild guild = event.getMessage().getGuild();
            int amountOnlineOrIdle = 0;
            for (Member otherMember : guild.getMembers()) {
                if (otherMember.getOnlineStatus().equals(OnlineStatus.ONLINE) ||
                        otherMember.getOnlineStatus().equals(OnlineStatus.IDLE)) {
                    amountOnlineOrIdle++;
                }
            }
            message = message.replace("{online-count}", String.valueOf(amountOnlineOrIdle));
        }
        if (message.contains("{staff-random}")) {
            Guild guild = event.getMessage().getGuild();
            Role botRole = getBestRole(guild.getMember(event.getJDA().getSelfUser()));
            List<Member> staff = new ArrayList<>();
            for (Member otherMember : guild.getMembers()) {
                if (!otherMember.getOnlineStatus().equals(OnlineStatus.ONLINE) &&
                        !otherMember.getOnlineStatus().equals(OnlineStatus.IDLE)) {
                    continue;
                }
                if (getBestRole(otherMember).compareTo(botRole) > 0) {
                    staff.add(otherMember);
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

    public static void getMessagesFromGoogle(Collection<SearchWord> lookFors) {
        SpreadsheetService service = new SpreadsheetService("Sheet1");

        for (SearchWord lookFor : lookFors) {
            allMessages.put(lookFor.getSearchWord(), new ArrayList<>());
            for (String synonym : lookFor.getSynonyms()) {
                allMessages.put(synonym, new ArrayList<>());
            }
        }

        try {
            String sheetUrl = BotBoiServer.getInstance().getSettings().getProperty("sheet.url");
            URL url = new URL(sheetUrl);

            ListFeed lf = service.getFeed(url, ListFeed.class);
            for (ListEntry le : lf.getEntries()) {
                CustomElementCollection cec = le.getCustomElements();
                for (SearchWord lookFor : lookFors) {
                    String removedSpaces = lookFor.getSearchWord().replace(" ", "");
                    String message = cec.getValue(removedSpaces);
                    if (message == null) {
                        continue;
                    }
                    allMessages.get(lookFor.getSearchWord()).add(message);
                    for (String synonym : lookFor.getSynonyms()) {
                        allMessages.get(synonym).add(message);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getMessage(String sent) {
        String longest = "";

        for (Entry<String, List<String>> entry : messages.entrySet()) {
            String lookFor = entry.getKey();
            if (lookFor.equals("else")) {
                continue;
            }
            if (messageContainsWord(lookFor, sent)) {
                if (lookFor.length() > longest.length()) {
                    longest = lookFor;
                }
            }
        }

        if (longest.equals("")) return returnNewMessage("else");

        return returnNewMessage(longest);
    }

    public static boolean messageContainsWord(String searchWord, String originalMessage) {
        String message = originalMessage.toLowerCase().replace("'", "");
        String word = searchWord.toLowerCase().replace("'", "");

        if (!message.contains(word)) return false;
        if (!message.contains(" " + word) &&
                !message.contains(">" + word) &&
                !message.contains(word + " ") &&
                !message.contains(word + "<")) return false;

        int msgIndex = message.indexOf(word);

        String wordPlusOne = word;
        String wordMinusOne = word;
        if (msgIndex + word.length() + 1 < message.length()) {
            wordPlusOne = message.substring(msgIndex, msgIndex + word.length() + 1);
        }
        if (msgIndex - 1 >= 0) {
            wordMinusOne = message.substring(msgIndex - 1, msgIndex + word.length());
        }

        if (!wordPlusOne.equals(word + " ") &&
                !wordPlusOne.equals(word) &&
                !wordPlusOne.equals(word + "<")) return false;
        if (!wordMinusOne.equals(" " + word) &&
                !wordMinusOne.equals(">" + word) &&
                !wordMinusOne.equals(word)) return false;

        return true;
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
