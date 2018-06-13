package com.sap.demo.repository;

import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.sap.demo.entity.Person;

public interface PersonRepository extends ReactiveCrudRepository<Person, String>,
											ReactiveQueryByExampleExecutor<Person>{

}
