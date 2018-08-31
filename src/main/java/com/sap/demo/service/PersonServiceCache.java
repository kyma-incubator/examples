package com.sap.demo.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import com.sap.demo.entity.Person;
import com.sap.demo.exception.NotFoundException;
import com.sap.demo.exception.PersonServiceException;
import com.sap.demo.log.LoggingThreadContext;
import com.sap.demo.repository.PersonRepository;

@Service("PersonService")
@Profile("Cache")
public class PersonServiceCache implements PersonService {
	
	@Autowired
	private PersonRepository repository;
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	

	/* (non-Javadoc)
	 * @see com.sap.demo.service.PersonService#savePerson(com.sap.demo.entity.Person)
	 */
	@Override
	@CachePut(key="#result.id", value="persons")
	public Person savePerson(Person person) {
		
		Person result = person;
		
		//prevent no change updates
		if(person.getId() != null) {
			Optional<Person> dbResult = repository.findById(person.getId()); 
			
			if (dbResult.isPresent()) {
				
				if (dbResult.get().equals(person)) {
					result = dbResult.get();
				} else {
					result = repository.save(person);
				}
				
			} else {
				result = repository.save(person);
			}
		} else {		
			result = repository.save(person);
		}
		
		logger.debug(String.format("%s: Updating Cache for Person ID: %s", 
				LoggingThreadContext.getLoggingKey(), result.getId()));
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.sap.demo.service.PersonService#deltaUpdate(com.sap.demo.entity.Person)
	 */
	@Override
	@CachePut(key="#result.id", value="persons")
	public Person deltaUpdate(Person person) {
		
		
		if(person.getId() == null) {
			throw new PersonServiceException("ID of Person must not be null");
		}
		
		logger.debug(String.format("%s: Updating Cache for Person ID: %s", 
				LoggingThreadContext.getLoggingKey(), person.getId()));
			
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
	
	/* (non-Javadoc)
	 * @see com.sap.demo.service.PersonService#listPersons()
	 */
	@Override
	public Iterable<Person> listPersons() {
		
		return repository.findAll();
	}
	
	
	@Cacheable(key="#id", value="persons")
	public Person findPerson(String id) {	
		Optional<Person> result = repository.findById(id);
		
		if(!result.isPresent()) {
			throw new NotFoundException(String.format(
					"Person with id '%s' not found", id));
		}
		
		logger.debug(String.format("%s: Cache miss for Person ID: %s", 
				LoggingThreadContext.getLoggingKey(), id));
				
		return result.isPresent() ? result.get() : null;
	}	
	

	@Override
	@CacheEvict(key="#id", value="persons")
	public void deletePerson(String id) {
		
		if(id == null) {
			throw new PersonServiceException("ID of Person must not be null");
		}
		
		repository.deleteById(id);
		
		logger.debug(String.format("%s: Cache evict for Person ID: %s", 
				LoggingThreadContext.getLoggingKey(), id));
	}	
	
	/* (non-Javadoc)
	 * @see com.sap.demo.service.PersonService#search(com.sap.demo.entity.Person)
	 */
	@Override
	public Iterable<Person> search(Person person) {		
		return repository.findAll(Example.of(person));
	}
	
	/* (non-Javadoc)
	 * @see com.sap.demo.service.PersonService#deleteAllPersons()
	 */
	@Override
	@CacheEvict(value = "persons", allEntries=true)
	public void deleteAllPersons() {
		repository.deleteAll();
		logger.debug(String.format("%s: Cache evicted for all persons", 
				LoggingThreadContext.getLoggingKey()));
	}

}
