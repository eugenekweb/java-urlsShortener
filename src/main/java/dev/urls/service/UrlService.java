package dev.urls.service;

import dev.urls.model.ShortUrl;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UrlService {
    ShortUrl createShortUrl(String originalUrl, UUID userUuid, Integer maxClicks, Integer lifetimeHours);

//    Optional<String> getOriginalUrl(String shortPath);

    UUID getShortUrlOwner(String shortPath);

    void clickShortUrl(String shortPath);

    void updateUrlClicksLimit(String shortPath, int newLimit);

    void updateUrlLifeTime(String shortPath, int hours);

    void deleteShortUrl(String shortCode);

//    boolean isShortUrlValid(String shortPath);

    Optional<ShortUrl> getShortUrl(String shortPath);

    String getFullShortUrl(ShortUrl shortUrl) throws URISyntaxException;

    Optional<ShortUrl> findByPath(String shortPath);

    List<ShortUrl> findAllUrlsByUuid(UUID userUuid);

    String getShortUrlStatus(ShortUrl shortUrl) throws URISyntaxException;
}


