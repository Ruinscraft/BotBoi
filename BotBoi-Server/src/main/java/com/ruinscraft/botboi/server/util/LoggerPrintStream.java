package com.ruinscraft.botboi.server.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerPrintStream extends PrintStream {

	public LoggerPrintStream(OutputStream outputStream) {
		super(outputStream);
	}

	@Override
	public void println(Object object) {
		log(object);
	}

	@Override
	public void println(String string) {
		log(string);
	}

	private String getTimePrefix() {
		String dateAndTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		dateAndTime = dateAndTime.replace("T", " ");
		if (dateAndTime.contains(".")) dateAndTime = dateAndTime.substring(0, dateAndTime.indexOf("."));
		String prefix = "[LOG " + dateAndTime + "] ";
		return prefix;
	}

	private void log(Object object) {
		super.println(getTimePrefix() + object);
	}

}
