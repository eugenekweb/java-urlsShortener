package dev.urls.service;

import dev.urls.model.ShortUrl;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UrlService {
    ShortUrl createShortUrl(String originalUrl, UUID userUuid, Integer maxClicks, Integer lifetimeHours);

    Optional<String> getOriginalUrl(String shortPath);

    void clickShortUrl(String shortPath);

    void updateClicksLimit(String shortPath, UUID userUuid, int newLimit);

    void deleteShortUrl(String shortCode, UUID userUuid);

    boolean isShortUrlExpired(String shortPath);

    boolean isShortUrlActive(String shortPath);

    boolean isShortUrlLimitExceeded(String shortPath);

    Optional<ShortUrl> getShortUrl(String shortPath);

    String getFullShortUrl(ShortUrl shortUrl) throws URISyntaxException;

    Optional<ShortUrl> findByPath(String shortPath);

    List<ShortUrl> findAllUrlsByUuid(UUID userUuid);

    String getShortUrlStatus(ShortUrl shortUrl) throws URISyntaxException;
}


