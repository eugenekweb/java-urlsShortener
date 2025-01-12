package dev.urls.service;

import dev.urls.config.AppConfig;

import java.util.Random;

public class UrlGeneratorService {
    private static final String PATH_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final Random random = new Random();
    private final int pathLength;

    public UrlGeneratorService(AppConfig config) {
        this.pathLength = config.getShortUrlLength();
    }

    public String generatePath() {
        StringBuilder result = new StringBuilder(pathLength);
        for (int i = 0; i < pathLength; i++) {
            int randomIndex = random.nextInt(PATH_CHARS.length());
            result.append(PATH_CHARS.charAt(randomIndex));
        }
        return result.toString();
    }
}