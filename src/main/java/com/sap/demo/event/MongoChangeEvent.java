package com.sap.demo.event;

import org.bson.Document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MongoChangeEvent {
	
	public static final char DELETE = 'D';
	public static final char SAVE = 'S';
	
	private String collectionName;
	private char type;
	private Document eventData;
}
