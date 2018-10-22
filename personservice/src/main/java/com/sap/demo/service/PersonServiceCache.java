package com.sap.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.sap.demo.entity.Person;
import com.sap.demo.repository.PersonRepository;

import io.micrometer.core.instrument.MeterRegistry;

@Service("PersonService")
@Profile("Cache")
public class PersonServiceCache extends PersonServiceDefault {
	

	
	public PersonServiceCache(MeterRegistry registry, PersonRepository repository) {
		super(registry, repository);
	}


	Logger logger = LoggerFactory.getLogger(this.getClass());
	

	/* (non-Javadoc)
	 * @see com.sap.demo.service.PersonService#savePerson(com.sap.demo.entity.Person)
	 */
	@Override
	@CachePut(key="#result.id", value="persons")
	public Person savePerson(Person person) {		
		Person result = super.savePerson(person);
		
		logger.debug(String.format("Updating Cache for Person ID: %s", 
				 result.getId()));
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.sap.demo.service.PersonService#deltaUpdate(com.sap.demo.entity.Person)
	 */
	@Override
	@CachePut(key="#result.id", value="persons")
	public Person deltaUpdate(Person person) {
		
		Person result = super.deltaUpdate(person);
		
		logger.debug(String.format("Updating Cache for Person ID: %s", 
				result.getId()));
		
		return result;
	}
	
	
	@Override
	@Cacheable(key="#id", value="persons")
	public Person findPerson(String id) {		
		
		logger.debug(String.format("Cache miss for Person ID: %s", 
				id));				
		return super.findPerson(id);
	}	
	

	@Override
	@CacheEvict(key="#id", value="persons")
	public void deletePerson(String id) {
		
		
		super.deletePerson(id);		
		logger.debug(String.format("Cache evict for Person ID: %s", 
				id));
	}		
	
	
	/* (non-Javadoc)
	 * @see com.sap.demo.service.PersonService#deleteAllPersons()
	 */
	@Override
	@CacheEvict(value = "persons", allEntries=true)
	public void deleteAllPersons() {
		super.deleteAllPersons();
		logger.debug("Cache evicted for all persons");
	}

}
