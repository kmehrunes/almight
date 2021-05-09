package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.jwt.oauth.AuthorizationCodeVerifier;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.*;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TokenExchange(from = "authorizationCode", to = "accessToken")
public class AuthorizationCodeToAccessToken implements Exchange {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationCodeToAccessToken.class);

    private final AccountsService accountsService;
    private final AuthorizationCodeVerifier authorizationCodeVerifier;
    private final AccessTokenProvider accessTokenProvider;
    private final ServiceMapper serviceMapper;

    @Inject
    public AuthorizationCodeToAccessToken(final AccountsService accountsService,
                                          final AuthorizationCodeVerifier authorizationCodeVerifier,
                                          final AccessTokenProvider accessTokenProvider,
                                          final ServiceMapper serviceMapper) {
        this.accountsService = accountsService;
        this.authorizationCodeVerifier = authorizationCodeVerifier;
        this.accessTokenProvider = accessTokenProvider;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Either<Exception, AuthResponseBO> exchange(final AuthRequestBO request) {
        return authorizationCodeVerifier.verifyAndGetAccountToken(request.getToken())
                .flatMap(this::generateToken);
    }

    private Either<Exception, AuthResponseBO> generateToken(final AccountTokenDO accountToken) {
        if (accountToken.getAdditionalInformation() == null) {
            return generateWithNoRestrictions(accountToken);
        } else {
            if (TokenRestrictionsBO.class.isAssignableFrom(accountToken.getAdditionalInformation().getClass())) {
                return getAccount(accountToken.getAssociatedAccountId())
                        .map(account -> accessTokenProvider
                                .generateToken(account, serviceMapper.toBO(accountToken.getTokenRestrictions())));
            } else {
                throw new ServiceAuthorizationException(ErrorCode.INVALID_ADDITIONAL_INFORMATION_TYPE,
                        "Found additional information of wrong type " + accountToken.getAdditionalInformation().getClass(),
                        EntityType.ACCOUNT, accountToken.getAssociatedAccountId());
            }
        }
    }

    private Either<Exception, AuthResponseBO> generateWithNoRestrictions(final AccountTokenDO accountToken) {
        return getAccount(accountToken.getAssociatedAccountId())
                .map(accessTokenProvider::generateToken);
    }

    private Either<Exception, AccountBO> getAccount(final String accountId) {
        return accountsService.getById(accountId)
                .<Either<Exception, AccountBO>>map(Either::right)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "Account " + accountId + " does not exist")));
    }
}
