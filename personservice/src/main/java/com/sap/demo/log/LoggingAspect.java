package com.sap.demo.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
@Component
public class LoggingAspect {

	private final static ToStringStyle LOGGER_STYLE = new RecursiveToStringStyle();
	

	@Pointcut("within(com.sap.demo..*) && !@annotation(com.sap.demo.log.NoLogging)")
	public void loggableClass() {
	}
	
	
	

	private String objectToString(Object o) {
		
		try {
			
			if (o.getClass().getMethod("toString").getDeclaringClass() == o.getClass()) {
				return o.toString();
			} else {						
				return ReflectionToStringBuilder.toString(o, LOGGER_STYLE, true, true);
			}
			
		// Ignore Exception
		} catch (NoSuchMethodException e) {
			return "No Result";
		} catch (SecurityException e) {
			return "Not Accessible";
		} 
		
	}

	private String convertArgumentArrayToString(Object[] arguments) {

		String result = "";

		if (arguments != null) {

			result = Arrays.stream(arguments)
					.map(argument -> String.format("%s: %s", argument != null ? argument.getClass() : "null",
							argument != null ? objectToString(argument) : "null"))
					.collect(Collectors.joining("\n"));

		}

		return result;
	}
	
	

	@Around("loggableClass()")
	public Object log(ProceedingJoinPoint joinPoint) throws Throwable {

		// check if class was generated, if yes don't do anything, otherwise log
		if (joinPoint.getTarget().getClass().isSynthetic()) {
			return joinPoint.proceed();	
		}else {
			final Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
			try {
				if(log.isTraceEnabled()) {
					String methodName = joinPoint.getSignature().toLongString();
					String argumentString = convertArgumentArrayToString(joinPoint.getArgs());

					String traceTrace = String.format("Entering %s with Arguments:\n%s", methodName, argumentString);
					
					log.trace(traceTrace);					
				} else if (log.isDebugEnabled()) {

					String methodName = joinPoint.getSignature().toShortString();
					String debugTrace = String.format("Entering %s", methodName);

					log.debug(debugTrace);
				}
				

				Object result = joinPoint.proceed();
				
				if(log.isTraceEnabled() ) {
					String methodName = joinPoint.getSignature().toShortString();

					String traceTrace = String.format("Exiting %s with result: %s", methodName,
							result != null ? 
									objectToString(result) : 
									"null");
					
					log.trace(traceTrace);
					
					
				} else if (log.isDebugEnabled()) {
				
					String methodName = joinPoint.getSignature().toShortString();
					String debugTrace = String.format("Exiting %s", methodName);
					log.debug(debugTrace);
				}

				return result;
			} catch (Throwable t) {
				
				if (log.isErrorEnabled()) {
					String methodName = joinPoint.getSignature().toShortString();
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					t.printStackTrace(pw);
					String stackTrace = sw.toString();

					String errorTrace = String.format("Catching %s in method %s" + "\nMessage: %s\nStackTrace: %s",
							t.getClass().getName(), methodName, t.getMessage(), stackTrace);
					log.error(errorTrace);
				}
				throw t;
			}
		}
	}

}
