package com.sap.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import com.sap.demo.entity.Person;
import com.sap.demo.repository.PersonRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PersonService {
	
	@Autowired
	private PersonRepository repository;
	

	public Mono<Person> savePerson(Mono<Person> person) {
		
		return repository.saveAll(person).elementAt(0);
	}
	
	public Flux<Person> listPersons() {
		
		return repository.findAll();
	}
	
	public Mono<Person> findPerson(String id) {		
		return repository.findById(id);
	}	
	
	public Mono<Void> deletePerson(String id) {
		return repository.deleteById(id);
	}	
	
	public Flux<Person> search(Person person) {		
		return repository.findAll(Example.of(person));
	}

}
