package com.sap.demo.exception;

public class NotFoundException extends RuntimeException{

	private static final long serialVersionUID = -4471754942923414698L;

	public NotFoundException(String message) {
		super(message);
	}

}
