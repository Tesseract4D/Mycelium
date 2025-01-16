package cn.tesseract.mycelium.config;

import com.google.gson.Gson;

import java.io.File;

public abstract class ConfigJSON<T> extends Config {
    public static final Gson GSON = new Gson();
    public final Class<T> clazz;
    public T instance;

    public ConfigJSON(File file, Class<T> clazz) {
        super(file);
        this.clazz = clazz;
    }

    public ConfigJSON(String file, Class<T> clazz) {
        super(file + ".json");
        this.clazz = clazz;
    }

    @Override
    public void read() {
        instance = GSON.fromJson(readFile(), clazz);
    }

    @Override
    public void save() {
        saveFile(GSON.toJson(instance));
    }
}
