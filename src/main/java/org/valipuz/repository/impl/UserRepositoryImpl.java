package org.valipuz.repository.impl;

import com.google.gson.reflect.TypeToken;
import org.valipuz.repository.UserRepository;
import org.valipuz.utils.JsonFileHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UserRepositoryImpl implements UserRepository {
    private final JsonFileHandler<Set<UUID>> jsonFileHandler;
    private final Set<UUID> users;

    public UserRepositoryImpl(String fileName) {
        jsonFileHandler = new JsonFileHandler<>(fileName, new TypeToken<Set<UUID>>(){}.getType());
        Set<UUID> loaded = jsonFileHandler.read();
        users = loaded == null ? new HashSet<>() : loaded;
    }

    @Override
    public void saveUser(UUID userId) {
        users.add(userId);
        jsonFileHandler.write(users);
    }

    @Override
    public boolean userExists(UUID userId) {
        return users.contains(userId);
    }
}
