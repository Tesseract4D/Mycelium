package cn.tesseract.mycelium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;

public abstract class ConfigJSON<T> extends Config {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    public final Class<T> clazz;
    public T instance;

    public ConfigJSON(File file, Class<T> clazz) {
        super(file);
        this.clazz = clazz;
    }

    public ConfigJSON(String name, Class<T> clazz) {
        super(name + ".json");
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
