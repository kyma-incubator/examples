package com.sap.demo.applicationconnector.rest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.demo.applicationconnector.client.PairingService;
import com.sap.demo.applicationconnector.client.RegistrationService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;

@Profile("ApplicationConnector")
@RestController
public class ApplicationConnectorApi {

	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private PairingService pairingService;

	@PostMapping("/api/v1/applicationconnector/registration/automatic")
	@ApiOperation(value = "Register to Kyma Environment automatically", notes = "This Operation registers "
			+ "the Service to the configured Kyma environment automatically")
	public ResponseEntity<Map<String, String>> connectivityTest(@RequestBody ConnectUrl connectUrl) {

		if (pairingService.executeInitialPairing(connectUrl)) {
			String registrationId = registrationService.registerWithKymaInstance();
			if (StringUtils.isNotBlank(registrationId)) {
				return new ResponseEntity<Map<String, String>>(Collections.singletonMap("id", registrationId), HttpStatus.OK);
			} else {
				return new ResponseEntity<Map<String, String>>(Collections.singletonMap("id", registrationId), HttpStatus.valueOf(500));
			}
		} else {
			return new ResponseEntity<Map<String, String>>(
					Collections.singletonMap("response", "Certificate generation failed. See logs."),
					HttpStatus.valueOf(500));
		}
	}

	@PostMapping("/api/v1/applicationconnector/registration/manual")
	@ApiOperation(value = "Register to Kyma Environment manually", notes = "This Operation registers "
			+ "the Service to the configured Kyma environment using on the JKS that was created manually", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, String>> connectivityTest(@ApiParam(name = "jksFile", value = "Upload the generated JKS file", required = true)
	@RequestPart("jksFile") MultipartFile jksFile) {

		String registrationId = registrationService.registerWithKymaInstance();
		if (StringUtils.isNotBlank(registrationId)) {
			return new ResponseEntity<Map<String, String>>(Collections.singletonMap("id", registrationId), HttpStatus.OK);
		} else {
			return new ResponseEntity<Map<String, String>>(Collections.singletonMap("id", registrationId), HttpStatus.valueOf(500));
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
