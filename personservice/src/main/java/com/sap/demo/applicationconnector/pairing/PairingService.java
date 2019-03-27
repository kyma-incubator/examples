package com.sap.demo.applicationconnector.pairing;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.demo.applicationconnector.entity.Connection;
import com.sap.demo.applicationconnector.exception.ApplicationConnectorException;
import com.sap.demo.applicationconnector.exception.RestTemplateCustomizerException;
import com.sap.demo.applicationconnector.rest.ApplicationConnectorApi.ConnectUrl;
import com.sap.demo.applicationconnector.util.CertificateService;
import com.sap.demo.applicationconnector.util.CertificateService.CsrResult;
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
 * @see ConnectionModel
 */
@Service
public class PairingService {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private RestTemplate pairingTemplate;

    private CertificateService certService;

    private RestTemplateBuilder restTemplateBuilder;

    @Value("${personservicekubernetes.applicationconnector.keystorepassword}")
    private char[] keystorePassword;

    @Value("${personservicekubernetes.applicationconnector.keystorelocation}")
    private String keystoreLocation;

    /**
     * Sets the {@link ClientCertRestTemplateBuilder} to be used by this object
     * 
     * @param restTemplateBuilder {@link ClientCertRestTemplateBuilder} to be used
     *                            by this Object
     */
    @Autowired
    public void setClientCertRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
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

    private KeyStore getCertificateInternal(RestTemplate restTemplate, char[] keystorePassword, URI csrUrl, byte[] csr,
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

            Certificate[] certificateChain = new X509Certificate[2];

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            certificateChain[0] = cf.generateCertificate(
                    new ByteArrayInputStream(base64Decoder.decode(response.getBody().getClientCrt())));

            certificateChain[1] = cf
                    .generateCertificate(new ByteArrayInputStream(base64Decoder.decode(response.getBody().getCaCrt())));

            ks.load(null, keystorePassword);

            ks.setKeyEntry("extension-factory-key", keyPair.getPrivate(), keystorePassword, certificateChain);

            // persisting the keystore
            FileOutputStream out = new FileOutputStream(keystoreLocation);
            ks.store(out, keystorePassword);
            out.flush();
            out.close();
            logger.trace("Babe, bin fertig!");
            
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
     * Refreshes the current connection data to Kyma / Extension Factory and returns
     * an object {@link ConnectionModel} with all needed details.
     * 
     * @param currentConnectionModel model containing all details for the current
     *                               connection
     * @return {@link ConnectionModel} that contains updated connection details
     * @throws ApplicationConnectorException   if anything fails
     * @throws RestTemplateCustomizerException if anything fails with acquiring the
     *                                         {@link RestTemplate}
     */
    public Connection getInfo(Connection currentConnectionModel) {
        return getInfo(currentConnectionModel.getInfoUrl(), currentConnectionModel.getKeystorePass(),
                currentConnectionModel.getSslKey(), currentConnectionModel.getCertificateAlgorithm(),
                currentConnectionModel.getCertificateSubject());
    }

    private Connection getInfo(URI infoUrl, char[] keystorePassword, KeyStore keyStore,
            String certificateAlgorithm, String certificateSubject) {

        RestTemplate restTemplate = new RestTemplate();
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
            result.setKeystorePass(keystorePassword);
            result.setSslKey(keyStore);
            result.setCertificateAlgorithm(certificateAlgorithm);
            result.setCertificateSubject(certificateSubject);

            if (response.getBody().getUrls().getEventsUrl() != null)
                result.setEventsURLs(Collections.singletonList(response.getBody().getUrls().getEventsUrl()));

            return result;
        } catch (RestClientException e) {
            throw new ApplicationConnectorException(e.getMessage(), e);
        }
    }

    /**
     * Establishes the initial connection to Kyma / Extension Factory and returns an
     * object {@link ConnectionModel} with all needed details.
     * 
     * @param keystorePassword password to be provided for the keystore
     * @param connectUri       with valid one time token from Connector Services
     * @return {@link ConnectionModel} that contains all info related to the
     *         connection
     * @throws ApplicationConnectorException if anything fails
     */
    public boolean executeInitialPairing(ConnectUrl connectUrl) {
        try {
            ConnectInfo connectInfo = getConnectInfo(new URI(connectUrl.getUrl()));
            
            CsrResult csr = certService.createCSR(connectInfo.getCertificate().getSubject(),
            connectInfo.getCertificate().getKeyAlgorithm());
            
            KeyStore keyStore = getCertificateInternal(pairingTemplate, keystorePassword, connectInfo.getCsrUrl(),
            csr.getCsr(), csr.getKeypair());
            
            if (keyStore != null) {
                return true;
            }
            
        } catch (URISyntaxException e) {
            throw new ApplicationConnectorException(e.getMessage(), e);
        }

        return false;
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

    }
}