package com.sap.demo.event;

public interface EventBridge {
	
	public void writeMongoChangeEvent(MongoChangeEvent event);

}
