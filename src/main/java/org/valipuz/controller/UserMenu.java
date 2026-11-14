package org.valipuz.controller;

import org.valipuz.entity.ShortLink;
import org.valipuz.exception.CustomException;
import org.valipuz.service.LinkService;
import org.valipuz.service.UserService;
import org.valipuz.utils.Messages;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Меню пользователя: вход по UUID, создание, изменение лимита, удаление и просмотр своих ссылок
 */
public class UserMenu {
    private final Scanner scanner;
    private final UserService userService;
    private final LinkService linkService;

    private UUID currentUser;

    public UserMenu(Scanner scanner, UserService userService, LinkService linkService) {
        this.scanner = scanner;
        this.userService = userService;
        this.linkService = linkService;
    }

    public boolean runUserMenu() {
        currentUser = null;
        chooseUser();
        while (true) {
            printUserMenu();
            String choice = readLine();
            if (choice == null) {
                return true;
            }

            try {
                switch (choice) {
                    case "1" -> handleCreate();
                    case "2" -> handleEdit();
                    case "3" -> handleDelete();
                    case "4" -> handleList();
                    case "5" -> chooseUser();   // смена пользователя, остаёмся в этом меню
                    case "6" -> {
                        return false;
                    }
                    case "0" -> {
                        return true;
                    }
                    default -> System.out.println("Введите цифру от 0 до 6");
                }
            } catch (CustomException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Метод для выбора и смены пользователя
     */
    private void chooseUser() {
        System.out.print("Введите ваш UUID или нажмите Enter, если вы новый пользователь: ");
        String input = readLine();
        if (input == null) {
            return;
        }
        if (input.isEmpty()) {
            currentUser = null;
            System.out.println("Вы новый пользователь! Вы получите свой ID после создания первой короткой ссылки!");
            return;
        }

        try {
            currentUser = userService.signIn(input);
            System.out.println("Вы вошли как " + currentUser);
        } catch (IllegalArgumentException e) {
            throw new CustomException(Messages.INVALID_USER_ID);
        }
    }

    /**
     * Обработка создания короткой ссылки
     */
    private void handleCreate() {
        System.out.print("Введите URL (пример: https://github.com): ");
        String inputUrl = readLine();
        System.out.print("Введите лимит переходов от 1 до " + linkService.getMaxLimit()
                + " или нажмите Enter для значения по умолчанию (" + linkService.getDefaultLimit() + "): ");
        String inputLimit = readLine();

        ShortLink link = linkService.create(inputUrl, inputLimit, currentUser);
        if (currentUser == null) {
            currentUser = link.getOwnerId();
            System.out.println("Вы зарегистрированы! Ваш ID: " + currentUser + "\n" + "Запомните его, чтобы управлять своими ссылками.");
        }
        System.out.println("Короткая ссылка: " + linkService.toShortUrl(link.getCode()));
    }

    /**
     * Изменение лимита кликов короткой ссылки
     */
    private void handleEdit() {
        if (currentUser == null) {
            System.out.println("Вы пока не зарегистрированы. Для регистрации создайте первую короткую ссылку через пункт 1 в Меню пользователя");
            return;
        }
        System.out.print("Введите короткую ссылку для изменения (пример: " + linkService.toShortUrl("aB3xK9") + "): ");
        String inputLink = readLine();
        System.out.print("Введите новый лимит переходов от 1 до " + linkService.getMaxLimit() + ": ");
        String inputLimit = readLine();

        ShortLink link = linkService.editLimit(inputLink, inputLimit, currentUser);
        System.out.println("Лимит изменён: " + link.getClicks() + "/" + link.getLimitClicks());
    }

    /**
     * Удаление короткой ссылки
     */
    private void handleDelete() {
        if (currentUser == null) {
            System.out.println("Вы пока не зарегистрированы. Для регистрации создайте первую короткую ссылку через пункт 1 в Меню пользователя");
            return;
        }
        System.out.print("Введите короткую ссылку для удаления (пример: " + linkService.toShortUrl("aB3xK9") + "): ");
        String inputLink = readLine();

        linkService.delete(inputLink, currentUser);
        System.out.println("Ссылка удалена");
    }

    /**
     * Показ живых по времени ссылок пользователя
     */
    private void handleList() {
        if (currentUser == null) {
            System.out.println("Вы пока не зарегистрированы. Для регистрации создайте первую короткую ссылку через пункт 1 в Меню пользователя");
            return;
        }
        List<ShortLink> links = linkService.findLinksByOwner(currentUser);
        if (links.isEmpty()) {
            System.out.println("У вас пока нет ссылок.");
            return;
        }
        for (ShortLink shortLink : links) {
            System.out.println(linkService.toShortUrl(shortLink.getCode())
                    + " - " + shortLink.getOriginalUrl()
                    + " | переходов использовано: " + shortLink.getClicks() + "/" + shortLink.getLimitClicks());
        }
    }

    private void printUserMenu() {
        System.out.print("""
            ====== Меню пользователя ======

            Выберите действие (введите цифру):

            1 - Создать короткую ссылку
            2 - Изменить лимит кликов у короткой ссылки
            3 - Удалить короткую ссылку
            4 - Просмотреть свои короткие ссылки
            5 - Сменить пользователя
            6 - В главное меню
            0 - Выйти из программы
            """);
    }

    private String readLine() {
        return scanner.hasNextLine() ? scanner.nextLine().trim() : null;
    }
}
