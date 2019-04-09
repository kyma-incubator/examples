package com.sap.demo.applicationconnector.configuration;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;


/**
* Spring Configurations relevant for the "pairing step"
* 
* @author Andreas Krause
* @see RestTemplate
* @see RestTemplateBuilder
* @see HttpClient
*/
@Configuration
public class PairingServiceConfiguration {
	
	private RestTemplateBuilder restTemplateBuilder;
	
	/**
	 * Sets the {@link RestTemplateBuilder} to be used by this object
	 * 
	 * @param restTemplateBuilder {@link RestTemplateBuilder} to be used by this Object 
	 */
	@Autowired
	public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplateBuilder = restTemplateBuilder;
	}
	
	/**
	 * Creates the {@link RestTemplate} Bean with default {@link HttpClient} for consistency
	 * 
	 * @return generic {@link RestTemplate} based on {@link HttpClient}
	 */
	@Bean("PairingTemplate")
	public RestTemplate pairingRestTemplate() {
		
		HttpClient client = HttpClients.createDefault();
		return restTemplateBuilder
				.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
				.build();
		
	}
	
	

}
