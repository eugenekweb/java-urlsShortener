package dev.urls.repository;

import dev.urls.model.ShortUrl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UrlRepository {
    long getNextUrlId();
    void save(ShortUrl shortUrl);
    Optional<ShortUrl> findById(Long id);
    Optional<ShortUrl> findByPath(String path);
    List<ShortUrl> findByUserUuid(UUID path);
    void delete(String path);
}
