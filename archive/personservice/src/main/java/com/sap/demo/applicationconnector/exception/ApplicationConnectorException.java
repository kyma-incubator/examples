package com.sap.demo.applicationconnector.exception;



/**
* Baseline Exception for all failures with connecting to the Kyma / 
* Extension Factory Application Connector component.
*/
public class ApplicationConnectorException extends RuntimeException{

	private static final long serialVersionUID = -9204263913670593402L;

	public ApplicationConnectorException(String message) {
		super(message);
	}
	
	public ApplicationConnectorException(String message, Throwable e) {
		super(message, e);
	}
}
