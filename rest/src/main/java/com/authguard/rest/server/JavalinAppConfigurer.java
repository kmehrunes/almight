package com.authguard.rest.server;

import io.javalin.Javalin;

public interface JavalinAppConfigurer {
    void configure(Javalin app);
}
