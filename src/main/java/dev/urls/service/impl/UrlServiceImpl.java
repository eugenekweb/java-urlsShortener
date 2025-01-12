package dev.urls.service.impl;

import dev.urls.config.AppConfig;

import java.awt.*;
import java.io.IOException;
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
        try {
            Desktop.getDesktop().browse(new URI(shortUrl.getOriginalUrl()));
            System.out.println("Ссылка открыта в браузере");
        } catch (URISyntaxException | IOException e) {
            System.out.println("Ошибка открытия ссылки: " + e.getMessage());
        }
    }

    private Optional<String> getOriginalUrl(String shortPath) {
        return Optional.ofNullable(urlRepository.findByPath(shortPath).get().getOriginalUrl());
    }

    @Override
    public UUID getShortUrlOwner(String shortPath) {
        return findByPath(shortPath).get().getUserUuid();
    }

    public void updateUrlClicksLimit(String shortPath, int newLimit) {
        if (isShortUrlExpired(shortPath)) return;
        ShortUrl url = findByPath(shortPath).get();
        url.setClicksLimit(newLimit);
        url.setClicksCounter(0);
        url.setActive(true);
        isShortUrlValid(shortPath);
//        urlRepository.save(url); //?
    }

    @Override
    public void updateUrlLifeTime(String shortPath, int hours) {
        if (isShortUrlExpired(shortPath)) return;
        ShortUrl url = findByPath(shortPath).get();
        url.setExpiresAt(LocalDateTime.now().plusHours(hours));
        isShortUrlValid(shortPath);
//        urlRepository.save(url); //?
    }

    public void deleteShortUrl(String shortPath) {
        urlRepository.delete(shortPath);
    }

    private boolean isShortUrlExpired(String shortPath) {
        if (getShortUrl(shortPath).get().getExpiresAt().isAfter(LocalDateTime.now())) {
            return false;
        } else {
            deleteShortUrl(shortPath);
            System.out.println("Ссылка устарела и была удалена");
            return true;
        }
    }

    private boolean isShortUrlActive(String shortPath) {
        if (getShortUrl(shortPath).get().isActive()) {
            return true;
        } else {
            System.out.println("Ссылка не активна");
            return false;
        }
    }

    private boolean isShortUrlLimitExceeded(String shortPath) {
        ShortUrl shortUrl = getShortUrl(shortPath).get();
        if (shortUrl.getClicksCounter() >= shortUrl.getClicksLimit()) {
            shortUrl.setActive(false);
            System.out.println("Ссылка достигла или уже превышает лимит кликов");
            return false;
        }
        return true;
    }


    private boolean isShortUrlValid(String shortPath) {
        if (!isShortUrlActive(shortPath)) return false;
        if (isShortUrlExpired(shortPath)) return false;
        if (isShortUrlLimitExceeded(shortPath)) return false;
        return true;
    }

     private void validateUrl(String url) {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Неверный формат URL");
        }
    }

    @Override
    public Optional<ShortUrl> getShortUrl(String shortPath) {
        return urlRepository.findByPath(shortPath);
    }

    @Override
    public String getFullShortUrl(ShortUrl shortUrl) {
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
    public String getShortUrlStatus(ShortUrl shortUrl) {
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
