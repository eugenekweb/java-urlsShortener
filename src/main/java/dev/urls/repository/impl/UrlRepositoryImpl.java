package dev.urls.repository.impl;

import dev.urls.model.ShortUrl;
import dev.urls.repository.UrlRepository;

import java.util.*;

public class UrlRepositoryImpl implements UrlRepository {
    private final Map<Long, ShortUrl> urlsById = new HashMap<>();
    private long nextUrlId = 0;

    @Override
    public long getNextUrlId() {
        return nextUrlId++;
    }

    @Override
    public void save(ShortUrl shortUrl) {
        urlsById.put(shortUrl.getId(), shortUrl);
    }

    @Override
    public Optional<ShortUrl> findByPath(String path) {
        return urlsById.values().stream()
                .filter(url -> url.getShortPath().equals(path))
                .findFirst();
    }

    @Override
    public List<ShortUrl> findAllUrlsByUserUuid(UUID userUuid) {
        return urlsById.values().stream()
                .filter(url -> url.getUserUuid().equals(userUuid))
                .toList();
    }

    @Override
    public void delete(String path) {
        ShortUrl urlToDelete = findByPath(path)
                .orElse(null);

        if (urlToDelete != null) {
            urlsById.remove(urlToDelete.getId());
        }
    }
}
