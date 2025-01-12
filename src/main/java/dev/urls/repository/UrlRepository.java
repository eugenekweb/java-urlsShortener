package dev.urls.repository;

import dev.urls.model.ShortUrl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UrlRepository {

    long getNextUrlId();

    void save(ShortUrl shortUrl);

    void delete(String path);

    Optional<ShortUrl> findByPath(String path);

    List<ShortUrl> findAllUrlsByUserUuid(UUID uuid);
}
