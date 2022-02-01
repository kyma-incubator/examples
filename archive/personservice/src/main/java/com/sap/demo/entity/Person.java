package com.sap.demo.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Person implements Serializable {
	
	private static final long serialVersionUID = 49994416335608157L;

	@Id
	@ApiModelProperty(name="ID", example="1bf8b88a-7bb8-4b92-90bc-d1fa34a60a57", 
	notes="Unique MongoDB identifier")
	private String id;
	
	@ApiModelProperty(name="First Name", example="John",
	notes="First Name of the Person")
	private String firstName;
	
	@ApiModelProperty(name="Last Name", example="Doe",
			notes="Last Name of the Person")
	private String lastName;
	
	@ApiModelProperty(name="Street Address", example="Nymphenburger Str.",
			notes="Street name w/o house number")
	private String streetAddress;
	
	@ApiModelProperty(name="House Number", example="86",
			notes="House Number")
	private String houseNumber;
	
	@ApiModelProperty(name="ZIP", example="80636",
			notes="House Number")
	private String zip;
	
	@ApiModelProperty(name="City", example="Muenchen",
			notes="City Name")
	private String city;
	
	//extension Fields
	@ApiModelProperty(name="Extension Fields",
			notes="Arbitrary json key value pairs")
	private Map<String, Object> extensionFields = new HashMap<String, Object>();

}
