package org.valipuz.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.valipuz.entity.ShortLink;
import org.valipuz.repository.LinkRepository;
import org.valipuz.repository.impl.LinkRepositoryImpl;
import org.valipuz.repository.impl.UserRepositoryImpl;
import org.valipuz.service.LinkService;
import org.valipuz.service.UserService;
import org.valipuz.utils.CodeGenerator;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты: пользовательский сценарий проходит через меню, сервисы и репозитории.
 * Ввод подаётся строкой через Scanner, вывод перехватывается из System.out
 */
class MainMenuTest {
    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    private LinkRepository linkRepository;
    private LinkService linkService;
    private UserService userService;

    @BeforeAll
    static void disableBrowser() {
        // при запуске из IDE браузер не должен открываться: без графики Desktop недоступен
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void setUp() {
        linkRepository = new LinkRepositoryImpl(tempDir.resolve("links.json").toString());
        userService = new UserService(new UserRepositoryImpl(tempDir.resolve("users.json").toString()));
        linkService = new LinkService(userService, linkRepository, new CodeGenerator(6),
                Clock.systemUTC(), 24, 10, 1000, "clck.ru");
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOut() {
        System.setOut(originalOut);
    }

    /**
     * Запускает программу с заданным вводом: каждая строка массива — одна строка, введённая пользователем
     */
    private String run(String... lines) {
        output.reset();
        Scanner scanner = new Scanner(String.join("\n", lines) + "\n");
        UserMenu userMenu = new UserMenu(scanner, userService, linkService);
        new MainMenu(scanner, userMenu, linkService).runMainMenu();
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Новый пользователь создаёт ссылку через меню и видит её в списке своих ссылок")
    void newUserCreatesLinkAndSeesItInList() {
        // 1 - меню пользователя, Enter - новый пользователь, 1 - создать, URL, Enter - лимит по умолчанию,
        // 4 - список ссылок, 0 - выход
        String out = run("1", "", "1", "https://github.com", "", "4", "0");

        List<ShortLink> links = linkRepository.findAllLinks();
        assertEquals(1, links.size());
        String shortUrl = linkService.toShortUrl(links.get(0).getCode());
        assertTrue(out.contains("Вы зарегистрированы! Ваш ID: " + links.get(0).getOwnerId()));
        assertTrue(out.contains("Короткая ссылка: " + shortUrl));
        assertTrue(out.contains(shortUrl + " - https://github.com | переходов использовано: 0/10"));
    }

    @Test
    @DisplayName("Переход по короткой ссылке через главное меню засчитывается, а после лимита блокируется")
    void openLinkThroughMainMenuUntilLimit() {
        ShortLink link = linkService.create("https://github.com", "1", null);
        String shortUrl = linkService.toShortUrl(link.getCode());

        // 2 - ввести короткую ссылку (дважды), 0 - выход
        String out = run("2", shortUrl, "2", shortUrl, "0");

        assertTrue(out.contains("https://github.com"));
        assertTrue(out.contains("Лимит переходов по ссылке исчерпан"));
        assertEquals(1, linkRepository.findLink(link.getCode()).orElseThrow().getClicks());
    }

    @Test
    @DisplayName("Неверный ввод в меню не прерывает работу программы")
    void invalidMenuInputShowsHint() {
        String out = run("5", "abc", "0");

        assertTrue(out.contains("Введите 1, 2 или 0"));
    }
}
