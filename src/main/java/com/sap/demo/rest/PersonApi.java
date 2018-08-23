package com.sap.demo.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/v1/person")
public class PersonApi {
	
	@Autowired
	private PersonService personService;	
	
	@PostMapping
	@ApiOperation(value="Create Person", notes="This function creates a new person entry on the DB."
			+ "Externally supplied ids are ignored.")
	public ResponseEntity<Person> create(@RequestBody Person person) {			
		person.setId(null);
		return new ResponseEntity<Person>(personService.savePerson(person), HttpStatus.CREATED);		
	}
	
	@GetMapping
	@ApiOperation(value="Get Person List", notes="This function retrieves all persons from the Database.")
	public ResponseEntity<Iterable<Person>> findAll() {
		return new ResponseEntity<Iterable<Person>>(personService.listPersons(), HttpStatus.OK);
	}
	
	
	//would ideally do a field level mapping, but not needed for demo app
	@PatchMapping(path="/{id}")
	@ApiOperation(value="Update Person", notes="This function updates a person from on the database. "
			+ "This does not support delta updates.")
	public ResponseEntity<Person> upsert(@PathVariable("id") String id, 
			@RequestBody Person person) {
		
		return new ResponseEntity<Person>(personService.savePerson(person), HttpStatus.OK); 
	}
	
	@GetMapping(path="/{id}")
	@ApiOperation(value="Read Person by ID", notes="This function reads a single person from the database.")
	public ResponseEntity<Person> read(@PathVariable("id") String id) {
		
		return new ResponseEntity<Person>(personService.findPerson(id), HttpStatus.OK); 
	}
	
	@DeleteMapping(path="/{id}")
	@ApiOperation(value="Delete Person by ID", notes="This function deletes a person from the database.")
	public ResponseEntity<Person> delete(@PathVariable("id") String id) {
		personService.deletePerson(id);
		return new ResponseEntity<Person>(HttpStatus.NO_CONTENT);
		
	}
	
	@DeleteMapping
	@ApiOperation(value="Delete all Persons in DB", notes="This function deletes all persons from the database.")
	public ResponseEntity<Person> deleteAll() {
		personService.deleteAllPersons();
		return new ResponseEntity<Person>(HttpStatus.NO_CONTENT);
		
	}
	
	@PostMapping(path="/search")
	@ApiOperation(value="Search for People", notes="This function performs a people search in the database, and returns the persons that meet the criteria " + 
			"specified in the request.")
	public ResponseEntity<Iterable<Person>> search(@RequestBody Person person) {
		return new ResponseEntity<Iterable<Person>>(personService.search(person), HttpStatus.OK);
	}

}
