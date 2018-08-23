package com.sap.demo.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

import com.sap.demo.entity.Person;

@Component
public class MongoPersonListener extends AbstractMongoEventListener<Person> {
	
	private EventBridge eventBridge;
	
	@Autowired
	public void setEventBridge(EventBridge eventBridge) {
		this.eventBridge = eventBridge;
	}
	
	@Override	
	public void onAfterDelete(AfterDeleteEvent<Person> event) {
		eventBridge.writeMongoChangeEvent(
				new MongoChangeEvent(event.getCollectionName(), MongoChangeEvent.DELETE, 
						event.getDocument()));	
	}
	@Override
	public void onAfterSave(AfterSaveEvent<Person> event) {
		eventBridge.writeMongoChangeEvent(
				new MongoChangeEvent(event.getCollectionName(), MongoChangeEvent.SAVE, 
						event.getDocument()));		
	}
	
	
}
