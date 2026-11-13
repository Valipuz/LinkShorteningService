package org.valipuz.service;

import org.valipuz.entity.ShortLink;
import org.valipuz.exception.CustomException;
import org.valipuz.repository.LinkRepository;
import org.valipuz.utils.CodeGenerator;
import org.valipuz.utils.Messages;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class LinkService {
    private final UserService userService;

    private final LinkRepository linkRepository;

    private final CodeGenerator codeGenerator;
    private final Clock clock;
    private final int ttlHours;
    private final int defaultLimit;
    private final int maxLimit;
    private final String domain;

    public LinkService(UserService userService, LinkRepository linkRepository, CodeGenerator codeGenerator, Clock clock,
            int ttlHours, int defaultLimit, int maxLimit, String domain) {
        this.userService = userService;
        this.linkRepository = linkRepository;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
        this.ttlHours = ttlHours;
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
        this.domain = domain;
    }

    /**
     * Создание ссылки и использование userService для регистрации пользователя
     *
     * @param url        оригинальный url
     * @param inputLimit лимит кликов ссылки от пользователя
     * @param ownerId    пользователь
     * @return созданная сущность ссылки
     */
    public synchronized ShortLink create(String url, String inputLimit, UUID ownerId) {
        validateUrl(url);
        int clicksLimit = parseLimit(inputLimit);
        if (ownerId == null) {
            ownerId = userService.register();
        }

        String code;
        do {
            code = codeGenerator.generate();
        } while (linkRepository.findLink(code).isPresent());

        long expiresAt = clock.millis() + Duration.ofHours(ttlHours).toMillis();
        ShortLink shortLink = new ShortLink(code, url, ownerId, expiresAt, clicksLimit);
        linkRepository.saveLink(shortLink);
        return shortLink;
    }

    /**
     * Изменение лимита кликов у короткой ссылки
     *
     * @param inputLink   короткая ссылка
     * @param inputLimit  новый лимит
     * @param currentUser пользователь, который сделал запрос на изменение
     * @return короткая ссылка с изменённым лимитом кликов
     */
    public synchronized ShortLink editLimit(String inputLink, String inputLimit, UUID currentUser) {
        ShortLink shortLink = findLinkAndCheckOwner(inputLink, currentUser);
        if (inputLimit == null || inputLimit.isEmpty()) {
            throw new CustomException(Messages.EMPTY_NEW_LIMIT);
        }
        shortLink.setLimitClicks(parseLimit(inputLimit));
        linkRepository.saveLink(shortLink);
        return shortLink;
    }

    public synchronized void delete(String inputLink, UUID currentUser) {
        ShortLink shortLink = findLinkAndCheckOwner(inputLink, currentUser);
        linkRepository.deleteLink(shortLink.getCode());
    }

    /**
     * Получение всех живых ссылок пользователя
     *
     * @param ownerId пользователь
     * @return список ссылок
     */
    public synchronized List<ShortLink> findLinksByOwner(UUID ownerId) {
        long now = clock.millis();
        return linkRepository.findLinksByOwner(ownerId).stream()
                .filter(link -> !link.isExpired(now))
                .toList();
    }

    /**
     * Переход по короткой ссылке: проверка TTL и лимита, подсчёт перехода
     *
     * @param inputShortLink короткая ссылка
     * @return оригинальный URL
     */
    public synchronized String shortLinkToOriginalUrl(String inputShortLink) {
        String code = extractCode(inputShortLink);
        ShortLink shortLink = linkRepository.findLink(code)
                .orElseThrow(() -> new CustomException(
                        Messages.LINK_NOT_FOUND_ON_OPEN + Messages.CREATE_NEW_HINT));
        if (shortLink.isExpired(clock.millis())) {
            throw new CustomException(Messages.LINK_EXPIRED);
        }
        if (shortLink.isClickLimitsReached()) {
            throw new CustomException(Messages.LIMIT_REACHED);
        }
        shortLink.registerClick();
        linkRepository.saveLink(shortLink);
        return shortLink.getOriginalUrl();
    }

    /**
     * Метод для удаления коротких ссылок с истёкшим сроком жизни
     *
     * @return количество удалённых ссылок
     */
    public synchronized int removeExpired() {
        long now = clock.millis();
        List<String> expired = linkRepository.findAllLinks().stream()
                .filter(link -> link.isExpired(now))
                .map(ShortLink::getCode)
                .toList();
        if (!expired.isEmpty()) {
            linkRepository.deleteLinks(expired);
        }
        return expired.size();
    }

    /**
     * Преобразование в короткую ссылку из кода
     *
     * @param code сгенерированный код для короткой ссылки
     * @return короткая ссылка
     */
    public String toShortUrl(String code) {
        return domain + "/" + code;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    /**
     * Валидация входящего оригинального URL
     *
     * @param url оригинальный URL
     */
    private void validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new CustomException(Messages.EMPTY_URL);
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new CustomException(Messages.INVALID_URL);
            }
        } catch (URISyntaxException e) {
            throw new CustomException(Messages.INVALID_URL);
        }
    }

    /**
     * Получение входящего лимита кликов
     *
     * @param inputLimit лимит кликов от пользователя
     * @return лимит
     */
    private int parseLimit(String inputLimit) {
        if (inputLimit == null) {
            throw new CustomException(Messages.LIMIT_NOT_PASSED);
        }
        if (inputLimit.isEmpty()) {
            return defaultLimit;
        }
        int limit;
        try {
            limit = Integer.parseInt(inputLimit);
        } catch (NumberFormatException e) {
            throw new CustomException(Messages.LIMIT_NOT_NUMBER);
        }
        if (limit < 1 || limit > maxLimit) {
            throw new CustomException(Messages.limitOutOfRange(maxLimit));
        }
        return limit;
    }

    /**
     * Ищем короткую ссылку у пользователя и проверяем может ли он её менять
     *
     * @param inputLink   короткая ссылка
     * @param currentUser пользователь, который сделал запрос на изменение
     * @return найденная ссылка
     */
    private ShortLink findLinkAndCheckOwner(String inputLink, UUID currentUser) {
        String code = extractCode(inputLink);
        long now = clock.millis();
        ShortLink link = linkRepository.findLink(code)
                .filter(l -> !l.isExpired(now))
                .orElseThrow(() -> new CustomException(Messages.LINK_NOT_FOUND));
        if (!link.getOwnerId().equals(currentUser)) {
            throw new CustomException(Messages.NOT_OWNER);
        }
        return link;
    }

    /**
     * Получение кода из короткой ссылки
     *
     * @param inputLink короткая ссылка
     * @return код
     */
    private String extractCode(String inputLink) {
        if (inputLink == null || inputLink.isEmpty()) {
            throw new CustomException(Messages.EMPTY_SHORT_LINK);
        }
        String prefix = domain + "/";
        if (!inputLink.startsWith(prefix) || inputLink.length() == prefix.length()) {
            throw new CustomException(Messages.invalidShortLink(prefix));
        }
        return inputLink.substring(prefix.length());
    }
}
