package com.sap.demo.rest;


import java.util.Map.Entry;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@RestController
public class EnvironmentApi {
	
	@GetMapping("/api/v1/environment")
	public Flux<Entry<String,String>> getEnvironment() {
		
		return Flux.fromStream(System.getenv().entrySet().stream());				
	}

}
