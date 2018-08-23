package com.sap.demo.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import com.sap.demo.entity.Person;

public interface PersonRepository extends CrudRepository<Person, String>,
											QueryByExampleExecutor<Person>{

}
