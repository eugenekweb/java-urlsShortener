package dev.urls.service.impl;

import dev.urls.config.AppConfig;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import dev.urls.model.ShortUrl;
import dev.urls.repository.UrlRepository;
import dev.urls.service.UrlGeneratorService;
import dev.urls.service.UrlService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class UrlServiceImpl implements UrlService {
    private final UrlRepository urlRepository;
    private final UrlGeneratorService urlGenerator;
    private final AppConfig config;

    public UrlServiceImpl(UrlRepository urlRepository, UrlGeneratorService urlGenerator, AppConfig config) {
        this.urlRepository = urlRepository;
        this.urlGenerator = urlGenerator;
        this.config = config;
    }

    public ShortUrl createShortUrl(String originalUrl, UUID userUuid, Integer customLifetimeHours, Integer customClicksLimit) {
        validateUrl(originalUrl);

        String shortPath;
        do {
            shortPath = urlGenerator.generatePath();
        } while (urlRepository.findByPath(shortPath).isPresent());

        // Определяем время жизни ссылки
        int lifetimeHours = customLifetimeHours != null ?
                Math.min(customLifetimeHours, config.getDefaultLifetimeHours()) :
                config.getDefaultLifetimeHours();

        // Определяем лимит кликов
        int clicksLimit = customClicksLimit != null ?
                Math.max(customClicksLimit, config.getDefaultClicksLimit()) :
                config.getDefaultClicksLimit();

        ShortUrl shortUrl = ShortUrl.builder()
                .id(urlRepository.getNextUrlId())
                .originalUrl(originalUrl)
                .shortPath(shortPath)
                .userUuid(userUuid)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(lifetimeHours))
                .clicksLimit(clicksLimit)
                .clicksCounter(0)
                .isActive(true)
                .build();

        urlRepository.save(shortUrl);
        return shortUrl;
    }

    public Optional<String> getOriginalUrl(String shortPath) {
        return urlRepository.findByPath(shortPath)
                .filter(this::isUrlValid)
                .map(url -> {
                    url.setClicksCounter(url.getClicksCounter() + 1);
                    return url.getOriginalUrl();
                });
    }

    public void updateClicksLimit(String shortPath, UUID userUuid, int newLimit) {
        ShortUrl url = validateUrlOwnership(shortPath, userUuid);
        url.setClicksLimit(newLimit);
        urlRepository.save(url);
    }

    public void deleteUrl(String shortPath, UUID userUuid) {
        validateUrlOwnership(shortPath, userUuid);
        urlRepository.delete(shortPath);
    }

    private boolean isUrlValid(ShortUrl url) {
        if (!url.isActive()) return false;
        if (url.getExpiresAt().isBefore(LocalDateTime.now())) {
            url.setActive(false);
            return false;
        }
        if (url.getClicksCounter() >= url.getClicksLimit()) {
            url.setActive(false);
            return false;
        }
        return true;
    }

    private ShortUrl validateUrlOwnership(String shortPath, UUID userUuid) {
        return urlRepository.findByPath(shortPath)
                .filter(url -> url.getUserUuid().equals(userUuid))
                .orElseThrow(() -> new IllegalArgumentException("URL not found or access denied"));
    }

    private void validateUrl(String url) {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format");
        }
    }

    @Override
    public Optional<ShortUrl> getShortUrl(String shortPath) {
        return urlRepository.findByPath(shortPath);
    }

    @Override
    public String getFullShortUrl(ShortUrl shortUrl) throws URISyntaxException {
        return URI.create(config.getDomain() + "/" + shortUrl.getShortPath()).toString();
    }

    @Override
    public Optional<ShortUrl> findByPath(String shortPath) {
        return urlRepository.findByPath(shortPath);
    }
}
