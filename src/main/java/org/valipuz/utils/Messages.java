package org.valipuz.utils;

/**
 * Тексты ошибок, которые программа показывает пользователю
 */
public final class Messages {
    public static final String CREATE_NEW_HINT =
        " Создайте новую короткую ссылку в меню пользователя (пункт 1 главного меню)";

    // пользователь
    public static final String USER_NOT_FOUND = "Пользователь с таким ID не найден";
    public static final String INVALID_USER_ID = "Некорректный формат ID. Пример: 3f2a8c1e-5b7d-4c21-9a10-7e6f2d1b8c44";

    // оригинальный URL
    public static final String EMPTY_URL = "Вы не ввели ссылку!";
    public static final String INVALID_URL = "Некорректный URL. Пример: https://github.com";

    // лимит переходов
    public static final String EMPTY_NEW_LIMIT = "Вы не ввели новый лимит кликов, не получится поменять!";
    public static final String LIMIT_NOT_PASSED = "Ошибка ввода лимита кликов";
    public static final String LIMIT_NOT_NUMBER = "Лимит должен быть целым числом";

    // короткая ссылка
    public static final String EMPTY_SHORT_LINK = "Вы не ввели короткую ссылку!";
    public static final String LINK_NOT_FOUND = "Короткая ссылка не найдена";
    public static final String LINK_NOT_FOUND_ON_OPEN =
        "Короткая ссылка не найдена: возможно, она была удалена владельцем или по истечении срока действия.";
    public static final String LINK_EXPIRED = "Срок действия ссылки истёк, она больше недоступна." + CREATE_NEW_HINT;
    public static final String LIMIT_REACHED = "Лимит переходов по ссылке исчерпан, ссылка заблокирована." + CREATE_NEW_HINT;
    public static final String NOT_OWNER = "Управлять ссылкой может только её владелец!";

    // сообщения зависят от значений из конфига, поэтому это методы, а не константы
    public static String limitOutOfRange(int maxLimit) {
        return "Лимит должен быть от 1 до " + maxLimit;
    }

    public static String invalidShortLink(String prefix) {
        return "Некорректный ввод короткой ссылки. Ссылка должна иметь вид " + prefix + "<код>, например " + prefix + "f3gh89";
    }

    private Messages() {
    }
}
