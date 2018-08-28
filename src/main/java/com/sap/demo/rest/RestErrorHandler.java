package com.sap.demo.rest;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.sap.demo.exception.NotFoundException;
import com.sap.demo.exception.PersonServiceException;

import lombok.AllArgsConstructor;
import lombok.Data;



@ControllerAdvice
public class RestErrorHandler extends ResponseEntityExceptionHandler {
	
	@ExceptionHandler(value=NotFoundException.class)
	public ResponseEntity<RestError> handleResourceNotAvailable(HttpServletRequest request, RuntimeException e) {
		RestError restError = new RestError(e.getMessage());
		return new ResponseEntity<RestError>(restError, HttpStatus.NOT_FOUND);
	}
	
	@ExceptionHandler(value=PersonServiceException.class)
	public ResponseEntity<RestError> handlePersonServiceException(HttpServletRequest request, RuntimeException e) {
		RestError restError = new RestError(e.getMessage());
		return new ResponseEntity<RestError>(restError, HttpStatus.NOT_FOUND);
	}
	
	@Data
	@AllArgsConstructor
	public static class RestError {
		private String message;
	}
}
