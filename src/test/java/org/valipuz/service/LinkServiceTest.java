package org.valipuz.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.valipuz.entity.ShortLink;
import org.valipuz.exception.CustomException;
import org.valipuz.repository.LinkRepository;
import org.valipuz.repository.UserRepository;
import org.valipuz.repository.impl.LinkRepositoryImpl;
import org.valipuz.repository.impl.UserRepositoryImpl;
import org.valipuz.utils.CodeGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkServiceTest {
    // момент «сейчас» для всех тестов; TTL ссылки 24 часа
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String URL = "https://github.com";

    @TempDir
    Path tempDir;   // JUnit создаёт новую пустую папку перед КАЖДЫМ тестом и удаляет её после

    private LinkRepository linkRepository;
    private UserService userService;
    private LinkService linkService;

    @BeforeEach
    void setUp() {
        UserRepository userRepository = new UserRepositoryImpl(tempDir.resolve("users.json").toString());
        linkRepository = new LinkRepositoryImpl(tempDir.resolve("links.json").toString());
        userService = new UserService(userRepository);
        linkService = serviceAt(NOW, new CodeGenerator(6));
    }

    /**
     * Сервис с теми же репозиториями, но со своими часами и генератором кодов.
     * Нужен, чтобы "перемотать время" вперёд или подставить предсказуемые коды
     */
    private LinkService serviceAt(Instant time, CodeGenerator codeGenerator) {
        return new LinkService(userService, linkRepository, codeGenerator,
                Clock.fixed(time, ZoneOffset.UTC), 24, 10, 1000, "clck.ru");
    }

    private String shortUrl(ShortLink link) {
        return linkService.toShortUrl(link.getCode());
    }

    @Test
    @DisplayName("При создании короткой ссылки с лимитом по умолчанию должно сохраниться 10")
    void createLinkWithDefaultLimit() {
        String url = "https://github.com";

        // Пользователь новый, лимит не указан (нажал Enter)
        ShortLink link = linkService.create(url, "", null);

        assertEquals(10, link.getLimitClicks());
        assertEquals(0, link.getClicks());
        assertEquals(url, link.getOriginalUrl());
        assertNotNull(link.getOwnerId());
    }

    @Test
    @DisplayName("При создании короткой ссылки должен сохраниться введённый лимит (5)")
    void createLinkWithUserLimit() {
        String url = "https://github.com";

        // Пользователь новый, лимит 5
        ShortLink link = linkService.create(url, "5", null);

        assertEquals(5, link.getLimitClicks());
        assertEquals(0, link.getClicks());
        assertEquals(url, link.getOriginalUrl());
        assertNotNull(link.getOwnerId());
    }

    @Test
    @DisplayName("При создании короткой ссылки получаем ошибку, если передаём URL без http/https")
    void createWithInvalidOriginalUrl() {
        String url = "github.com";

        // Пользователь новый, лимит 5
        CustomException exception = assertThrows(CustomException.class,
                () -> linkService.create(url, "5", null));

        assertTrue(exception.getMessage().startsWith("Некорректный URL"));
        // проверка URL идёт до регистрации, поэтому ни пользователь, ни ссылка не должны сохраниться
        assertFalse(Files.exists(tempDir.resolve("users.json")));
        assertFalse(Files.exists(tempDir.resolve("links.json")));
    }

    @Test
    @DisplayName("При создании короткой ссылки получаем ошибку, если URL не введён")
    void createWithEmptyOriginalUrl() {
        CustomException exception = assertThrows(CustomException.class,
                () -> linkService.create("", "5", null));

        assertEquals("Вы не ввели ссылку!", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "1001", "abc", "1.5"})
    @DisplayName("При создании короткой ссылки получаем ошибку, если лимит вне диапазона 1..1000 или не число")
    void createWithInvalidLimit(String limit) {
        CustomException exception = assertThrows(CustomException.class,
                () -> linkService.create(URL, limit, null));

        assertTrue(exception.getMessage().startsWith("Лимит должен быть"));
        // лимит проверяется до регистрации, поэтому пользователь не сохраняется
        assertFalse(Files.exists(tempDir.resolve("users.json")));
    }

    @Test
    @DisplayName("Новый пользователь регистрируется при создании первой ссылки и становится её владельцем")
    void createRegistersNewUser() {
        ShortLink link = linkService.create(URL, "", null);

        assertTrue(userService.exists(link.getOwnerId()));
    }

    @Test
    @DisplayName("Один и тот же URL, сокращённый дважды одним пользователем, даёт разные короткие ссылки")
    void sameUrlTwiceGivesDifferentCodes() {
        ShortLink first = linkService.create(URL, "", null);
        ShortLink second = linkService.create(URL, "", first.getOwnerId());

        assertNotEquals(first.getCode(), second.getCode());
    }

    @Test
    @DisplayName("Один и тот же URL у разных пользователей даёт разные короткие ссылки")
    void sameUrlForDifferentUsersGivesDifferentCodes() {
        ShortLink first = linkService.create(URL, "", null);
        ShortLink second = linkService.create(URL, "", null);

        assertNotEquals(first.getOwnerId(), second.getOwnerId());
        assertNotEquals(first.getCode(), second.getCode());
    }

    @Test
    @DisplayName("Переход по короткой ссылке возвращает исходный URL и увеличивает счётчик переходов")
    void resolveReturnsOriginalUrlAndCountsClick() {
        ShortLink link = linkService.create(URL, "5", null);

        String originalUrl = linkService.shortLinkToOriginalUrl(shortUrl(link));

        assertEquals(URL, originalUrl);
        assertEquals(1, linkRepository.findLink(link.getCode()).orElseThrow().getClicks());
    }

    @Test
    @DisplayName("После исчерпания лимита переход по ссылке блокируется")
    void resolveBlockedWhenLimitReached() {
        ShortLink link = linkService.create(URL, "2", null);
        linkService.shortLinkToOriginalUrl(shortUrl(link));
        linkService.shortLinkToOriginalUrl(shortUrl(link));

        CustomException exception = assertThrows(CustomException.class,
                () -> linkService.shortLinkToOriginalUrl(shortUrl(link)));

        assertTrue(exception.getMessage().startsWith("Лимит переходов по ссылке исчерпан"));
        // заблокированный переход не засчитывается
        assertEquals(2, linkRepository.findLink(link.getCode()).orElseThrow().getClicks());
    }

    @Test
    @DisplayName("Переход по ссылке с истёкшим сроком жизни сообщает об истечении срока")
    void resolveExpiredLink() {
        ShortLink link = linkService.create(URL, "", null);
        LinkService later = serviceAt(NOW.plus(Duration.ofHours(25)), new CodeGenerator(6));

        CustomException exception = assertThrows(CustomException.class,
                () -> later.shortLinkToOriginalUrl(shortUrl(link)));

        assertTrue(exception.getMessage().startsWith("Срок действия ссылки истёк"));
    }

    @Test
    @DisplayName("Очистка удаляет только ссылки с истёкшим сроком жизни")
    void removeExpiredDeletesOnlyExpiredLinks() {
        // истекает через 24 часа
        ShortLink old = linkService.create(URL, "", null);
        // создана на 20 часов позже, истекает через 44 часа от NOW
        ShortLink fresh = serviceAt(NOW.plus(Duration.ofHours(20)), new CodeGenerator(6))
                .create(URL, "", old.getOwnerId());

        int removed = serviceAt(NOW.plus(Duration.ofHours(25)), new CodeGenerator(6)).removeExpired();

        assertEquals(1, removed);
        assertTrue(linkRepository.findLink(old.getCode()).isEmpty());
        assertTrue(linkRepository.findLink(fresh.getCode()).isPresent());
    }

    @Test
    @DisplayName("Владелец меняет лимит, а счётчик переходов при этом не сбрасывается")
    void ownerEditsLimitAndClicksAreKept() {
        ShortLink link = linkService.create(URL, "5", null);
        linkService.shortLinkToOriginalUrl(shortUrl(link));
        linkService.shortLinkToOriginalUrl(shortUrl(link));

        ShortLink edited = linkService.editLimit(shortUrl(link), "10", link.getOwnerId());

        assertEquals(10, edited.getLimitClicks());
        assertEquals(2, edited.getClicks());
    }

    @Test
    @DisplayName("Другой пользователь не может изменить лимит ссылки")
    void otherUserCannotEditLimit() {
        ShortLink link = linkService.create(URL, "5", null);
        UUID otherUser = userService.register();

        CustomException exception = assertThrows(CustomException.class,
                () -> linkService.editLimit(shortUrl(link), "100", otherUser));

        assertEquals("Управлять ссылкой может только её владелец!", exception.getMessage());
        assertEquals(5, linkRepository.findLink(link.getCode()).orElseThrow().getLimitClicks());
    }

    @Test
    @DisplayName("Другой пользователь не может удалить ссылку")
    void otherUserCannotDelete() {
        ShortLink link = linkService.create(URL, "", null);
        UUID otherUser = userService.register();

        CustomException exception = assertThrows(CustomException.class,
                () -> linkService.delete(shortUrl(link), otherUser));

        assertEquals("Управлять ссылкой может только её владелец!", exception.getMessage());
        assertTrue(linkRepository.findLink(link.getCode()).isPresent());
    }

    @Test
    @DisplayName("После удаления владельцем переход по ссылке сообщает, что она не найдена")
    void ownerDeletesLink() {
        ShortLink link = linkService.create(URL, "", null);

        linkService.delete(shortUrl(link), link.getOwnerId());

        CustomException exception = assertThrows(CustomException.class,
                () -> linkService.shortLinkToOriginalUrl(shortUrl(link)));
        assertTrue(exception.getMessage().startsWith("Короткая ссылка не найдена"));
    }

    @Test
    @DisplayName("Если сгенерированный код уже занят, генерируется новый")
    void occupiedCodeIsRegenerated() {
        // генератор, который выдаёт коды по списку: второй вызов снова вернёт занятый AAAAAA
        CodeGenerator predictable = new CodeGenerator(6) {
            private final Iterator<String> codes = List.of("AAAAAA", "AAAAAA", "BBBBBB").iterator();

            @Override
            public String generate() {
                return codes.next();
            }
        };
        LinkService service = serviceAt(NOW, predictable);

        ShortLink first = service.create(URL, "", null);
        ShortLink second = service.create(URL, "", first.getOwnerId());

        assertEquals("AAAAAA", first.getCode());
        assertEquals("BBBBBB", second.getCode());
    }
}
