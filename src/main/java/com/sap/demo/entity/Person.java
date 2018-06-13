package com.sap.demo.entity;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Document
@Data
@AllArgsConstructor
public class Person {
	
	@Id
	private String id;
	
	private String firstName;
	private String lastName;
	private String streetAddress;
	private String houseNumber;
	private String zip;
	private String city;
	
	//extension Fields
	private Map<String, Object> extensionFields;

}
