package com.authguard.basic;

import com.authguard.basic.passwords.SecurePassword;
import com.authguard.service.AccountsService;
import com.authguard.service.CredentialsService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.CredentialsBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

import java.util.Base64;
import java.util.Optional;

public class BasicAuthProvider {
    private final CredentialsService credentialsService;
    private final AccountsService accountsService;
    private final SecurePassword securePassword;

    @Inject
    public BasicAuthProvider(final CredentialsService credentialsService, final AccountsService accountsService,
                             final SecurePassword securePassword) {
        this.credentialsService = credentialsService;
        this.securePassword = securePassword;
        this.accountsService = accountsService;
    }

    public Either<Exception, AccountBO> authenticateAndGetAccount(final String basicToken) {
        final String[] parts = TokensUtils.parseAuthorization(basicToken);

        if (parts[0].equals("Basic")) {
            return handleBasicAuthentication(parts[1]);
        } else {
            return Either.left(new ServiceException(ErrorCode.UNSUPPORTED_SCHEME, "Unsupported authorization scheme"));
        }
    }

    public Either<Exception, AccountBO> getAccount(final String basicToken) {
        final String[] parts = TokensUtils.parseAuthorization(basicToken);

        if (parts[0].equals("Basic")) {
            return handleBasicAuthenticationNoPassword(parts[1]);
        } else {
            throw new ServiceException(ErrorCode.UNSUPPORTED_SCHEME, "Unsupported authorization scheme");
        }
    }

    public Either<Exception, AccountBO> getAccountEither(final String basicToken) {
        final String[] parts = TokensUtils.parseAuthorization(basicToken);

        if (parts[0].equals("Basic")) {
            return handleBasicAuthenticationNoPassword(parts[1]);
        } else {
            throw new ServiceException(ErrorCode.UNSUPPORTED_SCHEME, "Unsupported authorization scheme");
        }
    }

    private Either<Exception, AccountBO> handleBasicAuthentication(final String base64Credentials) {
        final String[] decoded = new String(Base64.getDecoder().decode(base64Credentials)).split(":");

        if (decoded.length != 2) {
            throw new ServiceException(ErrorCode.INVALID_AUTHORIZATION_FORMAT, "Invalid format for basic authentication");
        }

        final String username =  decoded[0];
        final String password = decoded[1];

        return verifyCredentialsAndGetAccount(username, password);
    }

    private Either<Exception, AccountBO> handleBasicAuthenticationNoPassword(final String base64Credentials) {
        final String[] decoded = new String(Base64.getDecoder().decode(base64Credentials)).split(":");

        if (decoded.length != 1) {
            return Either.left(new ServiceException(ErrorCode.INVALID_AUTHORIZATION_FORMAT, "Invalid format for basic authentication"));
        }

        final String username =  decoded[0];

        return verifyCredentialsAndGetAccount(username);
    }

    private Either<Exception, AccountBO> verifyCredentialsAndGetAccount(final String username, final String password) {
        final Optional<CredentialsBO> credentials = credentialsService.getByUsernameUnsafe(username);

        if (credentials.isPresent()) {
            if (securePassword.verify(password, credentials.get().getHashedPassword())) {
                return getAccountById(credentials.get().getAccountId());
            } else {
                return Either.left(new ServiceAuthorizationException(ErrorCode.PASSWORDS_DO_NOT_MATCH, "Passwords do not match"));
            }
        } else {
            return Either.left(new ServiceAuthorizationException(ErrorCode.CREDENTIALS_DOES_NOT_EXIST,
                    "Identifier " + username + " does not exist"));
        }
    }

    private Either<Exception, AccountBO> verifyCredentialsAndGetAccount(final String username) {
        final Optional<CredentialsBO> credentials = credentialsService.getByUsernameUnsafe(username);

        if (credentials.isPresent()) {
            return getAccountById(credentials.get().getAccountId());
        } else {
            return Either.left(new ServiceAuthorizationException(ErrorCode.CREDENTIALS_DOES_NOT_EXIST,
                    "Identifier " + username + " does not exist"));
        }
    }

    private Either<Exception, AccountBO> getAccountById(final String accountId) {
        final Optional<AccountBO> account = accountsService.getById(accountId);

        return account
                .<Either<Exception, AccountBO>>map(Either::right)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                "Account " + accountId + " does not exist")));
    }
}
