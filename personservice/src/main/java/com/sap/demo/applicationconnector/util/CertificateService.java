package com.sap.demo.applicationconnector.util;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.stereotype.Service;

import com.sap.demo.applicationconnector.exception.ApplicationConnectorException;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Utility Service that creates a Certificate Signing Request and returns the
 * DER (w/o base64) encoded CSR in conjunction with the newly generated key
 * pair. The underlying libraries are from @link http://www.bouncycastle.org
 * 
 * @author Andreas Krause
 */
@Service
public class CertificateService {

	private KeyPair generateKeyPair(String algorithm) {

		if (!algorithm.equals("rsa2048")) {
			throw new ApplicationConnectorException(String.format("Key Algorith %s not supported", algorithm));
		}

		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048, new SecureRandom());
			
			return keyGen.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new ApplicationConnectorException(String.format("Error generating Keypair: %s", e.getMessage()), e);
		}

	}

	private byte[] createSigningRequest(String subjectString, KeyPair keypair)
			throws OperatorCreationException, IOException {
		X500Principal subject = new X500Principal(subjectString);

		ContentSigner signGen = new JcaContentSignerBuilder("SHA1withRSA").build(keypair.getPrivate());

		PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject,
				keypair.getPublic());
		PKCS10CertificationRequest csr = builder.build(signGen);

		return csr.getEncoded();
	}

	/**
	 * Creates a certificate signing request and Key pair that can be used for
	 * generating a Kyma / Extension Factory compliant CSR. This will then be signed
	 * by the connector.
	 * 
	 * @param subject   subject string provided by the signingRequest/info endpoint
	 * @param algorithm algorithm string provided by the signingRequest/info
	 *                  endpoint
	 * @return generated public and private key and the resulting csr as specified
	 *         by the input parameters {@link CsrResult}}
	 * @throws ApplicationConnectorException if anything fails
	 */
	public CsrResult createCSR(String subject, String algorithm) {
		KeyPair keypair = generateKeyPair(algorithm);
		try {
			byte[] csr = createSigningRequest(subject, keypair);

			return new CsrResult(keypair, csr);

		} catch (Exception e) {
			throw new ApplicationConnectorException(e.getMessage(), e);
		}
	}

	@Data
	@AllArgsConstructor
	public static class CsrResult {

		private KeyPair keypair;
		private byte[] csr;

	}

}
