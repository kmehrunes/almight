package com.authguard.service.auth;

import com.authguard.dal.model.AccountTokenDO;
import io.vavr.control.Either;

public interface AuthTokenVerfier {
    /**
     * Verify a given token.
     * @return The associated account ID or empty if the token was invalid.
     */
    Either<Exception, String> verifyAccountToken(final String token);

    default Either<Exception, AccountTokenDO> verifyAndGetAccountToken(final String token) {
        throw new UnsupportedOperationException();
    }
}
