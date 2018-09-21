package com.sap.demo.monitoring.rest;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sap.demo.monitoring.DemoHealthIndicator;
import com.sap.demo.monitoring.DemoReadinessIndicator;

import io.swagger.annotations.ApiOperation;

@RestController
@Profile("Actuator")
public class MonitoringAPI {
	
	
	private DemoHealthIndicator healthIndicator;
	private DemoReadinessIndicator readinessIndicator;
	
	@Autowired
	public void setHealthIndicator(DemoHealthIndicator healthIndicator) {
		this.healthIndicator = healthIndicator;
	}
	
	@Autowired
	public void setReadinessIndicator(DemoReadinessIndicator readinessIndicator) {
		this.readinessIndicator = readinessIndicator;
	}
	
	
	@PostMapping("/api/v1/monitoring/health")
	@ApiOperation(value="Set Health Indicator Status", notes="This function set the health indicator status (Spring Boot Actuator)")
	public ResponseEntity<Map<String,String>> setHealthIndicator(
			@RequestParam(defaultValue="true", name="isUp") boolean isUp) {
		
		if(isUp) {
			healthIndicator.setHealthy();
		} else {
			healthIndicator.setUnhealthy();
		}
		
		return new ResponseEntity<Map<String,String>>(
				Collections.singletonMap("HealthStatus", String.valueOf(isUp)),
				HttpStatus.ACCEPTED);		
	}
	
	@PostMapping("/api/v1/monitoring/readiness")
	@ApiOperation(value="Set Readiness Indicator Status", notes="This function set the readiness indicator status (Spring Boot Actuator)")
	public ResponseEntity<Map<String,String>> setReadinessIndicator(
			@RequestParam(defaultValue="true", name="isReady") boolean isReady) {
		
		if(isReady) {
			readinessIndicator.setReady();
		} else {
			readinessIndicator.setNotReady();
		}
		
		return new ResponseEntity<Map<String,String>>(
				Collections.singletonMap("ReadynessStatus", String.valueOf(isReady)),
				HttpStatus.ACCEPTED);		
	}
}
