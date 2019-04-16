package com.sap.demo.applicationconnector.configuration;

import java.security.KeyStore;
import javax.net.ssl.SSLContext;

import com.sap.demo.applicationconnector.RegistrationServiceRestTemplateResponseErrorHandler;
import com.sap.demo.exception.PersonServiceException;
import com.sap.demo.log.NoLogging;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Profile("ApplicationConnector")
@Configuration
public class ApplicationConnectorRestTemplateConfiguration {

	private RestTemplateBuilder restTemplateBuilder;

	@Value("${personservicekubernetes.applicationconnector.keystorepassword}")
	private String keyStorePassword = "set-me";

	@Value("${personservicekubernetes.applicationconnector.baseurl}")
	private String connectorBaseUrl;

	@Value("${personservicekubernetes.applicationconnector.keystorelocation}")
	private String keystoreLocation;

	// private ClientCertificateRepository clientCertificateRepository;

	@Autowired
	public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}

	@NoLogging
	public RestTemplate applicationConnectorRestTemplate() {

		try {
			// return createFactory().createInstance();
			return new RestTemplate();
		} catch (Exception e) {
			e.printStackTrace();
			throw new PersonServiceException(e.getMessage(), e);
		}

		// try {
		// 	KeyStore clientCertificate = KeyStore.getInstance("JKS");

		// 	// get keystore from file
		// 	// clientCertificate.load(new FileInputStream(keystoreLocation), keyStorePassword.toCharArray());

		// 	// get keystore from mongodb
		// 	clientCertificateRepository.findById("123");

		// 	SSLContext sslContext = SSLContextBuilder.create()
		// 			// Trust all strategy to deal with local Kyma certificate.
		// 			// Do not use in production!!!
		// 			.loadTrustMaterial(null, new TrustSelfSignedStrategy())
		// 			.loadKeyMaterial(clientCertificate, keyStorePassword.toCharArray())
		// 			.build();
					
		// 	SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

		// 	HttpClient client = HttpClients.custom().setSSLSocketFactory(socketFactory).build();

		// 	return restTemplateBuilder.rootUri(connectorBaseUrl)
		// 			.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
		// 			.errorHandler(new RegistrationServiceRestTemplateResponseErrorHandler()).build();
		// } catch (Exception e) {
		// 	System.err.println(e.getMessage());
		// 	throw new PersonServiceException(e.getMessage(), e);
		// }
	}

}
