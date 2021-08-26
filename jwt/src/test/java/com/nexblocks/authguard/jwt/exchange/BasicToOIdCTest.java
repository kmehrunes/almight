package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.jwt.IdTokenProvider;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
import com.nexblocks.authguard.service.model.*;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class BasicToOIdCTest {
    private BasicAuthProvider basicAuth;
    private AccessTokenProvider accessTokenProvider;
    private IdTokenProvider idTokenProvider;
    private OpenIdConnectTokenProvider openIdConnectTokenProvider;

    private BasicToOIdC basicToOIdC;

    @BeforeEach
    void setup() {
        basicAuth = Mockito.mock(BasicAuthProvider.class);
        accessTokenProvider = Mockito.mock(AccessTokenProvider.class);
        idTokenProvider = Mockito.mock(IdTokenProvider.class);

        openIdConnectTokenProvider = new OpenIdConnectTokenProvider(accessTokenProvider, idTokenProvider);

        basicToOIdC = new BasicToOIdC(basicAuth, openIdConnectTokenProvider);
    }

    @Test
    void exchange() {
        final AccountBO account = AccountBO.builder().id("account").build();

        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .identifier("username")
                .password("password")
                .build();

        final AuthResponseBO accessTokenResponse = AuthResponseBO.builder()
                .token("access token")
                .refreshToken("refresh token")
                .build();

        final AuthResponseBO idTokenResponse = AuthResponseBO.builder()
                .token("id token")
                .build();

        final TokenOptionsBO options = TokenOptionsBO.builder()
                .source("basic")
                .build();

        Mockito.when(basicAuth.authenticateAndGetAccount(authRequest))
                .thenReturn(Either.right(account));

        Mockito.when(accessTokenProvider.generateToken(account, authRequest.getRestrictions(), options))
                .thenReturn(accessTokenResponse);

        Mockito.when(idTokenProvider.generateToken(account))
                .thenReturn(idTokenResponse);

        final Either<Exception, AuthResponseBO> actual = basicToOIdC.exchange(authRequest);

        final AuthResponseBO expected = AuthResponseBO.builder()
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .type("oidc")
                .token(OAuthResponseBO.builder()
                        .accessToken((String) accessTokenResponse.getToken())
                        .idToken((String) idTokenResponse.getToken())
                        .refreshToken((String) accessTokenResponse.getRefreshToken())
                        .build())
                .build();

        assertThat(actual.isRight()).isTrue();
        assertThat(actual.get()).isEqualTo(expected);
    }
}