package com.sap.demo.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import com.sap.demo.entity.Person;
import com.sap.demo.repository.PersonRepository;

@Service
public class PersonService {
	
	@Autowired
	private PersonRepository repository;
	

	public Person savePerson(Person person) {
		
		return repository.save(person);
	}
	
	public Iterable<Person> listPersons() {
		
		return repository.findAll();
	}
	
	public Person findPerson(String id) {	
		Optional<Person> result = repository.findById(id);
		
		return result.isPresent() ? result.get() : null;
	}	
	
	public void deletePerson(String id) {
		repository.deleteById(id);
	}	
	
	public Iterable<Person> search(Person person) {		
		return repository.findAll(Example.of(person));
	}

}
