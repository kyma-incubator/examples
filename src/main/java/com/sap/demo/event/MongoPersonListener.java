package com.sap.demo.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.sap.demo.entity.Person;

@Component
public class MongoPersonListener extends AbstractMongoEventListener<Person> {

	@Autowired
	private KafkaTemplate<String, KafkaMongoEvent> kafkaTemplate;
	
	@Autowired
	private KafkaTemplate<String, String> kafkaTemplateString;
	
	@Value("${kafka.topic.json}")
	private String topicName;
	
	@Value("${kafka.topic.text}")
	private String textTopicName;
	
	@Override
	public void onBeforeDelete(BeforeDeleteEvent<Person> event) {
		
		kafkaTemplateString.send(textTopicName, 
				String.format("Deleting Person %s", event.getDocument().toJson()));	
		
		
		
		kafkaTemplate.send(topicName, 
				new KafkaMongoEvent(event.getCollectionName(), KafkaMongoEvent.DELETE, 
						event.getDocument()));
		
	}
	
	@Override
	public void onBeforeSave(BeforeSaveEvent<Person> event) {
		kafkaTemplateString.send(textTopicName, String.format("Saving Person %s", event.getDocument().toJson()));		
		kafkaTemplate.send(topicName, 
				new KafkaMongoEvent(event.getCollectionName(), KafkaMongoEvent.SAVE, 
						event.getDocument()));
	}	
}
