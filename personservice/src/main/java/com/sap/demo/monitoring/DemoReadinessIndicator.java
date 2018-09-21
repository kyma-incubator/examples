package com.sap.demo.monitoring;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@WebEndpoint(id="ready",enableByDefault=true)
@Profile("Actuator")
public class DemoReadinessIndicator implements ApplicationListener<ApplicationReadyEvent>{
	
	private boolean isReady = false;
	
	public void setNotReady() {
		isReady = false;
	}
	
	public void setReady() {
		isReady = true;
	}
	
	@ReadOperation(produces="application/json")
	public ResponseEntity<Map<String, Boolean>> getReadinessStatus() {
		
		if(isReady) {
			return new ResponseEntity<Map<String,Boolean>>(Collections.singletonMap("isReady", isReady),
					HttpStatus.OK);
		} else {
			return new ResponseEntity<Map<String,Boolean>>(Collections.singletonMap("isReady", isReady),
					HttpStatus.SERVICE_UNAVAILABLE);
		}
		
		
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		isReady = true;
	}

}
