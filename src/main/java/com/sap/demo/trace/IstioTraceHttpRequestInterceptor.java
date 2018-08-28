package com.sap.demo.trace;

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.springframework.stereotype.Component;

@Component("IstioTraceInterceptor")
public class IstioTraceHttpRequestInterceptor implements HttpRequestInterceptor {

	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
		
		
		for (Entry<String, String> headerEntry : TraceContext.getHeaders().entrySet()) {
			request.setHeader(headerEntry.getKey(), headerEntry.getValue());
		}	

	}

}
