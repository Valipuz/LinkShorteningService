package org.valipuz.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.valipuz.exception.CustomException;
import org.valipuz.repository.impl.UserRepositoryImpl;
import org.valipuz.utils.Messages;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    @TempDir
    Path tempDir;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(new UserRepositoryImpl(tempDir.resolve("users.json").toString()));
    }

    @Test
    @DisplayName("Вход по UUID зарегистрированного пользователя возвращает этот же UUID")
    void signInRegisteredUser() {
        UUID userId = userService.register();

        assertEquals(userId, userService.signIn(userId.toString()));
    }

    @Test
    @DisplayName("Вход по незарегистрированному UUID даёт ошибку")
    void signInUnknownUser() {
        String unknown = UUID.randomUUID().toString();

        CustomException exception = assertThrows(CustomException.class, () -> userService.signIn(unknown));

        assertEquals(Messages.USER_NOT_FOUND, exception.getMessage());
    }

    @Test
    @DisplayName("Вход по строке не в формате UUID даёт IllegalArgumentException")
    void signInInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> userService.signIn("abc"));
    }
}
