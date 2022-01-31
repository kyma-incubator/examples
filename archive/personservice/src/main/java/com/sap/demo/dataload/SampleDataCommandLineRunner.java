package com.sap.demo.dataload;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sap.demo.entity.Person;
import com.sap.demo.repository.PersonRepository;

@Component
public class SampleDataCommandLineRunner implements CommandLineRunner {
	
	
	@Autowired
	private PersonRepository personRepository;

	@Override
	public void run(String... args) throws Exception {		
				
		if (personRepository.count() <= 0L) {
			personRepository.save(new Person(null, "John", "Doe", 
					"Nymphenburger Str.", "86", "80636", "Muenchen", 
					Collections.singletonMap("countryIso2", "DE")));
		}
		
	}

}
