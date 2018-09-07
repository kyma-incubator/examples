package com.sap.demo.security;

import java.util.Map;

import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;

public class CustomCompletenessVerifier implements JwtClaimsSetVerifier {
    @Override
    public void verify(Map<String, Object> claims) throws InvalidTokenException {
        if (!claims.containsKey("sub") || !claims.containsKey("scope")) {
        	throw new InvalidTokenException("sub and scopes must not be empty");
        }
    }
}