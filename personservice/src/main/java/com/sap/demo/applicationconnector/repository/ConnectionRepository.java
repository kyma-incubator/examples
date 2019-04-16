package com.sap.demo.applicationconnector.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.CrudRepository;
import com.sap.demo.applicationconnector.entity.Connection;

@Profile("ApplicationConnector")
public interface ConnectionRepository extends CrudRepository<Connection, String>{

}
