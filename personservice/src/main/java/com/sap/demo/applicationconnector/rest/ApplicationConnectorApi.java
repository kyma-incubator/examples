package com.sap.demo.applicationconnector.rest;


import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sap.demo.applicationconnector.client.RegistrationService;

import io.swagger.annotations.ApiOperation;

@Profile("ApplicationConnector")
@RestController
public class ApplicationConnectorApi {
	
	@Autowired
	private RegistrationService registrationService;
	
	@PostMapping("/api/v1/applicationconnector/registration")
	@ApiOperation(value="Register to Kyma Environment", notes="This Operation registers "
			+ "the Service to the configured Kyma environment")
	public ResponseEntity<Map<String,String>> serviceRegistration() {
		
		return new ResponseEntity<Map<String, String>>(
				Collections.singletonMap("id",registrationService.registerWithKymaInstance()), 
				HttpStatus.OK);
	}
	
	
	@DeleteMapping("/api/v1/applicationconnector/registration")
	@ApiOperation(value="Delete Kyma Environment registrations", 
	notes="This Operation deletes all Environment registration information.")
	public ResponseEntity<String> deleteServiceRegistration() {
		
		registrationService.deleteRegistrations();
		return new ResponseEntity<String>(				
				HttpStatus.NO_CONTENT);
	}
	
	@GetMapping("/api/v1/applicationconnector/registration")
	@ApiOperation(value="Read Kyma Environment registrations", 
	notes="This Operation reads all Environment registration information.")
	public ResponseEntity<List<RegistrationService.RegistrationQueryResponse>> readServiceRegistrations() {
		
		return new ResponseEntity<List<RegistrationService.RegistrationQueryResponse>>(				
				 registrationService.getExistingRegistrations(), HttpStatus.OK);
	}
}
