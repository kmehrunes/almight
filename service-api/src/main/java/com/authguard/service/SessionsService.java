package com.authguard.service;

import com.authguard.service.model.SessionBO;

import java.util.Optional;

public interface SessionsService {
    SessionBO create(SessionBO session);

    Optional<SessionBO> getById(String id);

    Optional<SessionBO> getByToken(String token);
}
