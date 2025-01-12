package dev.urls.ui;

import dev.urls.config.AppConfig;
import dev.urls.model.ShortUrl;
import dev.urls.model.User;
import dev.urls.service.UserService;
import dev.urls.service.impl.UrlServiceImpl;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleUI {
    private final Scanner scanner;
    private final UserService userService;
    private final UrlServiceImpl urlService;
    private final AppConfig config;
    private User currentUser;

    public ConsoleUI(UserService userService, UrlServiceImpl urlShortenerService, AppConfig config) {
        this.config = config;
        this.scanner = new Scanner(System.in);
        this.userService = userService;
        this.urlService = urlShortenerService;
    }


    public void start() {
        boolean running = true;
        while (running) {
            if (currentUser == null) {
                showLoginMenu();
            } else {
                running = showMainMenu();
            }
        }
        System.out.println("Программа завершена");
    }

    private void showLoginMenu() {
        System.out.println("\n=== Меню входа ===");
        System.out.println("1. Войти и создать ссылку / управление ссылками");
        System.out.println("2. Зарегистрироваться и создать ссылку");
        System.out.println("3. Завершить работу (close)");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> login();
            case "2" -> register("");
            case "3", "close" -> System.exit(0);
            default -> System.out.println("Что-то пошло не так! Повторите команду.");
        }
    }

    private boolean showMainMenu() {
        System.out.println("\n=== Главное меню ===");
        System.out.println("Пользователь: " + currentUser.getUsername());
        System.out.println("1. Создать короткую ссылку");
        System.out.println("2. Перейти по ссылке");
        System.out.println("3. Просмотреть статус ссылки");
        System.out.println("4. Изменить лимит кликов");
        System.out.println("5. Изменить время жизни ссылки");
        System.out.println("6. Удалить ссылку");
        System.out.println("7. Выйти из аккаунта (exit)");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> createShortUrl();
            case "2" -> openUrl();
            case "3" -> showUrlStatus();
            case "4" -> updateClicksLimit();
            case "5" -> updateLifetime();
            case "6" -> deleteUrl();
            case "7", "exit" -> {
                currentUser = null;
                System.out.println("Выход из аккаунта выполнен");
            }
            default -> System.out.println("Что-то пошло не так! Повторите команду.");
        }

        return true;
    }

    private void login() {
        System.out.println("Введите имя пользователя или UUID для входа.");
        String input = scanner.nextLine().trim();

        // Сначала пробуем найти по UUID
        try {
            UUID uuid = UUID.fromString(input);
            Optional<User> userByUuid = userService.findByUuid(uuid);
            if (userByUuid.isPresent()) {
                currentUser = userByUuid.get();
                System.out.println("Добро пожаловать, " + currentUser.getUsername() + "!");
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // Если ввод не является UUID, продолжаем поиск по имени
        }

        // Пробуем найти по имени пользователя
        Optional<User> userByName = userService.findByUsername(input);
        if (userByName.isPresent()) {
            currentUser = userByName.get();
            System.out.println("Добро пожаловать, " + currentUser.getUsername() + "!");
            System.out.println("Напомню, Ваш UUID: " + currentUser.getUUID());
        } else {
            // Если пользователь не найден, предлагаем зарегистрироваться
            System.out.println("Пользователь не найден. Хотите зарегистрироваться с именем '" + input + "'? (да/нет)");
            if (scanner.nextLine().trim().equalsIgnoreCase("да")) {
                register(input);
            }
        }
    }

    private void register(String username) {
        if (username.isEmpty()) {
            System.out.println("Введите уникальное имя пользователя для регистрации.");
            System.out.println("Если просто нажать Enter, Вам будет сгенерировано имя.");
            username = scanner.nextLine().trim();
        }

        if (username.isEmpty()) {
            username = "User-" + String.valueOf(this.hashCode()).substring(0, 7);
        } else {
            String name = username;
            while (true) {
                System.out.println("-- " + name + " --");
                if (userService.findByUsername(name).isPresent()) {
                    System.out.println("Имя пользователя уже занято. Введите другое имя.");
                    name = scanner.nextLine().trim();
                } else {
                    username = name;
                    break;
                }
                if (name.isEmpty()) break;
            }
        }

        try {
            currentUser = userService.createUser(username);
            System.out.println("Добрый день, " + username + ". Регистрация успешна!");
            System.out.println("Ваш UUID: " + currentUser.getUUID());
            System.out.println("Сохраните его для последующего входа в систему.");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка регистрации: " + e.getMessage());
        }
    }

    private void createShortUrl() {
        System.out.println("Введите оригинальную ссылку:");
        String originalUrl = scanner.nextLine().trim();

        // Запрашиваем опциональные параметры
        Integer lifetime = askForLifetime();
        Integer clicksLimit = askForClicksLimit();

        try {
            ShortUrl shortUrl = urlService.createShortUrl(originalUrl, currentUser.getUUID(), lifetime, clicksLimit);
            System.out.println("Создана короткая ссылка:");
            System.out.println(urlService.getFullShortUrl(shortUrl));
        } catch (IllegalArgumentException | URISyntaxException e) {
            System.out.println("Ошибка создания ссылки: " + e.getMessage());
        }
    }

    private Integer askForLifetime() {
        System.out.println("Хотите установить своё время жизни ссылки? (да/**нет**)");
        System.out.println("По умолчанию (часы): " + config.getDefaultLifetimeHours());

        if (scanner.nextLine().trim().equalsIgnoreCase("да")) {
            System.out.println("Введите время жизни в часах:");
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Неверный формат. Будет использовано значение по умолчанию.");
            }
        }
        return null;
    }

    private Integer askForClicksLimit() {
        System.out.println("Хотите установить свой лимит кликов? (да/**нет**)");
        System.out.println("По умолчанию (клики): " + config.getDefaultClicksLimit());

        if (scanner.nextLine().trim().equalsIgnoreCase("да")) {
            System.out.println("Введите лимит кликов:");
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Неверный формат. Будет использовано значение по умолчанию.");
            }
        }
        return null;
    }

    private void openUrl() {
        System.out.println("Введите короткий путь ссылки:");
        String shortPath = scanner.nextLine().trim();

        Optional<String> originalUrl = urlService.getOriginalUrl(shortPath);
        if (originalUrl.isPresent()) {
            try {
                Desktop.getDesktop().browse(new URI(originalUrl.get()));
                System.out.println("Ссылка открыта в браузере");
            } catch (IOException | URISyntaxException e) {
                System.out.println("Ошибка открытия ссылки: " + e.getMessage());
            }
        } else {
            System.out.println("Ссылка недоступна или не существует");
        }
    }

    private void showUrlStatus() {
        System.out.println("Введите короткий путь ссылки:");
        String shortPath = scanner.nextLine().trim();
        System.out.println(urlService.getShortUrl(shortPath));
    }

    private void updateClicksLimit() {
        System.out.println("Введите короткий путь ссылки:");
        String shortPath = scanner.nextLine().trim();

//         Проверяем существование ссылки и права доступа
        Optional<ShortUrl> urlOpt = urlService.findByPath(shortPath);
        if (urlOpt.isEmpty()) {
            System.out.println("Ссылка не найдена");
            return;
        }

        ShortUrl url = urlOpt.get();
        if (!url.getUserUuid().equals(currentUser.getUUID())) {
            System.out.println("У вас нет прав на изменение этой ссылки");
            return;
        }

        // Показываем текущие параметры
        System.out.println("Текущий статус ссылки:");
        System.out.println(urlService.getShortUrl(shortPath));

        System.out.println("\nВведите новый лимит кликов:");
        try {
            int newLimit = Integer.parseInt(scanner.nextLine().trim());
            urlService.updateClicksLimit(shortPath, currentUser.getUUID(), newLimit);
            System.out.println("Лимит кликов обновлен. Новый статус ссылки:");
            System.out.println(urlService.getShortUrl(shortPath));
        } catch (NumberFormatException e) {
            System.out.println("Неверный формат числа");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка обновления: " + e.getMessage());
        }
    }

    private void updateLifetime() {
        System.out.println("Введите короткий путь ссылки:");
        String shortPath = scanner.nextLine().trim();

        // Проверяем существование ссылки и права доступа
        Optional<ShortUrl> urlOpt = urlService.findByPath(shortPath);
        if (urlOpt.isEmpty()) {
            System.out.println("Ссылка не найдена");
            return;
        }

        ShortUrl url = urlOpt.get();
        if (!url.getUserUuid().equals(currentUser.getUUID())) {
            System.out.println("У вас нет прав на изменение этой ссылки");
            return;
        }

        // Показываем текущие параметры
        System.out.println("Текущий статус ссылки:");
        System.out.println(urlService.getShortUrl(shortPath));

        System.out.println("\nВведите новое время жизни ссылки (в часах):");
        try {
            int newLifetime = Integer.parseInt(scanner.nextLine().trim());
//            urlShortenerService.updateLifetime(shortPath, currentUser.getUuid(), newLifetime);
            System.out.println("Время жизни обновлено. Новый статус ссылки:");
            System.out.println(urlService.getShortUrl(shortPath));
        } catch (NumberFormatException e) {
            System.out.println("Неверный формат числа");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка обновления: " + e.getMessage());
        }
    }

    private void deleteUrl() {
        System.out.println("Введите короткий путь ссылки:");
        String shortPath = scanner.nextLine().trim();

        try {
            urlService.deleteUrl(shortPath, currentUser.getUUID());
            System.out.println("Ссылка успешно удалена");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка удаления: " + e.getMessage());
        }
    }
}