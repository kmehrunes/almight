package com.nexblocks.authguard.jwt.oauth.service;

import com.google.inject.Inject;
import com.nexblocks.authguard.jwt.exchange.PkceParameters;
import com.nexblocks.authguard.jwt.oauth.route.OpenIdConnectRequest;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.vavr.control.Try;
import okhttp3.HttpUrl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class OpenIdConnectService {
    private static final String BASIC_TOKEN_TYPE = "basic";
    private static final String AUTH_CODE_TOKEN_TYPE = "authorizationCode";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String OIDC_TOKEN_TYPE = "oidc";
    private static final String ACCESS_TOKEN_TYPE = "accessToken";

    private final ClientsService clientsService;
    private final ExchangeService exchangeService;

    @Inject
    public OpenIdConnectService(ClientsService clientsService, ExchangeService exchangeService) {
        this.clientsService = clientsService;
        this.exchangeService = exchangeService;
    }

    public CompletableFuture<AuthResponseBO> processAuth(OpenIdConnectRequest request, RequestContextBO requestContext) {
        if (!Objects.equals(request.getResponseType(), "code")) {
            return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Invalid response type"));
        }

        long parsedId;

        try {
            parsedId = Long.parseLong(request.getClientId());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.APP_DOES_NOT_EXIST,
                    "Invalid client ID"));
        }

        return clientsService.getById(parsedId)
                .thenCompose(AsyncUtils::fromClientOptional)
                .thenCompose(client -> {
                    if (client.getClientType() != Client.ClientType.SSO) {
                        return CompletableFuture.failedFuture(new ServiceException(ErrorCode.CLIENT_NOT_PERMITTED,
                                "Client isn't permitted to perform OIDC requests"));
                    }

                    Try<AuthRequestBO> validatedRequest = verifyClientRequest(client, request.getRedirectUri())
                            .flatMap(ignored -> createRequest(request, client));

                    if (validatedRequest.isFailure()) {
                        return CompletableFuture.failedFuture(validatedRequest.getCause());
                    }

                    return exchangeService.exchange(validatedRequest.get(), BASIC_TOKEN_TYPE, AUTH_CODE_TOKEN_TYPE,
                            requestContext.withClientId(String.valueOf(request.getClientId())));
                });
    }

    public CompletableFuture<AuthResponseBO> processAuthCodeToken(AuthRequestBO request, RequestContextBO requestContext) {
        return exchangeService.exchange(request, AUTH_CODE_TOKEN_TYPE, OIDC_TOKEN_TYPE, requestContext);
    }

    public CompletableFuture<AuthResponseBO> processRefreshToken(AuthRequestBO request, RequestContextBO requestContext) {
        return exchangeService.exchange(request, REFRESH_TOKEN_TYPE, ACCESS_TOKEN_TYPE, requestContext);
    }

    private Try<AuthRequestBO> createRequest(OpenIdConnectRequest request, ClientBO client) {
        boolean isPkce = request.getCodeChallenge() != null
                || request.getCodeChallengeMethod() != null;

        if (isPkce) {
            return verifyPkceRequest(request)
                    .map(ignored -> AuthRequestBO.builder()
                            .domain(client.getDomain())
                            .identifier(request.getIdentifier())
                            .password(request.getPassword())
                            .externalSessionId(request.getExternalSessionId())
                            .extraParameters(PkceParameters.forAuthCode(request.getCodeChallenge(), request.getCodeChallengeMethod()))
                            .build());
        }

        return Try.success(AuthRequestBO.builder()
                .domain(client.getDomain())
                .identifier(request.getIdentifier())
                .password(request.getPassword())
                .externalSessionId(request.getExternalSessionId())
                .build());
    }

    private Try<Boolean> verifyClientRequest(Client client, String redirectUri) {
        if (client.getClientType() != Client.ClientType.SSO) {
            return Try.failure(new ServiceException(ErrorCode.CLIENT_NOT_PERMITTED,
                    "Client isn't permitted to perform OIDC requests"));
        }

        final HttpUrl parsedUrl = HttpUrl.parse(redirectUri);

        if (parsedUrl == null) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Invalid redirect URL"));
        }

        if (!parsedUrl.host().equalsIgnoreCase(client.getBaseUrl())) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Redirect URL doesn't match the client base URL"));
        }

        return Try.success(true);
    }

    private Try<Boolean> verifyPkceRequest(OpenIdConnectRequest request) {
        if (!Objects.equals(request.getCodeChallengeMethod(), "S256")) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Code challenge method must be S256 (SHA-256)"));
        }

        if (request.getCodeChallenge() == null) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Code challenge missing"));
        }

        return Try.success(true);
    }
}
