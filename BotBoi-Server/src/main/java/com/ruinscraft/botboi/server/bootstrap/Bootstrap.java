package com.ruinscraft.botboi.server.bootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.ruinscraft.botboi.server.BotBoiServer;

public class Bootstrap {

	private static Scanner inputScanner = new Scanner(System.in);

	public static void main(String[] args) {
		// load the settings
		Properties settings = new Properties();
		File settingsFile = new File("server.properties");
		Map<String, Integer> names = new HashMap<>();

		try {
			Properties defaults = new Properties();

			InputStream is = Thread.currentThread()
					.getContextClassLoader().getResourceAsStream("server.properties");

			defaults.load(is);

			if (!settingsFile.exists()) {
				settingsFile.createNewFile();
			}

			settings.load(new FileInputStream("server.properties"));

			for (Map.Entry<Object, Object> defaultValue : defaults.entrySet()) {
				settings.putIfAbsent(defaultValue.getKey(), defaultValue.getValue());
			}

			settings.store(new FileOutputStream("server.properties"), null);

			for (String name : Files.lines(FileSystems.getDefault().getPath("names.txt"))
					.collect(Collectors.toSet())) {
				name = name.replace(",F,", " ");
				name = name.replace(",M,", " ");
				String frequency = name.substring(name.indexOf(" "), name.length());
				name = name.replace(frequency, "");
				if (name.length() < 3) {
					continue;
				}
				frequency = frequency.replace(" ", "");
				int frequencyInt = 0;
				try {
					frequencyInt = Integer.valueOf(frequency);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
				frequencyInt = (int) Math.pow(frequencyInt, name.length());
				names.put(name, frequencyInt);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		BotBoiServer server = new BotBoiServer(settings, names);

		// start the server safely
		try {
			server.run();
		} catch (Exception e) {
			e.printStackTrace();
		}

		new Thread(()->{
			while (inputScanner.hasNextLine()) {
				String input = inputScanner.nextLine();

				if (input.equalsIgnoreCase("stop")) {
					server.shutdown();
				}
			}
		}).start();
	}

}