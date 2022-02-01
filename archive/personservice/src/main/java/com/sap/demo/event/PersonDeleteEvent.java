package com.sap.demo.event;

import org.springframework.context.ApplicationEvent;

public class PersonDeleteEvent extends ApplicationEvent implements PersonEvent {
	
	private static final long serialVersionUID = 7485258772054118891L;
	
	
	public PersonDeleteEvent(String source) {
		super(source); 
	}

	@Override
	public String getPersonId() {		
		return (String)this.getSource();
	}	

}
