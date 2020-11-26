package com.authguard.service.impl;

import com.authguard.service.ExchangeService;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.TokenRestrictionsBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExchangeServiceImpl implements ExchangeService {
    private final Map<String, Exchange> exchanges;

    @Inject
    public ExchangeServiceImpl(final List<Exchange> exchanges) {
        this.exchanges = mapExchanges(exchanges);
    }

    @Override
    public TokensBO exchange(final String token, final String fromTokenType, final String toTokenType) {
        return exchange(token, null, fromTokenType, toTokenType);
    }

    @Override
    public TokensBO exchange(final String token, final TokenRestrictionsBO restrictions,
                             final String fromTokenType, final String toTokenType) {
        final String key = exchangeKey(fromTokenType, toTokenType);
        final Exchange exchange = exchanges.get(key);

        if (exchange == null) {
            throw new ServiceException(ErrorCode.UNKNOWN_EXCHANGE, "Unknown token exchange " + fromTokenType + " to " + toTokenType);
        }

        final Either<Exception, TokensBO> generatedTokens = restrictions == null ?
                exchange.exchangeToken(token) :
                exchange.exchangeToken(token, restrictions);

        if (generatedTokens.isRight()) {
            return generatedTokens.get();
        } else {
            final Exception e = generatedTokens.getLeft();

            if (e.getClass().isAssignableFrom(ServiceException.class)) {
                throw (ServiceException) e;
            } else {
                throw new RuntimeException(e); // TODO remove this
            }
        }
    }

    @Override
    public boolean supportsExchange(final String fromTokenType, final String toTokenType) {
        return exchanges.containsKey(exchangeKey(fromTokenType, toTokenType));
    }

    private Map<String, Exchange> mapExchanges(final List<Exchange> exchanges) {
        return exchanges.stream()
                .filter(exchange -> exchange.getClass().getAnnotation(TokenExchange.class) != null)
                .collect(Collectors.toMap(
                        this::tokenExchangeToString,
                        Function.identity()
                ));
    }

    private String tokenExchangeToString(final Exchange exchange) {
        final TokenExchange tokenExchange = exchange.getClass().getAnnotation(TokenExchange.class);
        return exchangeKey(tokenExchange.from(), tokenExchange.to());
    }

    private String exchangeKey(final String fromTokenType, final String toTokenType) {
        return fromTokenType + "-" + toTokenType;
    }
}
