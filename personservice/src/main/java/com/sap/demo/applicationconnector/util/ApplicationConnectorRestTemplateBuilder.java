package com.sap.demo.applicationconnector.util;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Iterator;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sap.demo.applicationconnector.entity.Connection;
import com.sap.demo.applicationconnector.exception.RestTemplateCustomizerException;
import com.sap.demo.applicationconnector.repository.ConnectionRepository;

@Profile("ApplicationConnector")
@Service
public class ApplicationConnectorRestTemplateBuilder {

	private ConnectionRepository connectionRepository;
	private RestTemplateBuilder restTemplateBuilder;

	@Autowired
	public void setConnectionRepository(ConnectionRepository connectionRepository) {
		this.connectionRepository = connectionRepository;
	}

	@Autowired
	public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}

	public RestTemplate getEventEndpointRestTemplate() {
		Iterator<Connection> connectionRegistrations = connectionRepository.findAll().iterator();

		if (!connectionRegistrations.hasNext()) {
			throw new RestTemplateCustomizerException("No connection registered.");
		}

		Connection connection = connectionRegistrations.next();
		String baseUri = connection.getEventsUrl().toString();

		RestTemplateBuilder builder = getRestTemplateBuilder(connection);
		return builder.rootUri(baseUri).build();
	}

	// Returns a RestTemplate where the baseUrl is set to the metadata URL of the
	// connection (/v1/metadata/services)
	public RestTemplate getMetadataEndpointRestTemplate() {
		Iterator<Connection> connectionRegistrations = connectionRepository.findAll().iterator();

		if (!connectionRegistrations.hasNext()) {
			throw new RestTemplateCustomizerException("No connection registered.");
		}

		Connection connection = connectionRegistrations.next();
		String baseUri = connection.getMetadataUrl().toString();

		RestTemplateBuilder builder = getRestTemplateBuilder(connection);
		return builder.rootUri(baseUri).additionalInterceptors(new RestTemplateUrlTrailingSlashRemover()).build();
	}

	private RestTemplateBuilder getRestTemplateBuilder(Connection connection) {

		char[] keyStorePassword = connection.getKeyStorePassword();

		KeyStore clientCertificate = connection.getSslKey();

		try {
			SSLContext sslContext = SSLContextBuilder.create().loadKeyMaterial(clientCertificate, keyStorePassword)
					.build();

			SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

			HttpClient client = HttpClients.custom().setSSLSocketFactory(socketFactory).build();

			return restTemplateBuilder.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client));

		} catch (KeyManagementException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (UnrecoverableKeyException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (KeyStoreException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		}
	}

	// Returns a RestTemplate from the Connection object saved in the database (or
	// from the cache)
	public RestTemplate applicationConnectorRestTemplate() {
		Iterator<Connection> connectionRegistrations = connectionRepository.findAll().iterator();

		if (!connectionRegistrations.hasNext()) {
			throw new RestTemplateCustomizerException("No connection registered.");
		}

		Connection connection = connectionRegistrations.next();
		char[] keyStorePassword = connection.getKeyStorePassword();

		KeyStore clientCertificate = connection.getSslKey();

		RestTemplate result;

		try {
			SSLContext sslContext = SSLContextBuilder.create().loadKeyMaterial(clientCertificate, keyStorePassword)
					.build();

			SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

			HttpClient client = HttpClients.custom().setSSLSocketFactory(socketFactory).build();

			result = restTemplateBuilder.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
					.build();

			return result;

		} catch (KeyManagementException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (UnrecoverableKeyException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (KeyStoreException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		}

	}

	// Returns a RestTemplate from the KeyStore object (or from cache)
	public RestTemplate applicationConnectorRestTemplate(KeyStore clientCertificate, char[] keyStorePassword) {
		RestTemplate result;

		try {
			SSLContext sslContext = SSLContextBuilder.create().loadKeyMaterial(clientCertificate, keyStorePassword)
					.build();

			SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

			HttpClient client = HttpClients.custom().setSSLSocketFactory(socketFactory).build();

			result = restTemplateBuilder.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
					.build();

			return result;

		} catch (KeyManagementException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (UnrecoverableKeyException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (KeyStoreException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		}
	}

}