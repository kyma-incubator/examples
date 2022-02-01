package com.sap.demo.applicationconnector.util;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

public class RestTemplateUrlTrailingSlashRemover implements ClientHttpRequestInterceptor {

    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
                URI uri = request.getURI();
                if (uri.getPath().endsWith("/")) {
                    uri = URI.create(uri.toString().substring(0, uri.toString().length()-1));
                }
                return execution.execute(new MyHttpRequestWrapper(request, uri), body);
            }

    private class MyHttpRequestWrapper extends HttpRequestWrapper {
        private URI targetUri;
        
        public MyHttpRequestWrapper(HttpRequest request, URI targetUri) {
            super(request);
            this.targetUri = targetUri;
        }

        @Override
        public URI getURI() {
            return targetUri;
        }
    }
}