package dev.urls.service;

import dev.urls.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {
    User createUser(String username);

    List<User> findByUsernamePattern(String pattern);

    Optional<User> findByUuid(UUID uuid);

    Optional<User> findByUsername(String input);
}