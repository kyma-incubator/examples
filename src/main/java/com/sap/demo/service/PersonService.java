package com.sap.demo.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import com.sap.demo.entity.Person;
import com.sap.demo.exception.NotFoundException;
import com.sap.demo.exception.PersonServiceException;
import com.sap.demo.repository.PersonRepository;

@Service
public class PersonService {
	
	@Autowired
	private PersonRepository repository;
	

	public Person savePerson(Person person) {
		
		//prevent no change updates
		if(person.getId() != null) {
			Optional<Person> dbResult = repository.findById(person.getId()); 
			
			if (dbResult.isPresent()) {
				
				if (dbResult.get().equals(person)) {
					return dbResult.get();
				} else {
					return repository.save(person);
				}
				
			} else {
				return repository.save(person);
			}
		} else {		
			return repository.save(person);
		}
	}
	
	public Person deltaUpdate(Person person) {
		
		if(person.getId() == null) {
			throw new PersonServiceException("ID of Person must not be null");
		}
			
		Optional<Person> currentPersonOptional = repository.findById(person.getId());
			
		if(currentPersonOptional.isPresent()) {	
			
			
			Person mergedPerson = currentPersonOptional.get();
			
			mergedPerson.setFirstName(
					person.getFirstName() == null ? mergedPerson.getFirstName() : person.getFirstName()
					);
			
			mergedPerson.setLastName(
					person.getLastName() == null ? mergedPerson.getLastName() : person.getLastName()
					);
			
			mergedPerson.setCity(
					person.getCity() == null ? mergedPerson.getCity() : person.getCity()
					);
			
			mergedPerson.setZip(
					person.getZip() == null ? mergedPerson.getZip() : person.getZip()
					);
			
			mergedPerson.setHouseNumber(
					person.getHouseNumber() == null ? mergedPerson.getHouseNumber() : person.getHouseNumber()
					);
			
			mergedPerson.setStreetAddress(
					person.getStreetAddress() == null ? mergedPerson.getStreetAddress() : person.getStreetAddress()
					);
			
			//not deep but good enough ;-)
			person.getExtensionFields()
				.forEach((key, value) -> mergedPerson.getExtensionFields().put(key, value));
			
			//check if there was a change, not very elegant but works
			currentPersonOptional = repository.findById(person.getId());
			if(currentPersonOptional.get().equals(mergedPerson)) {	
				
				return currentPersonOptional.get();
			} else  {
				return repository.save(mergedPerson);
			}
			
		} else {
			return repository.save(person);			
		}		
	}
	
	public Iterable<Person> listPersons() {
		
		return repository.findAll();
	}
	
	public Person findPerson(String id) {	
		Optional<Person> result = repository.findById(id);
		
		if(!result.isPresent()) {
			throw new NotFoundException(String.format(
					"Person with id '%s' not found", id));
		}
				
		return result.isPresent() ? result.get() : null;
	}	
	
	public void deletePerson(String id) {
		
		if(id == null) {
			throw new PersonServiceException("ID of Person must not be null");
		}
		
		repository.deleteById(id);
	}	
	
	public Iterable<Person> search(Person person) {		
		return repository.findAll(Example.of(person));
	}
	
	public void deleteAllPersons() {
		repository.deleteAll();
	}

}
