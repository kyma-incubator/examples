package com.sap.demo.applicationconnector.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import com.sap.demo.applicationconnector.exception.ApplicationConnectorException;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;


/**
 * Model Object that holds all information needed to establish a connection
 * to Kyma/Extension Factory.
 * 
 * 
 * @author Andreas Krause
 */
@Data
@Document
public class Connection {
	
	private String applicationName;
	private URI metadataUrl;
	private URI renewCertUrl;
	private URI revocationCertUrl;
	private URI infoUrl;
	
	private String certificateSubject;
	private String certificateAlgorithm;

	// Instead of persisting the KeyStore object, the byte[] of it is stored
	private byte[] sslKeyStream;
	@Transient
	private KeyStore sslKey;
	private char[] keystorePass;
	private List<URI> eventsURLs = new ArrayList<URI>();
	
	/**
	 * Returns the expiry date of the application certificate contained in the keystore 
	 * 
	 * @return {@link Date} NotAfter date of the certificate or null if no alias is found
	 * @throws ApplicationConnectorException if anything fails 
	 */
	public Date getCertificateExpirationDate() {
		try {
			Enumeration<String> aliases = sslKey.aliases();
			
			if (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate cert = (X509Certificate) sslKey.getCertificate(alias);
				
				return cert.getNotAfter();
			} else {
				return null;
			}
			
		} catch (KeyStoreException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}
	
	/**
	 * Returns the fingerprint date of the application certificate contained in the keystore 
	 * 
	 * @return fingerprint of the certificate
	 * @throws ApplicationConnectorException if anything fails 
	 */
	public String getCertificateFingerprint() {
		try {
			Enumeration<String> aliases = sslKey.aliases();
			
			if (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate cert = (X509Certificate) sslKey.getCertificate(alias);
				
				return DigestUtils.sha1Hex(cert.getEncoded());
			} else {
				return null;
			}
			
		} catch (KeyStoreException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		} catch (CertificateEncodingException e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}

	// This getter returns a KeyStore object which is loaded from the sslKeyStream byte[]
	// Only the byte[] is persisted to we need to create the KeyStore object ad hoc
	public KeyStore getSslKey(){
		try {
			this.sslKey = KeyStore.getInstance("JKS");
			this.sslKey.load(new ByteArrayInputStream(this.sslKeyStream), this.keystorePass);
			
			return sslKey;
		} catch (Exception e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}

	// This setter saves the OutputStream of the KeyStore object into the byte[] to persist it in that way
	public void setSslKey(KeyStore keystore){
		try {
			this.sslKey = keystore;

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			keystore.store(output, this.keystorePass);

			this.sslKeyStream = output.toByteArray();
		} catch (Exception e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}
	
}
