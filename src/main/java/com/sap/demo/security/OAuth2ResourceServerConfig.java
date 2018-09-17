package com.sap.demo.security;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.DelegatingJwtClaimsSetVerifier;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Configuration
@EnableResourceServer
@Profile("Security")
@EnableAutoConfiguration()
public class OAuth2ResourceServerConfig extends ResourceServerConfigurerAdapter {
	
	
	// Set parts where Authenticated/Authorized access is mandatory
    @Override
    public void configure(final HttpSecurity http) throws Exception {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).and()
        	.authorizeRequests().antMatchers("/api/**").authenticated()
        	.and().csrf().disable();
    }

    // JWT token store

    @Override
    public void configure(final ResourceServerSecurityConfigurer config) {
        config.tokenServices(tokenServices());
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
      
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        final JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
       
        // Signature is checked in Kyma API / Istio, hence we do not need to bother
        // retrieving the key, decoding it and putting it in here
        converter.setVerifier(new SignatureVerifier() {			
			@Override
			public String algorithm() {
				return "RS256";			
				// If your token uses a different Algorithm, depict it here
			}
			
			@Override
			public void verify(byte[] content, byte[] signature) {
				// Do nothing, token is already verified by istio	
			}
		});
        
  
        
        converter.setJwtClaimsSetVerifier(jwtClaimsSetVerifier());

        return converter;
    }

    @Bean
    public JwtClaimsSetVerifier jwtClaimsSetVerifier() {
       // You can add multiple here
    	return new DelegatingJwtClaimsSetVerifier(Arrays.asList(customCompletenessVerifier()));
    }

    
    @Bean
    public JwtClaimsSetVerifier customCompletenessVerifier() {
        return new CustomCompletenessVerifier();
    }

    @Bean
    @Primary
    public DefaultTokenServices tokenServices() {
        final DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore());
        return defaultTokenServices;
    }
}