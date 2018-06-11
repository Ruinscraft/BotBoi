package com.ruinscraft.botboi.server.bootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import com.ruinscraft.botboi.server.BotBoiServer;

public class Bootstrap {

	private static Scanner inputScanner = new Scanner(System.in);

	public static void main(String[] args) {
		// load the settings
		Properties settings = new Properties();
		File settingsFile = new File("server.properties");

		try {
			Properties defaults = new Properties();

			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("server.properties");

			defaults.load(is);

			if (!settingsFile.exists()) {
				settingsFile.createNewFile();
			}

			settings.load(new FileInputStream("server.properties"));

			for (Map.Entry<Object, Object> defaultValue : defaults.entrySet()) {
				settings.putIfAbsent(defaultValue.getKey(), defaultValue.getValue());
			}

			settings.store(new FileOutputStream("server.properties"), null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		BotBoiServer server = new BotBoiServer(settings);
		
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
