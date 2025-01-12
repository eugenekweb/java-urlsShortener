package dev.urls.model;

import lombok.*;

import java.util.UUID;

@Builder
@EqualsAndHashCode
@ToString
public class User {
    @Getter private final UUID UUID;
    @Getter @Setter private String username;

    public User(UUID uuid, String username) {
        this.UUID = uuid;
        this.username = username;
    }
}
