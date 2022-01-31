package com.sap.demo.event;

import org.springframework.context.ApplicationEvent;

import com.sap.demo.entity.Person;

public class PersonCreateEvent extends ApplicationEvent implements PersonEvent {
	
	private static final long serialVersionUID = 7485258772054118761L;
	
	
	public PersonCreateEvent(Person source) {
		super(source); 
	}

	@Override
	public String getPersonId() {		
		return ((Person)this.getSource()).getId();
	}	

}
