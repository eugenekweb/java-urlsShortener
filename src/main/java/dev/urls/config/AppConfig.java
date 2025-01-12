package dev.urls.config;

import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final String CONFIG_FILE = "application.properties";
    private final Properties properties;

    @Getter
    private String domain;
    @Getter
    private int shortUrlLength;
    @Getter
    private int defaultLifetimeHours;
    @Getter
    private int defaultLifetimeMin;
    @Getter
    private int defaultClicksLimit;

    public AppConfig() {
        properties = new Properties();
        loadProperties();
        initializeFields();
    }

    @SneakyThrows(IOException.class)
    private void loadProperties() {
        @Cleanup @NonNull
        InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
        properties.load(input);
    }

    private void initializeFields() {
        domain = properties.getProperty("url.domain");
        shortUrlLength = Integer.parseInt(properties.getProperty("url.length"));
        defaultLifetimeHours = Integer.parseInt(properties.getProperty("url.default.lifetime.hours"));
        defaultLifetimeMin = Integer.parseInt(properties.getProperty("url.default.lifetime.min"));
        defaultClicksLimit = Integer.parseInt(properties.getProperty("url.default.clicks.limit"));
    }
}