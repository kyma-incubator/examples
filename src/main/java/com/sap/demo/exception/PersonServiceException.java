package com.sap.demo.exception;

public class PersonServiceException extends RuntimeException{

	private static final long serialVersionUID = -9204263913670593402L;

	public PersonServiceException(String message) {
		super(message);
	}
	
	public PersonServiceException(String message, Throwable e) {
		super(message, e);
	}
}
