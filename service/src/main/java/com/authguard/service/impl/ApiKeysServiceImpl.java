package com.authguard.service.impl;

import com.authguard.dal.model.ApiKeyDO;
import com.authguard.dal.persistence.ApiKeysRepository;
import com.authguard.service.ApiKeysService;
import com.authguard.service.ApplicationsService;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.exchange.ApiKeyExchange;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;

import java.util.Optional;
import java.util.UUID;

public class ApiKeysServiceImpl implements ApiKeysService {
    private final ApplicationsService applicationsService;
    private final ApiKeyExchange apiKeyExchange;
    private final ApiKeysRepository keysRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public ApiKeysServiceImpl(final ApplicationsService applicationsService,
                              final ApiKeyExchange apiKeyExchange,
                              final ApiKeysRepository keysRepository,
                              final ServiceMapper serviceMapper) {
        this.applicationsService = applicationsService;
        this.apiKeyExchange = apiKeyExchange;
        this.keysRepository = keysRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public String generateApiKey(final String appId) {
        final AppBO app = applicationsService.getById(appId)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST,
                        "No app with ID " + appId + " found"));

        return generateApiKey(app);
    }

    @Override
    public String generateApiKey(final AppBO app) {
        final TokensBO token = apiKeyExchange.generateKey(app);
        final ApiKeyDO keyDO = serviceMapper.toDO(token, app);

        keyDO.setId(UUID.randomUUID().toString());

        keysRepository.save(serviceMapper.toDO(token, app));

        return token.getToken().toString();
    }

    @Override
    public Optional<AppBO> validateApiKey(final String key) {
        return apiKeyExchange.verifyAndGetAppId(key)
                .thenApply(optional -> optional.flatMap(applicationsService::getById))
                .join();

    }
}
