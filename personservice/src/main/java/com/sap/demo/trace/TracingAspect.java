package com.sap.demo.trace;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.demo.log.NoLogging;
import com.sap.demo.util.ReflectionUtils;

import brave.Span;
import brave.Span.Kind;
import brave.Tracer;

@Component
@Aspect
public class TracingAspect {
	
	private final Tracer tracer;

    @Autowired
	public TracingAspect(Tracer tracer) {
        this.tracer = tracer;
    }
	
	
	@Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)"
			+ " || @annotation(org.springframework.web.bind.annotation.PostMapping)"
			+ " || @annotation(org.springframework.web.bind.annotation.RequestMapping)"
			+ " || @annotation(org.springframework.web.bind.annotation.DeleteMapping)"
			+ " || @annotation(org.springframework.web.bind.annotation.PutMapping)"
			+ " || @annotation(org.springframework.web.bind.annotation.PatchMapping)")
	public void traceMethod() {
	}
	
	@Around("traceMethod()")
	@NoLogging
	public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
		
		
		Span span;
		if((span = tracer.currentSpan()) == null) {
			span = tracer.currentSpan()
					.name(joinPoint.getSignature().toShortString())
					.kind(Kind.SERVER)
					.start();
		}
		
		try {
			span.tag("call", joinPoint.getSignature().toLongString());
			
			Object result = joinPoint.proceed();
			span.finish();
			return result;
			
		} catch(Exception e) {
			
			span.tag("Exception", e.getClass().getName());
			
			span.tag("ArgumentValues", ReflectionUtils.convertArgumentArrayToString(joinPoint.getArgs()));
			span.error(e);
			span.finish();
			
			throw e;
				
		}
		
		
		
	}
	
}
