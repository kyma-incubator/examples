package com.sap.demo.applicationconnector.event;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sap.demo.event.PersonCreateEvent;

@Component
@Profile("ApplicationConnector")
public class PersonCreateEventListener 
	extends AbstractApplicationConnectorEventBridge 
	implements ApplicationListener<PersonCreateEvent> {

	@Override
	public void onApplicationEvent(PersonCreateEvent event) {		
		this.writePersonEvent(event);		
	}

}
