package com.sap.demo.trace;

import java.util.HashMap;
import java.util.Map;


public  class TraceContext {
	
	private static final ThreadLocal<Map<String, String>> traceHeaders = 
			new ThreadLocal<Map<String, String>>() {
		@Override
		protected Map<String, String> initialValue() {
			return new HashMap<String, String>();
		};
	};
	
	
	
	public static void setHeaders(Map<String, String> headers) {
		traceHeaders.set(new HashMap<String, String>(headers));
	}
	
	public static void setHeader(String headerName, String headerValue) {
		traceHeaders.get().put(headerName, headerValue);
	}
	
	public static Map<String, String> getHeaders() {
		return new HashMap<String, String>(traceHeaders.get());
	}
	
	public static String getHeaderValue(String headerName) {
		return traceHeaders.get().get(headerName);
	}
	
	public static boolean containsHeader(String headerName) {
		return traceHeaders.get().containsKey(headerName); 
	}
	
	public static void innitialize() {
		traceHeaders.remove();
	}

}
