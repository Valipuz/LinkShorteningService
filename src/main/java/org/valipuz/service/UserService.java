package org.valipuz.service;

import org.valipuz.exception.CustomException;
import org.valipuz.repository.UserRepository;
import org.valipuz.utils.Messages;

import java.util.UUID;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Идентификация пользователя по id
     *
     * @param input id пользователя в строке
     * @return id идентифицированного пользователя
     */
    public UUID signIn(String input) {
        UUID userId = UUID.fromString(input);
        if (exists(userId)) {
            return userId;
        }
        throw new CustomException(Messages.USER_NOT_FOUND);
    }

    /**
     * Регистрация пользователя
     *
     * @return id пользователя
     */
    public UUID register() {
        UUID userId = UUID.randomUUID();
        userRepository.saveUser(userId);
        return userId;
    }

    /**
     * Проверка существования пользователя с таким id
     *
     * @param userId id пользователя
     * @return существует или нет
     */
    public boolean exists(UUID userId) {
        return userRepository.userExists(userId);
    }
}
