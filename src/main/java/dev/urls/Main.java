package dev.urls;

import dev.urls.config.AppConfig;
import dev.urls.repository.UrlRepository;
import dev.urls.repository.UserRepository;
import dev.urls.repository.impl.UrlRepositoryImpl;
import dev.urls.repository.impl.UserRepositoryImpl;
import dev.urls.service.UrlGeneratorService;
import dev.urls.service.impl.UrlServiceImpl;
import dev.urls.service.impl.UserServiceImpl;
import dev.urls.ui.ConsoleUI;
import lombok.val;

import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) throws URISyntaxException {
        AppConfig config = new AppConfig();
        UrlRepository urlRepository = new UrlRepositoryImpl();
        UserRepository userRepository = new UserRepositoryImpl();
        UrlGeneratorService urlGenerator = new UrlGeneratorService(config);

        // Создаем сервисы
        val urlService = new UrlServiceImpl(urlRepository, urlGenerator, config);
        val userService = new UserServiceImpl(userRepository);

        // Создаем и запускаем консольный интерфейс
        ConsoleUI consoleUI = new ConsoleUI(userService, urlService, config);
        consoleUI.start();
    }
}

