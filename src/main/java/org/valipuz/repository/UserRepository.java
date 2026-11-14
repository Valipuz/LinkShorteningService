package org.valipuz.repository;

import java.util.UUID;

/**
 * Хранилище UUID зарегистрированных пользователей
 */
public interface UserRepository {
    void saveUser(UUID userId);
    boolean userExists(UUID userId);
}
