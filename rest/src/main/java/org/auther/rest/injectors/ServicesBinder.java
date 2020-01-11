package org.auther.rest.injectors;

import com.auther.config.ConfigContext;
import com.google.inject.AbstractModule;
import org.auther.service.*;
import org.auther.service.impl.*;
import org.auther.service.impl.passwords.SCryptPassword;

public class ServicesBinder extends AbstractModule {

    private final ConfigContext rootConfig;

    public ServicesBinder(final ConfigContext rootConfig) {
        this.rootConfig = rootConfig;
    }

    @Override
    public void configure() {
        bind(CredentialsService.class).to(CredentialsServiceImpl.class);
        bind(AuthenticationService.class).to(AuthenticationServiceImpl.class);
        bind(AuthorizationService.class).to(AuthorizationServiceImpl.class);
        bind(AccountsService.class).to(AccountsServiceImpl.class);
        bind(ApplicationsService.class).to(ApplicationsServiceImpl.class);
        bind(ApiKeysService.class).to(ApiKeysServiceImpl.class);
        bind(PermissionsService.class).to(PermissionsServiceImpl.class);
        bind(RolesService.class).to(RolesServiceImpl.class);

        bind(SecurePassword.class).to(SCryptPassword.class);

        if (rootConfig.get("otp") != null) {
            bind(OtpService.class).to(OtpServiceImpl.class);
        }
    }
}
