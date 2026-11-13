package org.valipuz.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CodeGeneratorTest {
    @Test
    @DisplayName("Код имеет заданную длину и состоит только из латинских букв и цифр")
    void codeHasGivenLengthAndAllowedChars() {
        CodeGenerator generator = new CodeGenerator(6);

        String code = generator.generate();

        assertEquals(6, code.length());
        assertTrue(code.matches("[A-Za-z0-9]+"));
    }

    @Test
    @DisplayName("Длина кода берётся из параметра конструктора")
    void codeLengthComesFromConstructor() {
        assertEquals(10, new CodeGenerator(10).generate().length());
    }

    @Test
    @DisplayName("Генератор выдаёт разные коды")
    void generatesDifferentCodes() {
        CodeGenerator generator = new CodeGenerator(6);
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }

        assertEquals(1000, codes.size());
    }
}
