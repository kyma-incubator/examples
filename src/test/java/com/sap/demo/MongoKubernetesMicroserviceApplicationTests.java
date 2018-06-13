package com.sap.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import com.sap.demo.entity.Person;
import com.sap.demo.repository.PersonRepository;

@RunWith(SpringRunner.class)
@DataMongoTest
public class MongoKubernetesMicroserviceApplicationTests {
	
	@Autowired
	private PersonRepository personRepository;
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	private Person createDummyPerson() {
		return new Person(null, "Andreas", 
				"Krause", "Badenerstr.", "676", "8048", "Zurich", 
				Collections.singletonMap("countryIso2", "CH"));
	}
	
	@Test
	public void writeData() {
		
		Person dummy = createDummyPerson();
		
		Person personResult = personRepository.save(dummy).block();
		
		assertThat(personResult).isNotNull();
		assertThat(personResult.getId()).isNotNull().isNotEmpty();
		
		mongoTemplate.remove(personResult);
		
	}

}
