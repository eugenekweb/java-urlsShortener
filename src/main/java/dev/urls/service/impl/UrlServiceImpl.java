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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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

    public void clickShortUrl(String shortPath) {
        ShortUrl shortUrl = urlRepository.findByPath(shortPath).get();
        if (isShortUrlValid(shortPath)) {
            shortUrl.setClicksCounter(shortUrl.getClicksCounter() + 1);
        }
//        urlRepository.findByPath(shortPath)
//                .filter(this::isUrlValid)
//                .map(url -> {
//                    url.setClicksCounter(url.getClicksCounter() + 1);
//                });
    }

    public Optional<String> getOriginalUrl(String shortPath) {
        return Optional.ofNullable(urlRepository.findByPath(shortPath).get().getOriginalUrl());
    }

    public void updateClicksLimit(String shortPath, UUID userUuid, int newLimit) {
        ShortUrl url = checkShortUrlOwner(shortPath, userUuid);
        url.setClicksLimit(newLimit);
        urlRepository.save(url);
    }

    public void deleteShortUrl(String shortPath, UUID userUuid) {
        checkShortUrlOwner(shortPath, userUuid);
        urlRepository.delete(shortPath);
    }

    @Override
    public boolean isShortUrlExpired(String shortPath) {
        return getShortUrl(shortPath).get().getExpiresAt().isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isShortUrlActive(String shortPath) {
        return getShortUrl(shortPath).get().isActive();
    }

    @Override
    public boolean isShortUrlLimitExceeded(String shortPath) {
        ShortUrl shortUrl = getShortUrl(shortPath).get();
        return shortUrl.getClicksCounter() >= shortUrl.getClicksLimit();
    }


    private boolean isShortUrlValid(String shortPath) {
        if (!isShortUrlActive(shortPath)) {
            System.out.println("Ссылка не активна");
            return false;
        }
        if (isShortUrlExpired(shortPath)) {
            urlRepository.delete(shortPath);
            System.out.println("Ссылка устарела и была удалена");
            return false;
        }
        if (isShortUrlLimitExceeded(shortPath)) {
            findByPath(shortPath).get().setActive(false);
            System.out.println("Ссылка достигла или уже превышает лимит кликов");
            return false;
        }
        return true;
    }

    private ShortUrl checkShortUrlOwner(String shortPath, UUID userUuid) {
        return urlRepository.findByPath(shortPath)
                .filter(url -> url.getUserUuid().equals(userUuid))
                .orElseThrow(() -> new IllegalArgumentException("Такой ссылки нет, либо это не Ваша ссылка"));
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

    @Override
    public List<ShortUrl> findAllUrlsByUuid(UUID userUuid) {
        return urlRepository.findAllUrlsByUserUuid(userUuid);
    }

    @Override
    public String getShortUrlStatus(ShortUrl shortUrl) throws URISyntaxException {
        String delimiter = "\t|\t";
        int clicksLeft = shortUrl.getClicksLimit() - shortUrl.getClicksCounter();
        Duration lifeLeft = Duration.between(LocalDateTime.now(), shortUrl.getExpiresAt());
        long hours = lifeLeft.toHours();
        long minutes = lifeLeft.minusHours(hours).toMinutes();
        String lifeLeftFormat = String.format("%d ч. %d мин.", hours, minutes);
        return getFullShortUrl(shortUrl) + delimiter
                + (shortUrl.isActive() ? "Активна" : "Неактивна") + delimiter
                + "Осталось кликов: " + clicksLeft + delimiter
                + "Осталось время жизни (ч): " + lifeLeftFormat + delimiter
                + "Оригинальная ссылка: " + shortUrl.getOriginalUrl();
    }
}
