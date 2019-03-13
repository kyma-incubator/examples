package com.sap.demo.applicationconnector.rest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.demo.applicationconnector.client.RegistrationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import lombok.Data;

@Profile("ApplicationConnector")
@RestController
public class ApplicationConnectorApi {

	@Autowired
	private RegistrationService registrationService;

	@PostMapping("/api/v1/applicationconnector/registration")
	@ApiOperation(value = "Register to Kyma Environment", notes = "This Operation registers "
			+ "the Service to the configured Kyma environment")
	public ResponseEntity<Map<String, String>> serviceRegistration() {

		return new ResponseEntity<Map<String, String>>(
				Collections.singletonMap("id", registrationService.registerWithKymaInstance()), HttpStatus.OK);
	}

	@PostMapping("/api/v1/applicationconnector/registration/autoregister")
	@ApiOperation(value = "Register to Kyma Environment", notes = "This Operation registers "
			+ "the Service to the configured Kyma environment")
	public ResponseEntity<Map<String, String>> connectivityTest(@RequestBody ConnectUrl connectUrl) {
		if (registrationService.createAndSaveKeyStore(connectUrl.getUrl())) {
			// return new ResponseEntity<String>(HttpStatus.OK);
			return new ResponseEntity<Map<String, String>>(
					Collections.singletonMap("id", registrationService.registerWithKymaInstance()), HttpStatus.OK);
		} else {
			return new ResponseEntity<Map<String, String>>(
					Collections.singletonMap("response", "Certificate generation failed. See logs."),
					HttpStatus.valueOf(500));
			// return new ResponseEntity<String>(HttpStatus.REQUEST_TIMEOUT);
		}
	}

	@DeleteMapping("/api/v1/applicationconnector/registration")
	@ApiOperation(value = "Delete Kyma Environment registrations", notes = "This Operation deletes all Environment registration information.")
	public ResponseEntity<String> deleteServiceRegistration() {

		registrationService.deleteRegistrations();
		return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
	}

	@GetMapping("/api/v1/applicationconnector/registration")
	@ApiOperation(value = "Read Kyma Environment registrations", notes = "This Operation reads all Environment registration information.")
	public ResponseEntity<List<RegistrationService.RegistrationQueryResponse>> readServiceRegistrations() {

		return new ResponseEntity<List<RegistrationService.RegistrationQueryResponse>>(
				registrationService.getExistingRegistrations(), HttpStatus.OK);
	}

	// Model for Get Response
	@Data
	public static class ConnectUrl {
		private String url;
	}
}
