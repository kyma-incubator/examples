package com.sap.demo.applicationconnector.client;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.sap.demo.applicationconnector.entity.Connection;
import com.sap.demo.applicationconnector.entity.ServiceRegistration;
import com.sap.demo.applicationconnector.exception.RestTemplateCustomizerException;
import com.sap.demo.applicationconnector.repository.ServiceRegistrationRepository;
import com.sap.demo.applicationconnector.util.ApplicationConnectorRestTemplateBuilder;
import com.sap.demo.exception.PersonServiceException;

import lombok.Data;

@Profile("ApplicationConnector")
@Service
public class RegistrationService {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	private ServiceRegistrationRepository serviceRegistrationRepository;
	private ApplicationConnectorRestTemplateBuilder restTemplateBuilder;

	@Value("${personservicekubernetes.applicationconnector.registrationfilelocation}")
	private String registrationFileLocation;

	@Autowired
	public void setServiceRegistrationRepository(ServiceRegistrationRepository serviceRegistrationRepository) {
		this.serviceRegistrationRepository = serviceRegistrationRepository;
	}

	@Autowired
	public void setRestTemplateBuilder(ApplicationConnectorRestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}

	private String getServiceModelForRegistration() {
		try {
			return FileUtils.readFileToString(new File(registrationFileLocation), "UTF-8");
		} catch (IOException e) {
			System.out.println(String.format("Error: %s", e.getMessage()));
			throw new PersonServiceException(e.getMessage(), e);
		}
	}

	public String registerWithKymaInstance() {

		String registrationId = "";
		Boolean isRegistered = false;

		Iterator<ServiceRegistration> serviceRegistrations = serviceRegistrationRepository.findAll().iterator();

		RestTemplate restTemplate = restTemplateBuilder.getMetadataEndpointRestTemplate();

		logger.trace(String.format("Has persisted registration: %b", serviceRegistrations.hasNext()));

		if (serviceRegistrations.hasNext()) {
			registrationId = serviceRegistrations.next().getServiceId();

			logger.trace(String.format("Persisted registration: %s", registrationId));

			ResponseEntity<Object> registrationStatus = restTemplate
					.getForEntity("/{registrationId}", Object.class, registrationId);

			logger.trace(String.format("Response Code GET %s registration: %d", registrationId,
					registrationStatus.getStatusCode().value()));

			isRegistered = registrationStatus.getStatusCode() == HttpStatus.OK ? true : false;
		}

		logger.trace(String.format("Is Registered: %b", isRegistered));

		if (isRegistered) {
			logger.trace("PUT Call");
			restTemplate.put("/{serviceId}", getServiceModelForRegistration(), registrationId);
		} else {
			logger.trace("POST Call");
			ResponseEntity<RegistrationResponseModel> response = restTemplate.postForEntity("/",
					getServiceModelForRegistration(), RegistrationResponseModel.class);

			logger.trace(String.format("Response Code POST: %s", response.getBody().toString()));
			registrationId = response.getBody().getId();
			ServiceRegistration registration = new ServiceRegistration(registrationId);

			serviceRegistrationRepository.save(registration);
		}

		return registrationId;
	}

	public void deleteRegistrations() {
		serviceRegistrationRepository.deleteAll();

		RestTemplate restTemplate = restTemplateBuilder.getMetadataEndpointRestTemplate();
		ParameterizedTypeReference<List<RegistrationQueryResponse>> responseType = new ParameterizedTypeReference<List<RegistrationQueryResponse>>() {
		};
		ResponseEntity<List<RegistrationQueryResponse>> kymaRegistrations = restTemplate
				.exchange("/v1/metadata/services", HttpMethod.GET, null, responseType);

		for (RegistrationQueryResponse response : kymaRegistrations.getBody()) {
			try {
				restTemplate.exchange("/{registrationId}", HttpMethod.DELETE, null, String.class,
						response.getId());
				logger.trace(String.format("Deleted %s from Kyma", response.getId()));
				// Do nothing, delete all you can
			} catch (Exception e) {
			}
		}

	}

	public List<RegistrationQueryResponse> getExistingRegistrations() {
		ParameterizedTypeReference<List<RegistrationQueryResponse>> responseType = new ParameterizedTypeReference<List<RegistrationQueryResponse>>() {
		};
		try {
			RestTemplate restTemplate = restTemplateBuilder.getMetadataEndpointRestTemplate();
			
			ResponseEntity<List<RegistrationQueryResponse>> kymaRegistrations = restTemplate
			.exchange("/", HttpMethod.GET, null, responseType);
			return kymaRegistrations.getBody();
		} catch (RestTemplateCustomizerException e) {
			// No registrations available
			System.out.println(String.format("Error: %s", e.getMessage()));
			return null;
		}
	}

	// Model for Registration Response
	@Data
	public static class RegistrationResponseModel {
		private String id;
	}

	// Model for Get Response
	@Data
	public static class RegistrationQueryResponse {
		private String id;
		private String provider;
		private String name;
		private String description;
	}

}
