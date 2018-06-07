package com.ruinscraft.botboi.server.bootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import com.ruinscraft.botboi.server.BotBoiServer;

public class Bootstrap {

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

		// start the server
		new BotBoiServer(settings).run();
	}

}
