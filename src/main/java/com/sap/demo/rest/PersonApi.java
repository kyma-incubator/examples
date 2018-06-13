package com.sap.demo.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sap.demo.entity.Person;
import com.sap.demo.service.PersonService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/person")
public class PersonApi {
	
	@Autowired
	private PersonService personService;	
	
	@PostMapping
	public Mono<Person> create(@RequestBody Mono<Person> person) {	
		
		return personService.savePerson(
			person.map(personData -> {
				personData.setId(null);
				return personData;
			})
		);		
	}
	
	@GetMapping
	public Flux<Person> findAll() {
		return personService.listPersons();
	}
	
	@PatchMapping(path="/{id}")
	public Mono<Person> upsert(@PathVariable("id") String id, 
			@RequestBody Mono<Person> person) {
		
		return personService.savePerson(
				person.map(personData -> {
					personData.setId(id);
					return personData;
				})
			);	
	}
	
	@GetMapping(path="/{id}")
	public Mono<Person> read(@PathVariable("id") String id) {
		
		return personService.findPerson(id);
	}
	
	@DeleteMapping(path="/{id}")
	public Mono<Void> delete(@PathVariable("id") String id) {
		return personService.deletePerson(id);
		
	}
	
	@PostMapping(path="/search")
	public Flux<Person> search(@RequestBody Person person) {
		return personService.search(person);
	}

}
