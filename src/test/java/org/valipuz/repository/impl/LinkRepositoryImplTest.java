package org.valipuz.repository.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.valipuz.entity.ShortLink;
import org.valipuz.repository.LinkRepository;
import org.valipuz.service.LinkService;
import org.valipuz.service.UserService;
import org.valipuz.utils.CodeGenerator;

import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты: сервис, репозитории, JsonFileHandler и Gson работают вместе с JSON-файлами
 */
class LinkRepositoryImplTest {
    @TempDir
    Path tempDir;

    private LinkRepository newLinkRepository() {
        return new LinkRepositoryImpl(tempDir.resolve("links.json").toString());
    }

    @Test
    @DisplayName("Ссылка и счётчик переходов сохраняются в файл и читаются после перезапуска")
    void linkAndClicksSurviveRestart() {
        LinkRepository linkRepository = newLinkRepository();
        UserService userService = new UserService(new UserRepositoryImpl(tempDir.resolve("users.json").toString()));
        LinkService linkService = new LinkService(userService, linkRepository, new CodeGenerator(6),
                Clock.systemUTC(), 24, 10, 1000, "clck.ru");
        ShortLink link = linkService.create("https://github.com", "5", null);
        linkService.shortLinkToOriginalUrl(linkService.toShortUrl(link.getCode()));
        linkService.shortLinkToOriginalUrl(linkService.toShortUrl(link.getCode()));

        // новый репозиторий ничего не знает о прошлом и читает только файл, как программа после перезапуска
        ShortLink reloaded = newLinkRepository().findLink(link.getCode()).orElseThrow();

        assertEquals("https://github.com", reloaded.getOriginalUrl());
        assertEquals(link.getOwnerId(), reloaded.getOwnerId());
        assertEquals(link.getExpiresAtMillis(), reloaded.getExpiresAtMillis());
        assertEquals(5, reloaded.getLimitClicks());
        assertEquals(2, reloaded.getClicks());
    }

    @Test
    @DisplayName("Удалённая ссылка не появляется после перезапуска")
    void deletedLinkIsGoneAfterRestart() {
        LinkRepository linkRepository = newLinkRepository();
        ShortLink kept = new ShortLink("AAAAAA", "https://github.com", UUID.randomUUID(), Long.MAX_VALUE, 10);
        ShortLink deleted = new ShortLink("BBBBBB", "https://example.com", kept.getOwnerId(), Long.MAX_VALUE, 10);
        linkRepository.saveLink(kept);
        linkRepository.saveLink(deleted);

        linkRepository.deleteLink("BBBBBB");

        LinkRepository reloaded = newLinkRepository();
        assertTrue(reloaded.findLink("AAAAAA").isPresent());
        assertTrue(reloaded.findLink("BBBBBB").isEmpty());
    }
}
