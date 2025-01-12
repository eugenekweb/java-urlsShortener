package dev.urls.repository.impl;

import dev.urls.model.User;
import dev.urls.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

public class UserRepositoryImpl implements UserRepository {
    private final Map<UUID, User> users = new HashMap<>();

    @Override
    public void save(User user) {
        users.put(user.getUUID(), user);
    }

    @Override
    public Optional<User> findByUuid(UUID uuid) {
        return Optional.ofNullable(users.get(uuid));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.values().stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
    }
}
