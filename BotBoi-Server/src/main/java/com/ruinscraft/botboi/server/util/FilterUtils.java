package com.ruinscraft.botboi.server.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FilterUtils {

    private static Map<Character, Character> replacements;
    private static List<Character> singleLetterWords;

    static {
        // replacements
        replacements = new HashMap<>();

        replacements.put('@', 'a');
        replacements.put('$', 's');
        replacements.put('3', 'e');
        replacements.put('1', 'i');
        replacements.put('!', 'i');
        replacements.put('|', 'i');

        // single letter words
        singleLetterWords = new ArrayList<>();

        singleLetterWords.add('a');
        singleLetterWords.add('i');
    }

    public static boolean isBadMessage(List<String> badWords, String msg) {
        msg = msg.toLowerCase();

        boolean flag = false;

        String[] split = msg.split(" ");

        for (int i = 0; i < split.length; i++) {
            if (split[i].length() == 1
                    && !singleLetterWords.contains(split[i].charAt(0))) {
                flag = true;
            }

            if (badWords.contains(split[i])) {
                return true;
            }
        }

        if (flag) {
            String combined = msg.replace(" ", "");
            for (String badWord : badWords) {
                if (combined.contains(badWord)) {
                    return true;
                }
            }
        }

        // recursive check for replacements
        for (char c : replacements.keySet()) {
            if (msg.contains(Character.toString(c))) {
                return isBadMessage(badWords, msg.replace(c, replacements.get(c)));
            }
        }

        return false;
    }

}