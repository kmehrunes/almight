package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.PermissionDO;
import com.nexblocks.authguard.dal.persistence.PermissionsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.PermissionsService;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PermissionsServiceImpl implements PermissionsService {
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
    public PermissionBO create(final PermissionBO permission) {
        if (permissionsRepository.search(permission.getGroup(), permission.getName(), permission.getDomain()).join().isPresent()) {
            throw new ServiceConflictException(ErrorCode.PERMISSION_ALREADY_EXIST,
                    "Permission " + permission.getFullName() + " already exists");
        }
        return persistenceService.create(permission);
    }

    @Override
    public Optional<PermissionBO> getById(final String id) {
        return persistenceService.getById(id);
    }

    @Override
    public Optional<PermissionBO> update(final PermissionBO entity) {
        throw new UnsupportedOperationException("Permissions cannot be updated");
    }

    @Override
    public List<PermissionBO> validate(final List<PermissionBO> permissions, final String domain) {
        return permissions.stream()
                .map(permission -> permissionsRepository.search(permission.getGroup(), permission.getName(), domain)
                        .join()
                        .map(serviceMapper::toBO))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionBO> getAll(final String domain) {
        return permissionsRepository.getAll(domain).join()
                .stream()
                .map(serviceMapper::toBO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionBO> getAllForGroup(final String group, final String domain) {
        return permissionsRepository.getAllForGroup(group, domain)
                .join()
                .stream()
                .map(serviceMapper::toBO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PermissionBO> delete(final String id) {
        return persistenceService.delete(id);
    }
}
