package dev.urls.ui;

import dev.urls.config.AppConfig;
import dev.urls.model.ShortUrl;
import dev.urls.model.User;
import dev.urls.service.UserService;
import dev.urls.service.impl.UrlServiceImpl;

import java.util.List;
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
        System.out.println("2. Показать список моих ссылок");
        System.out.println("3. Перейти по ссылке");
        System.out.println("4. Просмотреть статус ссылки");
        System.out.println("5. Изменить лимит кликов");
        System.out.println("6. Изменить время жизни ссылки");
        System.out.println("7. Удалить ссылку");
        System.out.println("8. Выйти из аккаунта (exit)");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> createShortUrl();
            case "2" -> listAllMyUrls();
            case "3" -> openUrl();
            case "4" -> showUrlStatus();
            case "5" -> updateClicksLimit();
            case "6" -> updateLifetime();
            case "7" -> deleteUrl();
            case "8", "exit" -> {
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
            System.out.println("Пользователь не найден. Хотите зарегистрироваться с именем '" + input + "'? (да/*нет*)");
            if (scanner.nextLine().trim().equalsIgnoreCase("да")) {
                register(input);
            } else register("");
        }
    }

    private void register(String username) {
        if (username.isEmpty()) {
            System.out.println("Введите уникальное имя пользователя для регистрации.");
            System.out.println("Если просто нажать Enter, Вам будет сгенерировано имя.");
            username = scanner.nextLine().trim();
        }

        if (username.isEmpty()) {
            do {
                username = "User-" + String.valueOf(this.hashCode()).substring(0, 7);
            } while (userService.findByUsername(username).isPresent());
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

        // Запрашиваем опциональные параметры ссылки
        Integer lifetime = askForSetLifetime();
        lifetime = lifetime > 0 ? lifetime : config.getDefaultLifetimeMin();
        Integer clicksLimit = askForSetClicksLimit();

        try {
            ShortUrl shortUrl = urlService.createShortUrl(originalUrl, currentUser.getUUID(), lifetime, clicksLimit);
            System.out.println("Создана короткая ссылка:");
            System.out.println(urlService.getFullShortUrl(shortUrl.getShortPath()));
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка создания ссылки: " + e.getMessage());
        }
    }

    private Integer askForSetLifetime() {
        System.out.println("Хотите установить своё время жизни ссылки?\n(введите нужное значение или Enter для значения по умолчанию)");
        int defaultValue = config.getDefaultLifetimeHours();
        System.out.println("Значение по умолчанию (часы): " + defaultValue);

        System.out.println("Введите время жизни в часах:");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Неверный формат. Будет использовано значение по умолчанию.");
            return defaultValue;
        }
    }

    private Integer askForSetClicksLimit() {
        System.out.println("Хотите установить свой лимит кликов?\n(введите нужное значение или Enter для значения по умолчанию)");
        int defaultValue = config.getDefaultClicksLimit();
        System.out.println("Значение по умолчанию (клики): " + defaultValue);

        System.out.println("Введите лимит кликов:");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Неверный формат. Будет использовано значение по умолчанию.");
            return defaultValue;
        }
    }

    private void listAllMyUrls() {
        List<ShortUrl> urls = urlService.findAllUrlsByUuid(currentUser.getUUID());
        if (urls.isEmpty()) {
            System.out.println("У вас нет ссылок или все они уже были удалены.");
            return;
        }
        System.out.println("Ваши ссылки:");
        for (int i = 0; i < urls.size(); i++) {
            ShortUrl url = urls.get(i);
            System.out.printf("%d. %s%n", i + 1, urlService.getShortUrlStatus(url));
        }
    }

    private void openUrl() {
        System.out.println("Введите короткий путь ссылки:");
        String shortPath = scanner.nextLine().trim();

        if (isOperationAvailable(shortPath)) {
            urlService.clickShortUrl(shortPath);
        }
    }

    private boolean isOperationAvailable(String shortPath) {
        if (isShortUrlExists(shortPath)) {
            if (urlService.getShortUrlOwner(shortPath).equals(currentUser.getUUID())) {
                return true;
            } else {
                System.out.println("У Вас нет прав на манипуляции с этой ссылкой");
                return false;
            }
        }
        return false;
    }

    private boolean isShortUrlExists(String shortPath) {
        if (urlService.findByPath(shortPath).isEmpty()) {
            System.out.println("Ссылка не найдена");
            return false;
        }
        return true;
    }

    private void showUrlStatus() {
        System.out.println("Введите короткий путь ссылки:");
        String shortPath = scanner.nextLine().trim();
        String status = getShortUrlStatus(shortPath);
        if (status != null) System.out.println(status);
    }

    private String getShortUrlStatus(String shortPath) {
        if (isOperationAvailable(shortPath))
            return urlService.getShortUrlStatus(urlService.findByPath(shortPath).get());
        else return null;
    }

    private void showUrlCurrentStatus(String shortPath) {
        System.out.println("Текущий статус ссылки:");
        System.out.println(getShortUrlStatus(shortPath));
    }

    private void updateClicksLimit() {
        System.out.println("Введите короткий путь ссылки:");
        String shortPath = scanner.nextLine().trim();

        if (!isOperationAvailable(shortPath)) return;

        showUrlCurrentStatus(shortPath);

        System.out.println("\nВведите новый лимит кликов:");
        try {
            int newLimit = Integer.parseInt(scanner.nextLine().trim());
            newLimit = newLimit > 0? newLimit : config.getDefaultClicksLimit();
            urlService.updateUrlClicksLimit(shortPath, newLimit);
            System.out.println("Лимит кликов обновлен");
            showUrlCurrentStatus(shortPath);
        } catch (NumberFormatException e) {
            System.out.println("Неверный формат числа");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка обновления: " + e.getMessage());
        }
    }

    private void updateLifetime() {
        System.out.println("Введите короткий путь ссылки:");
        String shortPath = scanner.nextLine().trim();

        if (!isOperationAvailable(shortPath)) return;

        showUrlCurrentStatus(shortPath);

        System.out.println("\nВведите новое время жизни ссылки (в часах от текущего времени):");
        try {
            int newLifetime = Integer.parseInt(scanner.nextLine().trim());
            urlService.updateUrlLifeTime(shortPath, (newLifetime > 0) ? newLifetime : config.getDefaultLifetimeMin());
            System.out.println("Время жизни обновлено");
            showUrlCurrentStatus(shortPath);
        } catch (NumberFormatException e) {
            System.out.println("Неверный формат числа");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка обновления: " + e.getMessage());
        }
    }

    private void deleteUrl() {
        System.out.println("Введите короткий путь ссылки:");
        String shortPath = scanner.nextLine().trim();

        if (!isOperationAvailable(shortPath)) return;

        try {
            urlService.deleteShortUrl(shortPath);
            System.out.println("Ссылка успешно удалена");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка удаления: " + e.getMessage());
        }
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
}