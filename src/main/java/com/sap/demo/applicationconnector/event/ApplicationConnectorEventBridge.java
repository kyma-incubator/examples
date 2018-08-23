package com.sap.demo.applicationconnector.event;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.demo.event.EventBridge;
import com.sap.demo.event.MongoChangeEvent;
import com.sap.demo.exception.PersonServiceException;

import lombok.AllArgsConstructor;
import lombok.Data;

@Profile("ApplicationConnector")
@Component
public class ApplicationConnectorEventBridge implements EventBridge{
	
	private static final String DELETE_EVENT = "person.deleted";
	private static final String MAINTAIN_EVENT = "person.maintained";
	private static final String VERSION = "v1";
	
	private RestTemplate restTemplate;
	
	@Autowired
	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Override	
	public void writeMongoChangeEvent(MongoChangeEvent event) {
		
		KymaEvent kymaEvent;
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		
		
		if(event.getType() == MongoChangeEvent.DELETE) {
			kymaEvent = new KymaEvent(DELETE_EVENT, VERSION, df.format(new Date()), 
					 Collections.singletonMap("personid", 
							 event.getEventData().get("_id").toString()));
		} else {
			kymaEvent = new KymaEvent(MAINTAIN_EVENT, VERSION, df.format(new Date()), 
					 Collections.singletonMap("personid", 
							 event.getEventData().get("_id").toString()));			
		}
		
		
		ResponseEntity<String> response =  restTemplate.exchange
				("/v1/events", HttpMethod.POST, new HttpEntity<KymaEvent>(kymaEvent)
				, String.class);
		
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new PersonServiceException("Event replication to Kyma unsuccessful");
		}
		
	}
	
	
	@Data
	@AllArgsConstructor
	private static class KymaEvent{
		@JsonProperty("event-type")
		private String eventType;
		
		@JsonProperty("event-type-version")
		private String eventTypeVersion;
		
		@JsonProperty("event-time")
		private String eventTime;
		
		
		private Map<String, Object> data;
	}
	
	
}

