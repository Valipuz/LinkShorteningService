package org.valipuz.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonFileHandler<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final Type type;

    public JsonFileHandler(String fileName, Type type) {
        this.path = Path.of(fileName);
        this.type = type;
    }

    public T read() {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return GSON.fromJson(Files.readString(path), type);
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Не удалось прочитать файл " + path, e);
        }
    }

    public void write(T data) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, GSON.toJson(data, type));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить файл " + path, e);
        }
    }
}
