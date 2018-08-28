package com.sap.demo.trace;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.sap.demo.log.NoLogging;


@Component
public class IstioHeaderExtractor implements Filter {
	
	private static final Set<String> ISTIO_HEADERS = new HashSet<String>(
			Arrays.asList("x-request-id", "x-b3-traceid", "x-b3-spanid", 
		"x-b3-parentspanid", "x-b3-sampled", "x-b3-flags", "x-ot-span-context"));
	

	@Override
	public void destroy() {
		//Nothing

	}

	@Override
	@NoLogging
	public void doFilter(ServletRequest request, 
			ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		
		TraceContext.innitialize();
		
		String currentHeaderName;
		
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		
		Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
		
		while (headerNames.hasMoreElements()) {
			currentHeaderName =  headerNames.nextElement();
			
			if(ISTIO_HEADERS.contains(currentHeaderName)) {
				TraceContext.setHeader(currentHeaderName, 
						httpServletRequest.getHeader(currentHeaderName));
			}
			
		}
		
		filterChain.doFilter(request, response);		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		//Nothing

	}

}
