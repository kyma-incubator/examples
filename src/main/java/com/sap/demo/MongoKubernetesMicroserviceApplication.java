package com.sap.demo;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.demo.event.EventBridge;
import com.sap.demo.event.MongoChangeEvent;


@SpringBootApplication
@Configuration
@EnableCaching
public class MongoKubernetesMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MongoKubernetesMicroserviceApplication.class, args);
	}	
	
	
	
	@ConditionalOnMissingBean()
	@Bean
	public EventBridge eventBridge() {
		return new EventBridge() {
			
			
			@Override
			public void writeMongoChangeEvent(MongoChangeEvent event) {
				
				if (event.getType() == MongoChangeEvent.DELETE) {
					System.out.println(String.format("Person %s deleted", event.getEventData().toJson()));
				} else  {
					System.out.println(String.format("Person %s maintained", event.getEventData().toJson()));
				}
			}
		};
	}
}
