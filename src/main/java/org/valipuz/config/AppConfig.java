package org.valipuz.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private final Properties properties = new Properties();

    public AppConfig(String fileName) {
        try (InputStream in = AppConfig.class.getResourceAsStream("/" + fileName)) {
            if (in == null) {
                throw new IllegalStateException("Не найден файл " + fileName);
            }
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать файл " + fileName, e);
        }
    }

    public int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public String getString(String key) {
        return properties.getProperty(key);
    }
}
