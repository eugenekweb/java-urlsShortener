package dev.urls.service.impl;

import dev.urls.model.User;
import dev.urls.repository.UserRepository;
import dev.urls.service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(String username) {
        User user = User.builder()
                .UUID(UUID.randomUUID())
                .username(username)
                .build();

        userRepository.save(user);
        return user;
    }

    @Override
    public List<User> findByUsernamePattern(String pattern) {
        return userRepository.findByUsernamePattern(pattern);
    }

    @Override
    public Optional<User> findByUuid(UUID uuid) {
        return userRepository.findByUuid(uuid);
    }

    @Override
    public Optional<User> findByUsername(String input) {
        return userRepository.findByUsername(input);
    }
}
