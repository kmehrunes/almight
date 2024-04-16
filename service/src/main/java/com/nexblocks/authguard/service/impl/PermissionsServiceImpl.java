package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.PermissionDO;
import com.nexblocks.authguard.dal.persistence.LongPage;
import com.nexblocks.authguard.dal.persistence.Page;
import com.nexblocks.authguard.dal.persistence.PermissionsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.PermissionBO;
import com.nexblocks.authguard.service.model.RoleBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PermissionsServiceImpl implements PermissionsService {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionsServiceImpl.class);

    private static final String PERMISSIONS_CHANNEL = "permissions";

    private final PermissionsRepository permissionsRepository;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<PermissionBO, PermissionDO, PermissionsRepository> persistenceService;

    @Inject
    public PermissionsServiceImpl(final PermissionsRepository permissionsRepository,
                                  final ServiceMapper serviceMapper,
                                  final MessageBus messageBus) {
        this.permissionsRepository = permissionsRepository;
        this.serviceMapper = serviceMapper;

        this.persistenceService = new PersistenceService<>(permissionsRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, PERMISSIONS_CHANNEL);
    }

    @Override
    public CompletableFuture<PermissionBO> create(final PermissionBO permission) {
        LOG.debug("New permission request. permission={}, domain={}", permission.getFullName(), permission.getDomain());

        if (permissionsRepository.search(permission.getGroup(), permission.getName(), permission.getDomain()).join().isPresent()) {
            throw new ServiceConflictException(ErrorCode.PERMISSION_ALREADY_EXIST,
                    "Permission " + permission.getFullName() + " already exists");
        }

        return persistenceService.create(permission);
    }

    @Override
    public CompletableFuture<Optional<PermissionBO>> getById(final long id, String domain) {
        return persistenceService.getById(id);
    }

    @Override
    public CompletableFuture<Optional<PermissionBO>> update(final PermissionBO permission, final String domain) {
        return getById(permission.getId(), domain)
                .thenCompose(opt -> {
                    if (opt.isEmpty()) {
                        return CompletableFuture.completedFuture(opt);
                    }

                    PermissionBO newPermission = PermissionBO.builder()
                            .from(opt.get())
                            .forAccounts(permission.isForAccounts())
                            .forApplications(permission.isForApplications())
                            .build();

                    return persistenceService.update(newPermission);
                });
    }

    @Override
    public List<PermissionBO> validate(final Collection<PermissionBO> permissions, final String domain, EntityType entityType) {
        return permissions.stream()
                .map(permission -> permissionsRepository.search(permission.getGroup(), permission.getName(), domain)
                        .join()
                        .map(serviceMapper::toBO))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(permission -> {
                    switch (entityType) {
                        case ACCOUNT: return permission.isForAccounts();
                        case APPLICATION: return permission.isForApplications();
                        default: return false;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<List<PermissionBO>> getAll(final String domain, final Long cursor) {
        return permissionsRepository.getAll(domain, LongPage.of(cursor, 20))
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<List<PermissionBO>> getAllForGroup(final String group, final String domain,
                                                                final Long cursor) {
        return permissionsRepository.getAllForGroup(group, domain, LongPage.of(cursor, 20))
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Optional<PermissionBO>> get(final String domain, final String group, final String name) {
        return permissionsRepository.search(group, name, domain)
                .thenApply(opt -> opt.map(serviceMapper::toBO));
    }

    @Override
    public CompletableFuture<Optional<PermissionBO>> delete(final long id, String domain) {
        LOG.info("Request to delete permission. permissionId={}", id);

        return persistenceService.delete(id);
    }
}
