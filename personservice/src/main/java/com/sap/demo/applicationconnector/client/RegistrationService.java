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
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.sap.demo.applicationconnector.entity.ServiceRegistration;
import com.sap.demo.applicationconnector.repository.ServiceRegistrationRepository;
import com.sap.demo.exception.PersonServiceException;

import lombok.Data;

@Profile("ApplicationConnector")
@Service
public class RegistrationService {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	private ServiceRegistrationRepository serviceRegistrationRepository;
	private RestTemplate restTemplate;
	
	@Autowired
	private ApplicationConnectorRestTemplateConfiguration configuration;

	@Value("${personservicekubernetes.applicationconnector.registrationfilelocation}")
	private String registrationFileLocation;

	@Autowired
	public void setServiceRegistrationRepository(ServiceRegistrationRepository serviceRegistrationRepository) {
		this.serviceRegistrationRepository = serviceRegistrationRepository;
	}

	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
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

		this.setRestTemplate(configuration.applicationConnectorRestTemplate());

		logger.trace(String.format("Has persisted registration: %b", serviceRegistrations.hasNext()));
		
		if (serviceRegistrations.hasNext()) {
			registrationId = serviceRegistrations.next().getServiceId();

			logger.trace(String.format("Persisted registration: %s", registrationId));

			ResponseEntity<Object> registrationStatus = restTemplate
					.getForEntity("/v1/metadata/services/{registrationId}", Object.class, registrationId);

			logger.trace(String.format("Response Code GET %s registration: %d", registrationId,
					registrationStatus.getStatusCode().value()));

			isRegistered = registrationStatus.getStatusCode() == HttpStatus.OK ? true : false;
		}

		logger.trace(String.format("Is Registered: %b", isRegistered));

		if (isRegistered) {
			logger.trace("PUT Call");
			restTemplate.put("/v1/metadata/services/{serviceId}", getServiceModelForRegistration(), registrationId);
		} else {
			logger.trace("POST Call");
			ResponseEntity<RegistrationResponseModel> response = restTemplate.postForEntity("/v1/metadata/services",
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
		ParameterizedTypeReference<List<RegistrationQueryResponse>> responseType = new ParameterizedTypeReference<List<RegistrationQueryResponse>>() {
		};
		ResponseEntity<List<RegistrationQueryResponse>> kymaRegistrations = restTemplate
				.exchange("/v1/metadata/services", HttpMethod.GET, null, responseType);

		for (RegistrationQueryResponse response : kymaRegistrations.getBody()) {
			try {
				restTemplate.exchange("/v1/metadata/services/{registrationId}", HttpMethod.DELETE, null, String.class,
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

		this.setRestTemplate(configuration.applicationConnectorRestTemplate());

		ResponseEntity<List<RegistrationQueryResponse>> kymaRegistrations = restTemplate
				.exchange("/v1/metadata/services", HttpMethod.GET, null, responseType);
		return kymaRegistrations.getBody();
	}

	public boolean createAndSaveKeyStore(String tokenUrl) {
		RestTemplate rest = new RestTemplate();
		ResponseEntity<ConnectServiceRequest> response = rest.getForEntity(tokenUrl, ConnectServiceRequest.class);
		try {
			String csrUrl = response.getBody().getCsrUrl();
			String[] keyCmd = { "openssl", "genrsa", "-out", "/jks/personservicekubernetes.key", "2048"};
			Process p = Runtime.getRuntime().exec(keyCmd);
			logger.trace("[Generate RSA key] " + (p.waitFor() == 0 ? "Success" : "Failed" ));
			
			String[] csrCmd = { "openssl", "req", "-new", "-sha256", "-out", "/jks/personservicekubernetes.csr", "-key", "/jks/personservicekubernetes.key", "-subj", "/OU=OrgUnit/O=Organization/L=Waldorf/ST=Waldorf/C=DE/CN=personservicekubernetes" };
			Process p2 = Runtime.getRuntime().exec(csrCmd);
			logger.trace("[Create CSR] " + (p2.waitFor() == 0 ? "Success" : "Failed" ));

			byte[] encoded = Files.readAllBytes(Paths.get("/jks/personservicekubernetes.csr"));
			String encodedCsrContent = new String(Base64.getEncoder().encode(encoded), StandardCharsets.UTF_8);

			Map<String, String> csr = new HashMap<String, String>();
			csr.put("csr", encodedCsrContent);
			logger.trace("Calling: " + csrUrl);
			ResponseEntity<CsrResponse> encodedCertificate = rest.postForEntity(csrUrl, csr, CsrResponse.class);

			byte[] decodedCrt = Base64.getDecoder().decode(encodedCertificate.getBody().getCrt().getBytes());

			Files.write(Paths.get("/jks/personservicekubernetes.crt"), decodedCrt);

			String[] pkcs12Cmd = { "openssl", "pkcs12", "-export", "-name", "personservicekubernetes", "-in",
					"/jks/personservicekubernetes.crt", "-inkey", "/jks/personservicekubernetes.key", "-out",
					"/jks/personservicekubernetes.p12", "-password", "pass:kyma-project" };
			Process p3 = Runtime.getRuntime().exec(pkcs12Cmd);
			logger.trace("[Create P12] " + (p3.waitFor() == 0 ? "Success" : "Failed" ));

			String[] jksCmd = { "keytool", "-importkeystore", "-noprompt", "-destkeystore", "/jks/personservicekubernetes.jks",
					"-srckeystore", "/jks/personservicekubernetes.p12", "-srcstoretype", "pkcs12", "-alias",
					"personservicekubernetes", "-srcstorepass", "kyma-project", "-storepass", "kyma-project" };
			Process p4 = Runtime.getRuntime().exec(jksCmd);
			logger.trace("[Create JKS] " + (p4.waitFor() == 0 ? "Success" : "Failed" ));

			// successfully created jks
			return true;
		} catch (Exception e) {
			logger.error("Error " + e.getMessage());
			e.printStackTrace();
		}

		return false;
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

	@Data
	public static class ConnectServiceRequest {
		private String csrUrl;
		private Map<String, Object> api = new HashMap<String, Object>();
		private Map<String, Object> certificate = new HashMap<String, Object>();

		public String getCsrUrl() {
			return this.csrUrl;
		}
	}

	@Data
	public static class CsrResponse {
		private String crt;
		private String clientCrt;
		private String caCrt;
	}
}
