package com.sap.demo.log;

import java.util.UUID;

public class LoggingThreadContext {
	
	private static final ThreadLocal<LoggingThreadConfig> loggingSetup = new ThreadLocal<LoggingThreadConfig>() {
		
		@Override
		protected LoggingThreadConfig initialValue() {
			return new LoggingThreadConfig();
		};
	};
	
	private static class LoggingThreadConfig {
		private String key = UUID.randomUUID().toString();
		
		private LoggingThreadConfig() {}
		
		private String getKey() {
			return key;
		}
		
		private void setKey(String key) {
			this.key = key;
		}		
	}
	
	public static void setLoggingKey(String key) {
		loggingSetup.get().setKey(key);		
	}
	
	public static String getLoggingKey() {		
		return loggingSetup.get().getKey();
	}

	
	public static void innitialize() {
		loggingSetup.remove();
	}
	
	

}
