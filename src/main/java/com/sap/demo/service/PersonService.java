package com.sap.demo.service;

import com.sap.demo.entity.Person;

public interface PersonService {

	Person savePerson(Person person);

	Person deltaUpdate(Person person);

	Iterable<Person> listPersons();

	Person findPerson(String id);

	void deletePerson(String id);

	Iterable<Person> search(Person person);

	void deleteAllPersons();

}