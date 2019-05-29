package com.sap.demo.applicationconnector.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.demo.applicationconnector.client.CertificateService.CsrResult;
import com.sap.demo.applicationconnector.entity.Connection;
import com.sap.demo.applicationconnector.exception.ApplicationConnectorException;
import com.sap.demo.applicationconnector.exception.RestTemplateCustomizerException;
import com.sap.demo.applicationconnector.repository.ConnectionRepository;
import com.sap.demo.applicationconnector.util.ApplicationConnectorRestTemplateBuilder;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Service that "pairs" the client with Kyma / Extension Factory. It supports
 * the following steps: * Initial Connect * Certificate Renewal * Get Info
 * (Needs to be periodically invoked)
 * 
 * All methods return a fresh Connection Model.
 * 
 * @author Andreas Krause
 * @see Connection
 */
@Profile("ApplicationConnector")
@Service
public class PairingService {

	private final Pattern CERT_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                    "([a-z0-9+/=\\r\\n]+)" +                    // Base64 text
                    "-+END\\s+.*CERTIFICATE[^-]*-+",            // Footer
			Pattern.CASE_INSENSITIVE);
			
	private RestTemplate pairingTemplate;
	private CertificateService certService;
	private ApplicationConnectorRestTemplateBuilder restTemplateBuilder;
	private ConnectionRepository connectionRepository;

	// Just for educational purposes. Don't do this in production environments!
	private String keyStorePassword = "kyma-project";

	@Autowired
	public void setConnectionRepository(ConnectionRepository connectionRepository) {
		this.connectionRepository = connectionRepository;
	}

	@Autowired
	public void setApplicationConnectorRestTemplateBuilder(
			ApplicationConnectorRestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}

	/**
	 * Sets the {@link CertificateService} to be used by this object
	 * 
	 * @param certService {@link CertificateService} to be used by this Object
	 */
	@Autowired
	public void setCertService(CertificateService certService) {
		this.certService = certService;
	}

	/**
	 * Sets the {@link RestTemplate} to be used by this object for the initila
	 * pairing step (no 2-way-ssl)
	 * 
	 * @param pairingTemplate {@link RestTemplate} to be used by this Object
	 */
	@Autowired
	@Qualifier("PairingTemplate")
	public void setPairingTemplate(RestTemplate pairingTemplate) {
		this.pairingTemplate = pairingTemplate;
	}

	private Encoder base64Encoder = Base64.getEncoder();
	private Decoder base64Decoder = Base64.getDecoder();

	private ConnectInfo getConnectInfo(URI connectUri) {
		try {
			ResponseEntity<ConnectInfo> response = pairingTemplate.getForEntity(connectUri, ConnectInfo.class);

			if (response.getStatusCode() != HttpStatus.OK) {
				throw new ApplicationConnectorException(String.format("Error Response Received, code: %d (%s)",
						response.getStatusCode().value(), response.getStatusCode().getReasonPhrase()));
			}

			return response.getBody();
		} catch (RestClientException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}

	private List<String> matchCertificates(String certsString) {
		
		Matcher m = CERT_PATTERN.matcher(certsString);
		
		List<String> result = new ArrayList<String>();
		
		while (m.find()) {
			result.add(m.group());
		}
		
		if (result.size() > 0) {
			return result;
		} else {
			throw new ApplicationConnectorException("No certificates contained in parsed string:\n " + certsString);
		}
   }

	private KeyStore getCertificateInternal(RestTemplate restTemplate, char[] keyStorePassword, URI csrUrl, byte[] csr,
			KeyPair keyPair) {
		String encodedCsr = String.format(
				"-----BEGIN CERTIFICATE REQUEST-----\n%s" + "\n-----END CERTIFICATE REQUEST-----",
				base64Encoder.encodeToString(csr));

		String doubleEncodedCsr = base64Encoder.encodeToString(encodedCsr.getBytes());

		CsrRequest request = new CsrRequest(doubleEncodedCsr);
		try {
			ResponseEntity<CsrResponse> response = restTemplate.postForEntity(csrUrl, request, CsrResponse.class);

			if (response.getStatusCode() != HttpStatus.CREATED) {
				throw new ApplicationConnectorException(String.format("Error Response Received, code: %d (%s)",
						response.getStatusCode().value(), response.getStatusCode().getReasonPhrase()));
			}

			KeyStore ks = KeyStore.getInstance("JKS");

			List<String> certs = matchCertificates(new String(
					 		base64Decoder.decode(response.getBody().getCrt())));
			
			Certificate[] certificateChain = new X509Certificate[certs.size()];
			
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
				
			for (int counter = 0; counter < certs.size(); counter++) {
				certificateChain[counter] = cf.generateCertificate(
												new ByteArrayInputStream(certs.get(counter).getBytes()));
			}
			
			ks.load(null, keyStorePassword);
			
			ks.setKeyEntry("extension-factory-key", 
					keyPair.getPrivate(), 
					keyStorePassword, 
					certificateChain);
			return ks;

		} catch (RestClientException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		} catch (KeyStoreException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		} catch (CertificateException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		} catch (IOException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}

	/**
	 * Refreshes the current certificate and private key used to communicate with
	 * Kyma / Extension Factory and returns a new object {@link Connection} with
	 * refreshed key store and password.
	 * 
	 * @param newKeyStorePassword password to be used for the refreshed keystore
	 * @return {@link Connection} that contains updated connection details with
	 *         refreshed keystore
	 * @throws ApplicationConnectorException   if anything fails
	 * @throws RestTemplateCustomizerException if anything fails with acquiring the
	 *                                         {@link RestTemplate}
	 */
	public void renewCertificate(char[] newKeyStorePassword) {

		Iterator<Connection> connectionRegistrations = connectionRepository.findAll().iterator();
		
		if (!connectionRegistrations.hasNext()) {
			throw new RestTemplateCustomizerException("No connection found");
		}

		Connection connection = connectionRegistrations.next();
		
		Connection result = getInfo(connection);

		CsrResult csr = certService.createCSR(connection.getCertificateSubject(),
		connection.getCertificateAlgorithm());

		RestTemplate restTemplate = restTemplateBuilder.applicationConnectorRestTemplate();

		KeyStore newKey = getCertificateInternal(restTemplate, newKeyStorePassword, connection.getRenewCertUrl(),
				csr.getCsr(), csr.getKeypair());

		result.setKeyStorePassword(newKeyStorePassword);
		result.setSslKey(newKey);

		connectionRepository.save(result);
	}

	/**
	 * Refreshes the current connection data to Kyma / Extension Factory and returns
	 * an object {@link Connection} with all needed details.
	 * 
	 * @param currentConnection model containing all details for the current
	 *                          connection
	 * @return {@link Connection} that contains updated connection details
	 * @throws ApplicationConnectorException   if anything fails
	 * @throws RestTemplateCustomizerException if anything fails with acquiring the
	 *                                         {@link RestTemplate}
	 */
	public Connection getInfo(Connection currentConnection) {
		return getInfo(currentConnection.getInfoUrl(), currentConnection.getKeyStorePassword(),
				currentConnection.getSslKey(), currentConnection.getCertificateAlgorithm(),
				currentConnection.getCertificateSubject());
	}

	private Connection getInfo(URI infoUrl, char[] keyStorePassword, KeyStore keyStore, String certificateAlgorithm,
			String certificateSubject) {

		RestTemplate restTemplate = restTemplateBuilder.applicationConnectorRestTemplate(keyStore, keyStorePassword);
		try {
			ResponseEntity<InfoResponse> response = restTemplate.getForEntity(infoUrl, InfoResponse.class);

			if (response.getStatusCode() != HttpStatus.OK) {
				throw new ApplicationConnectorException(String.format("Error Response Received, code: %d (%s)",
						response.getStatusCode().value(), response.getStatusCode().getReasonPhrase()));
			}

			Connection result = new Connection();

			result.setApplicationName(response.getBody().getClientIdentity().getApplication());
			result.setInfoUrl(infoUrl);
			result.setMetadataUrl(response.getBody().getUrls().getMetadataUrl());
			result.setRenewCertUrl(response.getBody().getUrls().getRenewCertUrl());
			result.setRevocationCertUrl(response.getBody().getUrls().getRevocationCertUrl());
			result.setKeyStorePassword(keyStorePassword);
			result.setSslKey(keyStore);
			result.setCertificateAlgorithm(certificateAlgorithm);
			result.setCertificateSubject(certificateSubject);
			result.setEventsInfoUrl(response.getBody().getUrls().getEventsInfoUrl());
			result.setEventsUrl(response.getBody().getUrls().getEventsUrl());

			return result;
		} catch (RestClientException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}

	/**
	 * Establishes the initial connection to Kyma / Extension Factory and returns an
	 * object {@link Connection} with all needed details.
	 * 
	 * @param connectUri with valid one time token from Connector Services
	 * @return {@link Connection} that contains all info related to the connection
	 * @throws ApplicationConnectorException if anything fails
	 */
	public void executeInitialPairing(URI connectUri) {

		ConnectInfo connectInfo = getConnectInfo(connectUri);

		CsrResult csr = certService.createCSR(connectInfo.getCertificate().getSubject(),
				connectInfo.getCertificate().getKeyAlgorithm());

		KeyStore keyStore = getCertificateInternal(pairingTemplate, this.keyStorePassword.toCharArray(),
				connectInfo.getCsrUrl(), csr.getCsr(), csr.getKeypair());

		Connection connection = getInfo(connectInfo.getApi().getInfoUrl(), this.keyStorePassword.toCharArray(),
				keyStore, connectInfo.getCertificate().getKeyAlgorithm(), connectInfo.getCertificate().getSubject());

		connectionRepository.save(connection);
	}

	public void executeManualPairing(URI infoUrl, KeyStore keyStore, String keyStorePassword) {
		try {
			Enumeration<String> aliases = keyStore.aliases();
			if (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

				String certificateSubject = cert.getSubjectX500Principal().getName();
				String certificateAlgorithm = cert.getPublicKey().getAlgorithm();

				Connection connection = getInfo(infoUrl, keyStorePassword.toCharArray(), keyStore, certificateAlgorithm,
						certificateSubject);

				connectionRepository.save(connection);
			} else {
				throw new ApplicationConnectorException("KeyStore has no aliases");
			}
		} catch (KeyStoreException e) {
			throw new ApplicationConnectorException("KeyStore is defect");
		}
	}

	public void deleteConnections() {
		connectionRepository.deleteAll();
	}

	@Data
	private static class ConnectInfo {
		private URI csrUrl;
		private Api api;
		private CertificateSpecification certificate;

	}

	@Data
	private static class Api {
		private URI metadataUrl;
		private URI certificatesUrl;
		private URI infoUrl;

		private CertificateSpecification certificate;

	}

	@Data
	private static class CertificateSpecification {
		private String subject;
		private String extensions;

		@JsonProperty("key-algorithm")
		private String keyAlgorithm;

	}

	@Data
	@AllArgsConstructor
	private static class CsrRequest {
		private String csr;
	}

	@Data
	@AllArgsConstructor
	private static class CsrResponse {
		private String crt;
		private String clientCrt;
		private String caCrt;
	}

	@Data
	private static class InfoResponse {
		private ClientIdentity clientIdentity;
		private Urls urls;
	}

	@Data
	private static class ClientIdentity {
		private String application;
	}

	@Data
	private static class Urls {
		private URI eventsUrl;
		private URI metadataUrl;
		private URI renewCertUrl;
		private URI revocationCertUrl;
		private URI eventsInfoUrl;
	}

}
