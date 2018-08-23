package com.sap.demo.applicationconnector.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Document
public class ServiceRegistration {
	
	String serviceId;

}
