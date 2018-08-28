package com.sap.demo.applicationconnector.client;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.sap.demo.exception.PersonServiceException;
import com.sap.demo.log.NoLogging;

@Profile("ApplicationConnector")
@Configuration
public class ApplicationConnectorRestTemplateConfiguration {
	
	private RestTemplateBuilder restTemplateBuilder;
	
	private HttpRequestInterceptor istioTraceInterceptor;
	
	@Value("${personservicekubernetes.applicationconnetor.keytorepassword}")
	private String keyStorePassword = "set-me";
	
	@Value("${personservicekubernetes.applicationconnetor.baseurl}")
	private String connectorBaseUrl;

	@Value("${personservicekubernetes.applicationconnetor.keystorelocation}")
	private String keystoreLocation;
	
	@Autowired
	public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}
	
	@Autowired
	@Qualifier("IstioTraceInterceptor")
	public void istioTraceInterceptor(HttpRequestInterceptor istioTraceInterceptor) {
		this.istioTraceInterceptor = istioTraceInterceptor;
	}
	
	
	
	@Bean("ApplicationConnectorRestTemplate")
	@NoLogging
	public RestTemplate applicationConnectorRestTemplate() {

		try {
			KeyStore clientCertificate = KeyStore.getInstance("JKS");

			clientCertificate.load(new FileInputStream(keystoreLocation), keyStorePassword.toCharArray());


			SSLContext sslContext = SSLContextBuilder
					.create()
					//Trust all strategy to deal with local Kyma certificate. 
					// Do not use in production!!!
					.loadTrustMaterial(null, new TrustSelfSignedStrategy())
					.loadKeyMaterial(clientCertificate, keyStorePassword.toCharArray())//new char[0])
					.build();
			SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

			HttpClient client = HttpClients.custom()
					.setSSLSocketFactory(socketFactory)
					.addInterceptorLast(istioTraceInterceptor)
					.build();	

			return restTemplateBuilder
							.rootUri(connectorBaseUrl)
							.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
							.errorHandler(new RegistrationServiceRestTemplateResponseErrorHandler())
							.build();
		} catch (Exception e) {
			throw new PersonServiceException(e.getMessage(), e);
		} 
	}

}
