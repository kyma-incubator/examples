package com.sap.demo.applicationconnector.event;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sap.demo.event.PersonChangeEvent;

@Component
@Profile("ApplicationConnector")
public class PersonChangeEventListener 
	extends AbstractApplicationConnectorEventBridge 
	implements ApplicationListener<PersonChangeEvent> {

	@Override
	public void onApplicationEvent(PersonChangeEvent event) {		
		this.writePersonEvent(event);		
	}

}
