package com.sap.demo.monitoring;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

@Component
public class HostnameInjectorFilter implements Filter {
	
	private String hostname;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
		hostname = System.getenv("HOSTNAME") == null ? "Environment Variable 'HOSTNAME' not set" : System.getenv("HOSTNAME");
	}

	

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}



	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletResponse httpServletResponse = (HttpServletResponse) response;
		httpServletResponse.setHeader("X-Serving-Host", hostname);
		
		chain.doFilter(request, response);
		
	}

}
