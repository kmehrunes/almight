package com.authguard.service.impl;

import com.authguard.dal.CredentialsAuditRepository;
import com.authguard.dal.CredentialsRepository;
import com.authguard.service.SecurePassword;
import com.authguard.dal.model.CredentialsAuditDO;
import com.authguard.dal.model.CredentialsDO;
import com.authguard.service.exceptions.ServiceConflictException;
import com.authguard.service.impl.mappers.ServiceMapperImpl;
import com.authguard.service.model.CredentialsBO;
import com.authguard.service.model.HashedPasswordBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CredentialsServiceImplTest {
    private CredentialsRepository credentialsRepository;
    private CredentialsAuditRepository credentialsAuditRepository;
    private SecurePassword securePassword;
    private CredentialsServiceImpl credentialsService;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        credentialsRepository = Mockito.mock(CredentialsRepository.class);
        credentialsAuditRepository = Mockito.mock(CredentialsAuditRepository.class);
        securePassword = Mockito.mock(SecurePassword.class);
        credentialsService = new CredentialsServiceImpl(credentialsRepository, credentialsAuditRepository, securePassword, new ServiceMapperImpl());
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(credentialsRepository);
        Mockito.reset(credentialsAuditRepository);
    }

    @Test
    void create() {
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class);
        final HashedPasswordBO hashedPassword = RANDOM.nextObject(HashedPasswordBO.class);

        Mockito.when(securePassword.hash(any())).thenReturn(hashedPassword);
        Mockito.when(credentialsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0, CredentialsDO.class));

        final CredentialsBO persisted = credentialsService.create(credentials);

        assertThat(persisted).isNotNull();
        assertThat(persisted).isEqualToIgnoringGivenFields(credentials, "id", "plainPassword", "hashedPassword");
        assertThat(persisted.getHashedPassword()).isNull();
    }

    @Test
    void createDuplicateUsername() {
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class);

        Mockito.when(credentialsRepository.findByUsername(credentials.getUsername())).thenReturn(Optional.of(CredentialsDO.builder().build()));

        assertThatThrownBy(() -> credentialsService.create(credentials)).isInstanceOf(ServiceConflictException.class);
    }

    @Test
    void getById() {
        final CredentialsDO credentials = RANDOM.nextObject(CredentialsDO.class);

        Mockito.when(credentialsRepository.getById(any())).thenReturn(Optional.of(credentials));

        final Optional<CredentialsBO> retrieved = credentialsService.getById("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(credentials, "hashedPassword", "plainPassword");
        assertThat(retrieved.get().getHashedPassword()).isNull();
        assertThat(retrieved.get().getPlainPassword()).isNull();
    }

    @Test
    void getByIdNotFound() {
        Mockito.when(credentialsRepository.getById(any())).thenReturn(Optional.empty());

        assertThat(credentialsService.getById("")).isEmpty();
    }

    @Test
    void getByUsername() {
        final CredentialsDO credentials = RANDOM.nextObject(CredentialsDO.class);

        Mockito.when(credentialsRepository.findByUsername(any())).thenReturn(Optional.of(credentials));

        final Optional<CredentialsBO> retrieved = credentialsService.getByUsername("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(credentials, "hashedPassword", "plainPassword");
        assertThat(retrieved.get().getHashedPassword()).isNull();
        assertThat(retrieved.get().getPlainPassword()).isNull();
    }

    @Test
    void getByUsernameNotFound() {
        Mockito.when(credentialsRepository.findByUsername(any())).thenReturn(Optional.empty());

        assertThat(credentialsService.getById("")).isEmpty();
    }

    @Test
    void update() {
        final CredentialsDO credentials = RANDOM.nextObject(CredentialsDO.class);
        final CredentialsBO update = RANDOM.nextObject(CredentialsBO.class).withId(credentials.getId());

        Mockito.when(credentialsRepository.getById(credentials.getId())).thenReturn(Optional.of(credentials));
        Mockito.when(credentialsRepository.update(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0, CredentialsDO.class)));
        Mockito.when(credentialsAuditRepository.save(any())).thenAnswer(invocation ->
                invocation.getArgument(0, CredentialsAuditDO.class));

        final Optional<CredentialsBO> result = credentialsService.update(update);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(update.getUsername());
        assertThat(result.get().getHashedPassword()).isNull();
        assertThat(result.get().getPlainPassword()).isNull();

        // verify call to audit repository
        final ArgumentCaptor<CredentialsAuditDO> argumentCaptor = ArgumentCaptor.forClass(CredentialsAuditDO.class);
        Mockito.verify(credentialsAuditRepository, Mockito.times(2)).save(argumentCaptor.capture());

        final List<CredentialsAuditDO> auditArgs = argumentCaptor.getAllValues();

        assertThat(auditArgs.size()).isEqualTo(2);

        assertThat(auditArgs.get(0)).isNotNull();
        assertThat(auditArgs.get(0).getCredentialId()).isEqualTo(credentials.getId());
        assertThat(auditArgs.get(0).getAction()).isEqualTo(CredentialsAuditDO.Action.ATTEMPT);
        assertThat(auditArgs.get(0).getUsername()).isEqualTo(credentials.getUsername());
        assertThat(auditArgs.get(0).getPassword()).isNull();

        assertThat(auditArgs.get(0)).isNotNull();
        assertThat(auditArgs.get(1).getCredentialId()).isEqualTo(credentials.getId());
        assertThat(auditArgs.get(1).getAction()).isEqualTo(CredentialsAuditDO.Action.UPDATED);
        assertThat(auditArgs.get(1).getUsername()).isEqualTo(credentials.getUsername());
        assertThat(auditArgs.get(1).getPassword()).isNull();
    }

    @Test
    void updatePassword() {
        final CredentialsDO credentials = RANDOM.nextObject(CredentialsDO.class);
        final CredentialsBO update = RANDOM.nextObject(CredentialsBO.class).withId(credentials.getId());

        Mockito.when(credentialsRepository.getById(credentials.getId())).thenReturn(Optional.of(credentials));
        Mockito.when(credentialsRepository.update(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0, CredentialsDO.class)));
        Mockito.when(credentialsAuditRepository.save(any())).thenAnswer(invocation ->
                invocation.getArgument(0, CredentialsAuditDO.class));

        final Optional<CredentialsBO> result = credentialsService.updatePassword(update);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(update.getUsername());
        assertThat(result.get().getHashedPassword()).isNull();
        assertThat(result.get().getPlainPassword()).isNull();

        // verify call to audit repository
        final ArgumentCaptor<CredentialsAuditDO> argumentCaptor = ArgumentCaptor.forClass(CredentialsAuditDO.class);
        Mockito.verify(credentialsAuditRepository, Mockito.times(2)).save(argumentCaptor.capture());

        final List<CredentialsAuditDO> auditArgs = argumentCaptor.getAllValues();

        assertThat(auditArgs.size()).isEqualTo(2);

        assertThat(auditArgs.get(0)).isNotNull();
        assertThat(auditArgs.get(0).getCredentialId()).isEqualTo(credentials.getId());
        assertThat(auditArgs.get(0).getAction()).isEqualTo(CredentialsAuditDO.Action.ATTEMPT);
        assertThat(auditArgs.get(0).getUsername()).isEqualTo(credentials.getUsername());
        assertThat(auditArgs.get(0).getPassword()).isNull();

        assertThat(auditArgs.get(0)).isNotNull();
        assertThat(auditArgs.get(1).getCredentialId()).isEqualTo(credentials.getId());
        assertThat(auditArgs.get(1).getAction()).isEqualTo(CredentialsAuditDO.Action.UPDATED);
        assertThat(auditArgs.get(1).getUsername()).isEqualTo(credentials.getUsername());
        assertThat(auditArgs.get(1).getPassword()).isNotNull();
    }

    @Test
    void updateDuplicateUsername() {
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class);

        Mockito.when(credentialsRepository.findByUsername(credentials.getUsername())).thenReturn(Optional.of(CredentialsDO.builder().build()));

        assertThatThrownBy(() -> credentialsService.update(credentials)).isInstanceOf(ServiceConflictException.class);
    }
}
