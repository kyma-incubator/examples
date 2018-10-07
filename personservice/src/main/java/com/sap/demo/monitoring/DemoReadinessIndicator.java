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

import lombok.Data;

@Component
@WebEndpoint(id="ready",enableByDefault=true)
@Profile("Actuator")
@Data
public class DemoReadinessIndicator implements ApplicationListener<ApplicationReadyEvent>{
	
	// Default to Max value and change to current time after Application Ready Event
	private long notReadyTimeMills = Long.MAX_VALUE;
	
	@ReadOperation(produces="application/json")
	public ResponseEntity<Map<String, Boolean>> getReadinessStatus() {
		
		if(System.currentTimeMillis() > notReadyTimeMills) {
			return new ResponseEntity<Map<String,Boolean>>(Collections.singletonMap("isReady", true),
					HttpStatus.OK);
		} else {
			return new ResponseEntity<Map<String,Boolean>>(Collections.singletonMap("isReady", false),
					HttpStatus.SERVICE_UNAVAILABLE);
		}
		
		
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		notReadyTimeMills = System.currentTimeMillis();
	}

}
