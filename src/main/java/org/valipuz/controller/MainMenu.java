package org.valipuz.controller;

import org.valipuz.exception.CustomException;
import org.valipuz.service.LinkService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

/**
 * Главное меню
 */
public class MainMenu {

    private final Scanner scanner;

    private final UserMenu userMenu;
    private final LinkService linkService;

    public MainMenu(Scanner scanner, UserMenu userMenu, LinkService linkService) {
        this.scanner = scanner;
        this.userMenu = userMenu;
        this.linkService = linkService;
    }

    public void runMainMenu() {
        System.out.println("Добро пожаловать в сервис сокращения ссылок!");
        while (true) {
            printWelcome();
            String choice = readLine();
            if (choice == null) {
                return;
            }
            try {
                switch (choice) {
                    case "1" -> {
                        if (userMenu.runUserMenu()) {
                            return;
                        }
                    }
                    case "2" -> handleOpen();
                    case "0" -> {
                        return;
                    }
                    default -> System.out.println("Введите 1, 2 или 0");
                }
            } catch (CustomException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Переход по короткой ссылке и открытие оригинального URL в браузере
     */
    private void handleOpen() {
        System.out.print("Введите короткую ссылку (пример: " + linkService.toShortUrl("aB3xK9") + "): ");
        String inputShortLink = readLine();

        String originalUrl = linkService.shortLinkToOriginalUrl(inputShortLink);
        openInBrowser(originalUrl);
    }

    /**
     * Открытие оригинального URL в браузере
     *
     * @param originalUrl оригинальный URL
     */
    private void openInBrowser(String originalUrl) {
        // нет графической среды или ОС не умеет открывать ссылки (сервер, CI)
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            System.out.println("Не удалось открыть браузер. Перейдите по ссылке вручную " + originalUrl);
            return;
        }
        try {
            // открываем URL в браузере по умолчанию
            // macOS не открывает адреса с не-ASCII символами (кириллица), поэтому кодируем их в %XX
            Desktop.getDesktop().browse(new URI(new URI(originalUrl).toASCIIString()));
        } catch (IOException | URISyntaxException e) {
            // браузер не запустился или URL не удалось разобрать
            System.out.println("Не удалось открыть браузер. Перейдите по ссылке вручную " + originalUrl);
        }
    }

    private void printWelcome() {
        System.out.print("""
                ====== Главное меню ======

                Выберите действие (введите цифру):

                1 - Создать/изменить/удалить короткую ссылку, посмотреть свои короткие ссылки
                2 - Ввести короткую ссылку
                0 - Выйти
                """);
    }


    private String readLine() {
        return scanner.hasNextLine() ? scanner.nextLine().trim() : null;
    }
}
