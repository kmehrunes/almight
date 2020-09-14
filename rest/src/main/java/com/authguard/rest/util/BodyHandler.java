package com.authguard.rest.util;

import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.validators.Validators;
import com.authguard.api.dto.validation.violations.Violation;
import com.authguard.rest.exceptions.RequestValidationException;
import com.authguard.rest.mappers.RestJsonMapper;
import io.javalin.http.Context;

import java.util.List;

public class BodyHandler<T> {
    private final Class<T> bodyClass;
    private final Validator<T> validator;

    public BodyHandler(final Class<T> bodyClass, final Validator<T> validator) {
        this.bodyClass = bodyClass;
        this.validator = validator;
    }

    public T get(final Context context) {
        return RestJsonMapper.asClass(context.body(), bodyClass);
    }

    public T getValidated(final Context context) {
        final T body = RestJsonMapper.asClass(context.body(), bodyClass);
        final List<Violation> violations = validator.validate(body);

        if (!violations.isEmpty()) {
            throw new RequestValidationException(violations);
        }

        return body;
    }

    public static class Builder<T> {
        private Class<T> bodyClass;

        public Builder(final Class<T> bodyClass) {
            this.bodyClass = bodyClass;
        }

        public Builder<T> bodyClass(final Class<T> bodyClass) {
            this.bodyClass = bodyClass;
            return this;
        }

        public BodyHandler<T> build() {
            final Validator<T> validator = Validators.getForClass(bodyClass);

            if (validator == null) {
                throw new IllegalStateException("No validator was found for class " + bodyClass);
            }

            return new BodyHandler<>(bodyClass, validator);
        }
    }
}
