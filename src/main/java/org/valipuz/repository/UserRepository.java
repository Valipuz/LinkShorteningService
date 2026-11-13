package org.valipuz.repository;

import java.util.UUID;

public interface UserRepository {
    void saveUser(UUID userId);
    boolean userExists(UUID userId);
}
