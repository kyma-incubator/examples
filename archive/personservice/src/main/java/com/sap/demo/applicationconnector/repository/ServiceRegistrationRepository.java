package com.sap.demo.applicationconnector.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.CrudRepository;
import com.sap.demo.applicationconnector.entity.ServiceRegistration;

@Profile("ApplicationConnector")
public interface ServiceRegistrationRepository extends CrudRepository<ServiceRegistration, String>{

}
