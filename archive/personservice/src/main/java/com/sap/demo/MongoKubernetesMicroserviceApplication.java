package com.sap.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.sap.demo.event.PersonChangeEvent;
import com.sap.demo.event.PersonCreateEvent;
import com.sap.demo.event.PersonDeleteEvent;

@SpringBootApplication
@Configuration
@EnableCaching
@EnableMongoRepositories(basePackages = "com.sap.demo")
public class MongoKubernetesMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MongoKubernetesMicroserviceApplication.class, args);
	}		
	
	@Bean
	@Profile("!ApplicationConnector")
	public ApplicationListener<PersonCreateEvent> personCreateEvent() {
		return new ApplicationListener<PersonCreateEvent>() {
						
			@Override
			public void onApplicationEvent(PersonCreateEvent event) {
				System.out.println(
						String.format("Person with ID %s created", event.getPersonId()));
			}
		};
		
	}
	
	@Bean
	@Profile("!ApplicationConnector")
	public ApplicationListener<PersonDeleteEvent> personDeleteEvent() {
		return new ApplicationListener<PersonDeleteEvent>() {
						
			@Override
			public void onApplicationEvent(PersonDeleteEvent event) {
				System.out.println(
						String.format("Person with ID %s deleted", event.getPersonId()));
			}
		};
		
	}
	
	@Bean
	@Profile("!ApplicationConnector")
	public ApplicationListener<PersonChangeEvent> personChangeEvent() {
		return new ApplicationListener<PersonChangeEvent>() {
						
			@Override
			public void onApplicationEvent(PersonChangeEvent event) {
				System.out.println(
						String.format("Person with ID %s changed", event.getPersonId()));
			}
		};
		
	}
	
}
