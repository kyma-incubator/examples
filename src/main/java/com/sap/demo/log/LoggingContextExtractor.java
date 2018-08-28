package com.sap.demo.log;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;


@Component
public class LoggingContextExtractor implements Filter {
	
	private static final String ISTIO_HEADER_REQUEST_ID = "x-request-id";
	

	@Override
	public void destroy() {
		//Nothing

	}

	@Override
	@NoLogging
	public void doFilter(ServletRequest request, 
			ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		
		LoggingThreadContext.innitialize();
		
		String currentHeaderName;
		
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		
		Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
		
		while (headerNames.hasMoreElements()) {
			currentHeaderName =  headerNames.nextElement();
			
			if(ISTIO_HEADER_REQUEST_ID.equalsIgnoreCase(currentHeaderName)) {
				LoggingThreadContext.setLoggingKey(httpServletRequest.getHeader(currentHeaderName));
			}
			
		}
		
		filterChain.doFilter(request, response);		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		//Nothing

	}

}
