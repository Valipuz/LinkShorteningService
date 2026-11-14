package org.valipuz.repository;

import org.valipuz.entity.ShortLink;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Хранилище коротких ссылок: сохранение, поиск по коду и по владельцу, удаление
 */
public interface LinkRepository {
    void saveLink(ShortLink shortLink);
    Optional<ShortLink> findLink(String code);
    List<ShortLink> findLinksByOwner(UUID ownerId);
    List<ShortLink> findAllLinks();
    void deleteLink(String code);
    void deleteLinks(List<String> codes);
}
