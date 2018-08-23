package com.sap.demo.rest;


import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;

@RestController
public class EnvironmentApi {
	
	@GetMapping("/api/v1/environment")
	@ApiOperation(value="List Environment Variables", notes="This Operation retrieves a list of "
			+ "all environment variables available in the underlying "
			+ "environment (e.g. container).")
	public ResponseEntity<Map<String,String>> getEnvironment() {
		
		return new ResponseEntity<Map<String,String>>(System.getenv(), HttpStatus.OK);
	}

}
