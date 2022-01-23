package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class PermissionsApi implements ApiRoute {

    @Override
    public String getPath() {
        return "permissions";
    }

    @Override
    public void addEndpoints() {
        post("/", this::create, ActorRoles.adminClient());
        get("/:id", this::getById, ActorRoles.adminClient());
        delete("/:id", this::getById, ActorRoles.adminClient());
        get("/domain/:domain/group/:group", this::getByGroup, ActorRoles.adminClient());
        get("/domain/:domain", this::getAll, ActorRoles.adminClient());
    }

    public abstract void create(final Context context);

    public abstract void getById(final Context context);

    public abstract void deleteById(final Context context);

    public abstract void getByGroup(final Context context);

    public abstract void getAll(final Context context);
}
