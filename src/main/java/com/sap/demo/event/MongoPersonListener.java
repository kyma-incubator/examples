package com.sap.demo.event;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

import com.sap.demo.entity.Person;

@Component
public class MongoPersonListener extends AbstractMongoEventListener<Person> {
	
	
	@Override
	public void onBeforeDelete(BeforeDeleteEvent<Person> event) {
		
		System.out.println(String.format("Deleting Person %s", event.getDocument().toJson()));
		
	}
	
	@Override
	public void onBeforeSave(BeforeSaveEvent<Person> event) {
		
		System.out.println(String.format("Saving Person %s", event.getDocument().toJson()));
		
	}	
}
