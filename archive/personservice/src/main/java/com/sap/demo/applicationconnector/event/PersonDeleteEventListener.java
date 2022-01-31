package com.sap.demo.applicationconnector.event;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sap.demo.event.PersonDeleteEvent;

@Component
@Profile("ApplicationConnector")
public class PersonDeleteEventListener 
	extends AbstractApplicationConnectorEventBridge 
	implements ApplicationListener<PersonDeleteEvent> {

	@Override
	public void onApplicationEvent(PersonDeleteEvent event) {		
		this.writePersonEvent(event);		
	}

}
