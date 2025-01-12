package dev.urls.service;

import dev.urls.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserService {
    User createUser(String username);

    Optional<User> findByUuid(UUID uuid);

    Optional<User> findByUsername(String input);
}