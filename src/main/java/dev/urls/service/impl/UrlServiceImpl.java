package dev.urls.service.impl;

import dev.urls.config.AppConfig;
import dev.urls.model.ShortUrl;
import dev.urls.repository.UrlRepository;
import dev.urls.service.UrlGeneratorService;
import dev.urls.service.UrlService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

    @Override
    public ShortUrl createShortUrl(String originalUrl, UUID userUuid, Integer customLifetimeHours, Integer customClicksLimit) {
        validateUrl(originalUrl);

        String shortPath;
        do {
            shortPath = urlGenerator.generatePath();
        } while (urlRepository.findByPath(shortPath).isPresent());

        // Вычисляем время жизни ссылки
        int lifetimeHours = customLifetimeHours != null ?
                Math.min(customLifetimeHours, config.getDefaultLifetimeHours()) :
                config.getDefaultLifetimeHours();

        // Вычисляем лимит кликов
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

    @Override
    public void clickShortUrl(String shortPath) {
        ShortUrl shortUrl = urlRepository.findByPath(shortPath).get();
        if (isShortUrlValid(shortPath, false)) {
            int clickCounter = shortUrl.getClicksCounter();
            shortUrl.setClicksCounter(++clickCounter);
            urlRepository.save(shortUrl);
        } else return;
        try {
            Desktop.getDesktop().browse(new URI(shortUrl.getOriginalUrl()));
            System.out.println("Ссылка открыта в браузере");
        } catch (URISyntaxException | IOException e) {
            System.out.println("Ошибка открытия ссылки: " + e.getMessage());
        }
        isShortUrlValid(shortPath, true);
    }

    @Override
    public UUID getShortUrlOwner(String shortPath) {
        return findByPath(shortPath).get().getUserUuid();
    }

    @Override
    public Optional<ShortUrl> getShortUrl(String shortPath) {
        return urlRepository.findByPath(shortPath);
    }

    @Override
    public String getFullShortUrl(String shortPath) {
        return URI.create(config.getDomain() + "/" + shortPath).toString();
    }

    @Override
    public Optional<ShortUrl> findByPath(String shortPath) {
        Optional<ShortUrl> shortUrl = urlRepository.findByPath(shortPath);
        if (shortUrl.isPresent()) isShortUrlValid(shortPath, true);
        return shortUrl;
    }

    @Override
    public List<ShortUrl> findAllUrlsByUuid(UUID userUuid) {
        return urlRepository.findAllUrlsByUserUuid(userUuid).stream()
                .filter(shortUrl -> !this.isShortUrlExpired(shortUrl.getShortPath(), true))
                .toList();
    }

    @Override
    public String getShortUrlStatus(ShortUrl shortUrl) {
        String delimiter = "\t|\t";
        int clicksLeft = shortUrl.getClicksLimit() - shortUrl.getClicksCounter();
        Duration lifeLeft = Duration.between(LocalDateTime.now(), shortUrl.getExpiresAt());
        long days = lifeLeft.toDays();
        long hours = lifeLeft.minusDays(days).toHours();
        long minutes = lifeLeft.minusDays(days).minusHours(hours).toMinutes();
        String lifeLeftFormat = String.format("%d дн. %d ч. %d мин.", days, hours, minutes);
        return getFullShortUrl(shortUrl.getShortPath()) + delimiter
                + (shortUrl.isActive() ? "Активна" : "Неактивна") + delimiter
                + "Осталось кликов: " + clicksLeft + delimiter
                + "Осталось время жизни: " + lifeLeftFormat + delimiter
                + "Оригинальная ссылка: " + shortUrl.getOriginalUrl();
    }

    @Override
    public void updateUrlClicksLimit(String shortPath, int newLimit) {
        if (isShortUrlExpired(shortPath, false)) return;
        ShortUrl url = findByPath(shortPath).get();
        url.setClicksLimit(newLimit);
        url.setClicksCounter(0);
        url.setActive(true);
        urlRepository.save(url);
        isShortUrlValid(shortPath, true);
    }

    @Override
    public void updateUrlLifeTime(String shortPath, int hours) {
        if (isShortUrlExpired(shortPath, false)) return;
        ShortUrl url = findByPath(shortPath).get();
        url.setExpiresAt(LocalDateTime.now().plusHours(hours));
        urlRepository.save(url);
        isShortUrlValid(shortPath, true);
    }

    @Override
    public void deleteShortUrl(String shortPath) {
        urlRepository.delete(shortPath);
    }

    private boolean isShortUrlExpired(String shortPath, boolean silent) {
        if (getShortUrl(shortPath).get().getExpiresAt().isAfter(LocalDateTime.now())) {
            return false;
        } else {
            silent = false; // Лучше всегда уведомлять об удалении ссылки
            if (!silent) System.out.println("Ссылка '" + getFullShortUrl(shortPath) + "' устарела и была удалена");
            deleteShortUrl(shortPath);
            return true;
        }
    }

    private boolean isShortUrlActive(String shortPath, boolean silent) {
        if (getShortUrl(shortPath).get().isActive()) {
            return true;
        } else {
            if (!silent) System.out.println("Ссылка не активна");
            return false;
        }
    }

    private boolean isShortUrlLimitExceeded(String shortPath, boolean silent) {
        ShortUrl shortUrl = getShortUrl(shortPath).get();
        if (shortUrl.getClicksCounter() >= shortUrl.getClicksLimit()) {
            shortUrl.setActive(false);
            urlRepository.save(shortUrl);
            if (!silent) System.out.println("Ссылка достигла или уже превышает лимит кликов");
            return true;
        }
        return false;
    }

    // Метод проверяет доступность ссылки и сразу обновляет данные по ней
    // silent - молчаливый режим, чтобы не падало слишком много уведомлений на одно и то же событие
    private boolean isShortUrlValid(String shortPath, boolean silent) {
        return (isShortUrlActive(shortPath, silent)
                & !isShortUrlLimitExceeded(shortPath, silent)
                & !isShortUrlExpired(shortPath, silent));
    }

    private void validateUrl(String url) {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Неверный формат URL");
        }
    }
}
