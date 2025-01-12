package dev.urls.service;

import dev.urls.model.ShortUrl;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UrlService {
    ShortUrl createShortUrl(String originalUrl, UUID userUuid, Integer maxClicks, Integer lifetimeHours);

    void deleteShortUrl(String shortCode);

    Optional<ShortUrl> findByPath(String shortPath);

    List<ShortUrl> findAllUrlsByUuid(UUID userUuid);

    Optional<ShortUrl> getShortUrl(String shortPath);

    UUID getShortUrlOwner(String shortPath);

    String getFullShortUrl(String shortPath);

    String getShortUrlStatus(ShortUrl shortUrl) throws URISyntaxException;

    void clickShortUrl(String shortPath);

    void updateUrlClicksLimit(String shortPath, int newLimit);

    void updateUrlLifeTime(String shortPath, int hours);
}


