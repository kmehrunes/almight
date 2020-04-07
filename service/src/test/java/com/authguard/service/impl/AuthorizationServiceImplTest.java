package com.authguard.service.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.service.AccountsService;
import com.authguard.service.AuthTokenVerfier;
import com.authguard.service.AuthorizationService;
import com.authguard.service.AuthProvider;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.config.ImmutableStrategyConfig;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationServiceImplTest {
    private final EasyRandom random = new EasyRandom(new EasyRandomParameters()
            .collectionSizeRange(1, 4));

    private AuthorizationService authorizationService;
    private AccountsService accountsService;
    private AuthTokenVerfier authenticationTokenVerifier;
    private AuthProvider accessTokenProvider;
    private AccountTokensRepository accountTokensRepository;

    @BeforeEach
    void setup() {
        authenticationTokenVerifier = Mockito.mock(AuthTokenVerfier.class);
        accountsService = Mockito.mock(AccountsService.class);
        accessTokenProvider = Mockito.mock(AuthProvider.class);
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);

        final ImmutableStrategyConfig accessTokenStrategy = ImmutableStrategyConfig.builder()
                        .refreshTokenLife("5m")
                        .build();

        authorizationService = new AuthorizationServiceImpl(accountsService, authenticationTokenVerifier,
                accessTokenProvider, accountTokensRepository, accessTokenStrategy);
    }

    @Test
    void authorize() {
        // data
        final String accountId = "accountId";
        final String mockIdToken = "mock-jwt";
        final String authorizationHeader = "Bearer " + mockIdToken;

        final AccountBO account = random.nextObject(AccountBO.class);
        final TokensBO tokens = random.nextObject(TokensBO.class);

        // prepare mocks
        Mockito.when(authenticationTokenVerifier.verifyAccountToken(mockIdToken))
                .thenReturn(Optional.of(accountId));
        Mockito.when(accountsService.getById(accountId)).thenReturn(Optional.of(account));
        Mockito.when(accessTokenProvider.generateToken(account)).thenReturn(tokens);

        // call
        final TokensBO generatedTokens = authorizationService.authorize(authorizationHeader);

        // assert
        assertThat(generatedTokens).isEqualTo(tokens);

        final ArgumentCaptor<AccountTokenDO> accountTokenArg = ArgumentCaptor
                .forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenArg.capture());

        final AccountTokenDO accountToken = accountTokenArg.getValue();

        assertThat(accountToken.getToken()).isEqualTo(tokens.getRefreshToken());
        assertThat(accountToken.getAssociatedAccountId()).isEqualTo(accountId);

        // since we can't know the exact expiration date those seem like reasonable limits
        final ZonedDateTime upperLimit = ZonedDateTime.now()
                .plus(Duration.ofMinutes(5));
        final ZonedDateTime lowerLimit = ZonedDateTime.now()
                .minus(Duration.ofSeconds(5))
                .plus(Duration.ofMinutes(5));

        assertThat(accountToken.expiresAt())
                .isBefore(upperLimit)
                .isAfter(lowerLimit);

    }

    @Test
    void refresh() {
        // data
        final String refreshToken = "refresh";
        final AccountTokenDO existingAccountToken = random.nextObject(AccountTokenDO.class)
                .withExpiresAt(ZonedDateTime.now().plus(Duration.ofMinutes(10)));

        final AccountBO account = random.nextObject(AccountBO.class)
                .withId(existingAccountToken.getAssociatedAccountId());
        final TokensBO tokens = random.nextObject(TokensBO.class);

        // prepare mocks
        Mockito.when(accountTokensRepository.getByToken(refreshToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(existingAccountToken)));

        Mockito.when(accountsService.getById(existingAccountToken.getAssociatedAccountId()))
                .thenReturn(Optional.of(account));
        Mockito.when(accessTokenProvider.generateToken(account)).thenReturn(tokens);

        // call
        final TokensBO generatedTokens = authorizationService
                .refresh(refreshToken);

        // assert
        assertThat(generatedTokens).isEqualTo(tokens);

        final ArgumentCaptor<AccountTokenDO> accountTokenArg = ArgumentCaptor
                .forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(accountTokenArg.capture());

        final AccountTokenDO newAccountToken = accountTokenArg.getValue();

        assertThat(newAccountToken.getToken()).isEqualTo(tokens.getRefreshToken());
        assertThat(newAccountToken.getAssociatedAccountId()).isEqualTo(account.getId());

        // since we can't know the exact expiration date those seem like reasonable limits
        final ZonedDateTime upperLimit = ZonedDateTime.now()
                .plus(Duration.ofMinutes(5));
        final ZonedDateTime lowerLimit = ZonedDateTime.now()
                .minus(Duration.ofSeconds(5))
                .plus(Duration.ofMinutes(5));

        assertThat(newAccountToken.expiresAt())
                .isBefore(upperLimit)
                .isAfter(lowerLimit);
    }

    @Test
    void refreshExpired() {
        // data
        final String refreshToken = "refresh";
        final AccountTokenDO existingAccountToken = random.nextObject(AccountTokenDO.class)
                .withExpiresAt(ZonedDateTime.now().minus(Duration.ofMinutes(10)));

        // prepare mocks
        Mockito.when(accountTokensRepository.getByToken(refreshToken))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(existingAccountToken)));

        // call
        assertThatThrownBy(() -> authorizationService.refresh(refreshToken))
                .isInstanceOf(CompletionException.class)
                .extracting("cause")
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}