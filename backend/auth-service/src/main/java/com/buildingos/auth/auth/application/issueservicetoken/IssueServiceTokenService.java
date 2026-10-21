package com.buildingos.auth.auth.application.issueservicetoken;

import com.buildingos.auth.auth.application.port.out.ServiceClientRegistry;
import com.buildingos.auth.auth.application.port.out.ServiceTokenIssuer;
import java.time.Duration;
import java.util.Objects;

/** Client-credentials exchange for service-to-service calls (BOS-010 D-38). */
public final class IssueServiceTokenService implements IssueServiceTokenUseCase {
    private final ServiceClientRegistry clients;
    private final ServiceTokenIssuer tokens;
    private final Duration ttl;

    public IssueServiceTokenService(ServiceClientRegistry clients, ServiceTokenIssuer tokens, Duration ttl) {
        this.clients = clients;
        this.tokens = tokens;
        this.ttl = Objects.requireNonNull(ttl, "ttl");
    }

    @Override
    public IssuedServiceToken execute(IssueServiceTokenCommand command) {
        var scopes = clients.authenticate(command.clientId(), command.clientSecret())
                .orElseThrow(ServiceClientRejectedException::new);
        var token = tokens.issueService(command.clientId(), scopes, ttl);
        return new IssuedServiceToken(token.accessToken(), token.expiresInSeconds());
    }
}
