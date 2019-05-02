package com.sap.demo.applicationconnector.util;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sap.demo.applicationconnector.entity.Connection;
import com.sap.demo.applicationconnector.exception.RestTemplateCustomizerException;
import com.sap.demo.applicationconnector.repository.ConnectionRepository;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Profile("ApplicationConnector")
@Service
public class ApplicationConnectorRestTemplateBuilder {
    private static Cache<String, RestTemplate> cache = CacheBuilder.newBuilder().maximumSize(100)
			.expireAfterAccess(10, TimeUnit.MINUTES).build();

	private ConnectionRepository connectionRepository;
	private RestTemplateBuilder restTemplateBuilder;

	@Autowired
	public void setConnectionRepository(ConnectionRepository connectionRepository) {
		this.connectionRepository = connectionRepository;
	}

	/**
	 * Sets the {@link RestTemplateBuilder} to be used by this object
	 * 
	 * @param restTemplateBuilder {@link RestTemplateBuilder} to be used by this
	 *                            Object
	 */
	@Autowired
	public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}

	private String getCertificateFingerprint(KeyStore keyStore) {
		try {
			Enumeration<String> aliases = keyStore.aliases();
			
			if (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
				
				return DigestUtils.sha1Hex(cert.getEncoded());
			} else {
				throw new RestTemplateCustomizerException("Key Store "
						+ "invalid, could not determine certificate fingerprint");
			}
		} catch (KeyStoreException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		} catch (CertificateEncodingException e) {
			throw new RestTemplateCustomizerException(e.getMessage(), e);
		}
	}

	public RestTemplate getEventEndpointRestTemplate() {
		Iterator<Connection> connectionRegistrations = connectionRepository.findAll().iterator();
		
		if (!connectionRegistrations.hasNext()) {
			throw new RestTemplateCustomizerException("No connection registered.");
		}

		Connection connection = connectionRegistrations.next();
		String baseUri = connection.getEventsUrl().toString();

		return getRestTemplate(connection, baseUri);
	}

	// Returns a RestTemplate where the baseUrl is set to the metadata URL of the connection (/v1/metadata/services)
	public RestTemplate getMetadataEndpointRestTemplate() {
		Iterator<Connection> connectionRegistrations = connectionRepository.findAll().iterator();
		
		if (!connectionRegistrations.hasNext()) {
			throw new RestTemplateCustomizerException("No connection registered.");
		}

		Connection connection = connectionRegistrations.next();
		String baseUri = connection.getMetadataUrl().toString();

		return getRestTemplate(connection, baseUri);
	}

	public RestTemplate getRestTemplate(Connection connection, String baseUri) {
		
		char[] keyStorePassword = connection.getKeyStorePassword();

		KeyStore clientCertificate = connection.getSslKey();

		String certificateFingerprint = getCertificateFingerprint(clientCertificate);
		
		RestTemplate result = cache.getIfPresent(certificateFingerprint);
		
		if (result == null) {
			try {
				SSLContext sslContext = SSLContextBuilder.create().loadKeyMaterial(clientCertificate, keyStorePassword)
						.build();

				SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

				HttpClient client = HttpClients.custom().setSSLSocketFactory(socketFactory).build();

				result = restTemplateBuilder.rootUri(baseUri)
						.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client)).build();
				
				cache.put(certificateFingerprint, result);
				
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
		} else {
			return result;
		}
	}

	// Returns a RestTemplate from the Connection object saved in the database (or from the cache)
	public RestTemplate applicationConnectorRestTemplate() {
		Iterator<Connection> connectionRegistrations = connectionRepository.findAll().iterator();
		
		if (!connectionRegistrations.hasNext()) {
			throw new RestTemplateCustomizerException("No connection registered.");
		}

		Connection connection = connectionRegistrations.next();
		char[] keyStorePassword = connection.getKeyStorePassword();

		KeyStore clientCertificate = connection.getSslKey();

		String certificateFingerprint = getCertificateFingerprint(clientCertificate);
		
		RestTemplate result = cache.getIfPresent(certificateFingerprint);
		
		if (result == null) {
			try {
				SSLContext sslContext = SSLContextBuilder.create().loadKeyMaterial(clientCertificate, keyStorePassword)
						.build();

				SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

				HttpClient client = HttpClients.custom().setSSLSocketFactory(socketFactory).build();

				result = restTemplateBuilder
						.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client)).build();
				
				cache.put(certificateFingerprint, result);
				
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
		} else {
			return result;
		}
	}

	// Returns a RestTemplate from the KeyStore object (or from cache)
	public RestTemplate applicationConnectorRestTemplate(KeyStore clientCertificate, char[] keyStorePassword) {
		String certificateFingerprint = getCertificateFingerprint(clientCertificate);
		
		RestTemplate result = cache.getIfPresent(certificateFingerprint);
		
		if (result == null) {

			try {
				SSLContext sslContext = SSLContextBuilder.create().loadKeyMaterial(clientCertificate, keyStorePassword)
						.build();

				SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

				HttpClient client = HttpClients.custom().setSSLSocketFactory(socketFactory).build();

				result = restTemplateBuilder
						.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client)).build();
				
				cache.put(certificateFingerprint, result);
				
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
		} else {
			return result;
		}
	}

}