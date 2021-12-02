package com.novetta.clavin.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class StaticProperties {
	private static Properties properties = new Properties();
	static {
		try {
			StaticProperties.load("properties/default.config");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private StaticProperties() {}
	
	public static void load(String configFilePath) throws IOException {
		properties.load(new FileInputStream(configFilePath));
	}
	
	public static String get(String propertyName) {
		return properties.getProperty(propertyName);
	}
}
