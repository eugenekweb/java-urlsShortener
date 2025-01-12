package dev.urls.repository;

import dev.urls.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    void save(User user);
    Optional<User> findByUuid(UUID uuid);
    Optional<User> findByUsername(String username);
}
