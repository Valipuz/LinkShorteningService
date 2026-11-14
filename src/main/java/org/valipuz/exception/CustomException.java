package org.valipuz.exception;

/**
 * Ошибка пользователя (неверный ввод, нет прав, ссылка недоступна), текст которой меню показывает в консоли
 */
public class CustomException extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }
}
