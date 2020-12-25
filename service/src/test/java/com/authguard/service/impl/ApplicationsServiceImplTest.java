package com.authguard.service.impl;

import com.authguard.dal.persistence.ApplicationsRepository;
import com.authguard.emb.MessageBus;
import com.authguard.service.AccountsService;
import com.authguard.service.ApplicationsService;
import com.authguard.dal.model.AppDO;
import com.authguard.service.IdempotencyService;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.PermissionBO;
import com.authguard.service.model.RequestContextBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ApplicationsServiceImplTest {
    private final EasyRandom random = new EasyRandom(new EasyRandomParameters()
            .collectionSizeRange(1, 4));

    private ApplicationsRepository applicationsRepository;
    private ApplicationsService applicationsService;
    private AccountsService accountsService;
    private IdempotencyService idempotencyService;
    private MessageBus messageBus;

    @BeforeEach
    void setup() {
        applicationsRepository = Mockito.mock(ApplicationsRepository.class);
        accountsService = Mockito.mock(AccountsService.class);
        idempotencyService = Mockito.mock(IdempotencyService.class);
        messageBus = Mockito.mock(MessageBus.class);

        applicationsService = new ApplicationsServiceImpl(applicationsRepository, accountsService,
                idempotencyService, new ServiceMapperImpl(), messageBus);
    }

    @Test
    void create() {
        final AppBO app = random.nextObject(AppBO.class);

        final String idempotentKey = "idempotent-key";
        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(accountsService.getById(app.getParentAccountId()))
                .thenReturn(Optional.of(random.nextObject(AccountBO.class)));

        Mockito.when(applicationsRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AppDO.class)));

        Mockito.when(idempotencyService.performOperation(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(app.getEntityType())))
                .thenAnswer(invocation -> {
                    return CompletableFuture.completedFuture(invocation.getArgument(0, Supplier.class).get());
                });

        final AppBO created = applicationsService.create(app, requestContext);

        assertThat(created).isEqualToIgnoringGivenFields(app, "id", "entityType");

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("apps"), any());
    }

    @Test
    void getById() {
        final AppDO app = random.nextObject(AppDO.class);

        app.setDeleted(false);

        Mockito.when(applicationsRepository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(app)));

        final Optional<AppBO> retrieved = applicationsService.getById("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(app,
                "permissions", "entityType");
        assertThat(retrieved.get().getPermissions()).containsExactly(app.getPermissions().stream()
                .map(permissionDO -> PermissionBO.builder()
                        .group(permissionDO.getGroup())
                        .name(permissionDO.getName())
                        .build()
                ).toArray(PermissionBO[]::new));
    }

    @Test
    void delete() {
        final AppDO app = random.nextObject(AppDO.class);

        app.setDeleted(false);

        Mockito.when(applicationsRepository.delete(app.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(app)));

        applicationsService.delete(app.getId());

        Mockito.verify(applicationsRepository).delete(app.getId());
    }

    @Test
    void activate() {
        final AppDO app = random.nextObject(AppDO.class);

        app.setActive(false);

        Mockito.when(applicationsRepository.getById(app.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(app)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AppDO.class))));

        final AppBO updated = applicationsService.activate(app.getId()).orElse(null);

        assertThat(updated).isNotNull();
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    void deactivate() {
        final AppDO app = random.nextObject(AppDO.class);

        app.setActive(true);

        Mockito.when(applicationsRepository.getById(app.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(app)));
        Mockito.when(applicationsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AppDO.class))));

        final AppBO updated = applicationsService.deactivate(app.getId()).orElse(null);

        assertThat(updated).isNotNull();
        assertThat(updated.isActive()).isFalse();
    }
}