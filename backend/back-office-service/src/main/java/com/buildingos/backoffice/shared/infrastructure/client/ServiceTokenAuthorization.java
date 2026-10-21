package com.buildingos.backoffice.shared.infrastructure.client;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Sends this service's own token (D-38) instead of the caller's; a 401 drops the cached token. */
public final class ServiceTokenAuthorization implements ClientHttpRequestInterceptor {
    private final ServiceTokenProvider tokens;

    public ServiceTokenAuthorization(ServiceTokenProvider tokens) {
        this.tokens = tokens;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.token());
        ClientHttpResponse response = execution.execute(request, body);
        if (response.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            tokens.invalidate();
        }
        return response;
    }
}
