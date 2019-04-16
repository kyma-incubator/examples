package com.sap.demo.applicationconnector.util;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sap.demo.applicationconnector.exception.RestTemplateCustomizerException;

/**
 * Utility Service that creates a RestTemplate, that uses 2-way-ssl for
 * authentication.
 * 
 * @author Andreas Krause
 * @see RestTemplate
 * @see RestTemplateBuilder
 * @see HttpClient
 */
@Service
public class ClientCertRestTemplateBuilder {

	private static Cache<String, RestTemplate> cache = CacheBuilder.newBuilder().maximumSize(100)
			.expireAfterAccess(10, TimeUnit.MINUTES).build();

	private RestTemplateBuilder restTemplateBuilder;

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
	
	/**
	 * Creates a {@link RestTemplate} for a given {@link KeyStore} and pass (for the
	 * keystore). Rest template will use key and certificate from KeyStore to
	 * establish 2-way-ssl (Client Certificate) connection. Rest Template uses
	 * {@link HttpClient}
	 * 
	 * @param clientCertificate      keystore holding certificate and private key
	 * @param password               to access the keystore
	 * @return {@link RestTemplate} that is 2-way-ssl enabled
	 * @throws RestTemplateCustomizerException if anything fails
	 */
	public RestTemplate applicationConnectorRestTemplate(KeyStore clientCertificate, char[] keystorePassword) {
		
		String certificateFingerprint = getCertificateFingerprint(clientCertificate);
		
		RestTemplate result = cache.getIfPresent(certificateFingerprint);
		
		if (result == null) {

			try {
				SSLContext sslContext = SSLContextBuilder.create().loadKeyMaterial(clientCertificate, keystorePassword)
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
