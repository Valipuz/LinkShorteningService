package org.valipuz.utils;

import java.security.SecureRandom;

/**
 * Генератор случайного кода короткой ссылки из латинских букв и цифр
 */
public class CodeGenerator {

    // не вынесен в конфиг, чтобы было сложнее менять (смена в конфиге сломает работу действующих коротких ссылок)
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SecureRandom random;
    private final int length;

    public CodeGenerator(int length) {
        random = new SecureRandom();
        this.length = length;
    }

    /**
     * Генерация кода для короткой ссылки
     *
     * @return код
     */
    public String generate() {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
