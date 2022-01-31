package com.sap.demo.applicationconnector.exception;


/**
* Baseline Exception for all errors related to creating a RestTemplate based on
* KeyStore and Pass.
*/
public class RestTemplateCustomizerException extends RuntimeException{

	private static final long serialVersionUID = -9204263913670593402L;

	public RestTemplateCustomizerException(String message) {
		super(message);
	}
	
	public RestTemplateCustomizerException(String message, Throwable e) {
		super(message, e);
	}
}
