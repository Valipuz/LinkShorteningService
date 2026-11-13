package org.valipuz.repository.impl;

import com.google.gson.reflect.TypeToken;
import org.valipuz.entity.ShortLink;
import org.valipuz.repository.LinkRepository;
import org.valipuz.utils.JsonFileHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LinkRepositoryImpl implements LinkRepository {
    private final JsonFileHandler<List<ShortLink>> jsonFileHandler;
    private final Map<String, ShortLink> links;

    public LinkRepositoryImpl(String fileName) {
        jsonFileHandler = new JsonFileHandler<>(fileName, new TypeToken<List<ShortLink>>(){}.getType());
        links = new LinkedHashMap<>();

        List<ShortLink> loaded = jsonFileHandler.read();
        if (loaded != null) {
            for (ShortLink link : loaded) {
                links.put(link.getCode(), link);
            }
        }
    }

    @Override
    public void saveLink(ShortLink shortLink) {
        links.put(shortLink.getCode(), shortLink);
        flush();
    }

    @Override
    public Optional<ShortLink> findLink(String code) {
        return Optional.ofNullable(links.get(code));
    }

    @Override
    public List<ShortLink> findLinksByOwner(UUID ownerId) {
        return links.values().stream()
                .filter(link -> link.getOwnerId().equals(ownerId))
                .toList();
    }

    @Override
    public List<ShortLink> findAllLinks() {
        return new ArrayList<>(links.values());
    }

    @Override
    public void deleteLink(String code) {
        links.remove(code);
        flush();
    }

    @Override
    public void deleteLinks(List<String> codes) {
        for (String code : codes) {
            links.remove(code);
        }
        flush();
    }

    private void flush() {
        jsonFileHandler.write(new ArrayList<>(links.values()));
    }
}
