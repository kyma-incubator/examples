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
		private boolean warnDebugEnabled = false;		
		
		private LoggingThreadConfig() {}
		
		private String getKey() {
			return key;
		}
		
		private void setKey(String key) {
			this.key = key;
		}
		
		private boolean isWarnDebugEnabled() {
			return warnDebugEnabled;
		}
		
		private void setWarnDebugEnabled(boolean warnDebugEnabled) {
			this.warnDebugEnabled = warnDebugEnabled;
		}
		
		
	}
	
	public static void setLoggingKey(String key) {
		loggingSetup.get().setKey(key);		
	}
	
	public static String getLoggingKey() {		
		return loggingSetup.get().getKey();
	}
	
	public static boolean isWarnLoggingEnabled() {
		return loggingSetup.get().isWarnDebugEnabled();
	}
	
	public static void setWarnLoggingEnabled(boolean warnDebugEnabled) {
		loggingSetup.get().setWarnDebugEnabled(warnDebugEnabled);
	}
	
	public static void removeLoggingKey() {
		loggingSetup.remove();
	}
	
	

}
