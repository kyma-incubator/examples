package com.sap.demo.monitoring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


@Component
@Profile("Actuator")
public class DemoHealthIndicator implements HealthIndicator {
	
	private boolean isHealthy = true;
	
	public void setUnhealthy() {
		isHealthy = false;
	}
	
	public void setHealthy() {
		isHealthy = true;
	}
	
	@Override
	public Health health() {
		
		return isHealthy ? 
				Health.up().withDetail("Healthy status set via API: ", isHealthy).build() :
				Health.down().withDetail("Healthy status set via API: ", isHealthy).build();
				
	}

}
