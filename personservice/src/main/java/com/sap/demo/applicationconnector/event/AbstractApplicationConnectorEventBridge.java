package com.sap.demo.applicationconnector.event;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.demo.applicationconnector.util.ApplicationConnectorRestTemplateBuilder;
import com.sap.demo.event.PersonCreateEvent;
import com.sap.demo.event.PersonDeleteEvent;
import com.sap.demo.event.PersonEvent;
import com.sap.demo.exception.PersonServiceException;

import lombok.AllArgsConstructor;
import lombok.Data;

public abstract class AbstractApplicationConnectorEventBridge {

	private static final String DELETE_EVENT = "person.deleted";
	private static final String CREATE_EVENT = "person.created";
	private static final String CHANGE_EVENT = "person.changed";
	private static final String VERSION = "v1";

	private ApplicationConnectorRestTemplateBuilder restTemplateBuilder;

	@Autowired
	public void setRestTemplateBuilder(ApplicationConnectorRestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}

	protected void writePersonEvent(PersonEvent event) {

		KymaEvent kymaEvent;

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

		RestTemplate restTemplate = restTemplateBuilder.getEventEndpointRestTemplate();

		if (event instanceof PersonDeleteEvent) {
			kymaEvent = new KymaEvent(DELETE_EVENT, VERSION, df.format(new Date()),
					Collections.singletonMap("personid", event.getPersonId()));
		} else if (event instanceof PersonCreateEvent) {
			kymaEvent = new KymaEvent(CREATE_EVENT, VERSION, df.format(new Date()),
					Collections.singletonMap("personid", event.getPersonId()));
		} else {
			kymaEvent = new KymaEvent(CHANGE_EVENT, VERSION, df.format(new Date()),
					Collections.singletonMap("personid", event.getPersonId()));
		}

		ResponseEntity<String> response = restTemplate.exchange("/", HttpMethod.POST,
				new HttpEntity<KymaEvent>(kymaEvent), String.class);
		System.out.println("Eventid: " + event.getPersonId());
		System.out.println("Wrote event: " + response.getStatusCode() + response.getHeaders().toString() + response.getBody().toString());
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new PersonServiceException("Event replication to Kyma unsuccessful");
		}

	}

	@Data
	@AllArgsConstructor
	private static class KymaEvent {
		@JsonProperty("event-type")
		private String eventType;

		@JsonProperty("event-type-version")
		private String eventTypeVersion;

		@JsonProperty("event-time")
		private String eventTime;

		private Map<String, Object> data;
	}

}
