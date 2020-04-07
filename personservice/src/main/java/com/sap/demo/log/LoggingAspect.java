package com.sap.demo.log;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sap.demo.util.ReflectionUtils;


@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
@Component
public class LoggingAspect {

	

	@Pointcut("within(com.sap.demo..*) && !@annotation(com.sap.demo.log.NoLogging)")
	public void loggableClass() {
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
					String argumentString = ReflectionUtils.convertArgumentArrayToString(joinPoint.getArgs());

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
									ReflectionUtils.objectToString(result) : 
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
