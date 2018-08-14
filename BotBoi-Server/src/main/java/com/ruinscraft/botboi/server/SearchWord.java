package com.ruinscraft.botboi.server;

import java.util.ArrayList;
import java.util.Collection;

public class SearchWord {

	private String searchWord;
	private Collection<String> synonyms;

	private SearchWord(String searchWord, String... synonyms) {
		this.searchWord = searchWord;
		this.synonyms = new ArrayList<>();
		for (String synonym : synonyms) {
			this.synonyms.add(synonym);
		}
	}

	public String getSearchWord() {
		return searchWord;
	}

	public Collection<String> getSynonyms() {
		return synonyms;
	}

	public boolean hasSynonyms() {
		return !synonyms.isEmpty();
	}

	public static SearchWord fromStringList(String firstWord, String... otherWords) {
		return new SearchWord(firstWord, otherWords);
	}

	public static SearchWord fromStringList(String... words) {
		if (words.length != 0) {
			String[] newWordsArray = new String[words.length - 1];
			for (int i = 1; i < words.length; i++) {
				newWordsArray[i - 1] = words[i];
			}
			return fromStringList(words[0], newWordsArray);
		}
		return null;
	}

	public static SearchWord fromFormattedList(String attached) {
		attached = attached.replace(";", "");
		String[] collection = attached.split(",");
		if (collection.length != 0) {
			return fromStringList(collection);
		}
		return null;
	}

}
