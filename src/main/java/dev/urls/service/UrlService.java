package dev.urls.service;

import dev.urls.model.ShortUrl;

import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

public interface UrlService {
    ShortUrl createShortUrl(String originalUrl, UUID userUuid, Integer maxClicks, Integer lifetimeHours);

    Optional<String> getOriginalUrl(String shortPath);

    //    String resolveShortUrl(String shortCode);
    void updateClicksLimit(String shortPath, UUID userUuid, int newLimit);

    void deleteUrl(String shortCode, UUID userUuid);

    //    boolean isUrlExpired(String shortCode);
//    boolean isUrlLimitExceeded(String shortCode);
    Optional<ShortUrl> getShortUrl(String shortCode);

    String getFullShortUrl(ShortUrl shortUrl) throws URISyntaxException;

    Optional<ShortUrl> findByPath(String shortPath);
}


