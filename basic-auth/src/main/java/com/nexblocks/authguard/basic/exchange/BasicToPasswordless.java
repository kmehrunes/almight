package com.nexblocks.authguard.basic.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.basic.passwordless.PasswordlessProvider;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.TokenOptionsMapper;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "basic", to = "passwordless")
public class BasicToPasswordless implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final PasswordlessProvider passwordlessProvider;

    @Inject
    public BasicToPasswordless(final BasicAuthProvider basicAuth, final PasswordlessProvider passwordlessProvider) {
        this.basicAuth = basicAuth;
        this.passwordlessProvider = passwordlessProvider;
    }

    @Override
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        return basicAuth.getAccountSessionAsync(request)
                .thenCompose(accountSession -> {
                    TokenOptionsBO tokenOptions = TokenOptionsMapper.fromAuthRequest(request)
                            .source("basic")
                            .trackingSession(accountSession.getSession().getSessionToken())
                            .build();

                    return passwordlessProvider.generateToken(accountSession.getAccount(), tokenOptions);
                });
    }
}
