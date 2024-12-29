package cn.tesseract.mycelium.config;

import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public abstract class Config {
    public static final File configDir = new File(Launch.minecraftHome, "config");

    public final File file;
    public String defaultConfig;

    public Config(File file, String defaultConfig) {
        this.file = file;
        this.defaultConfig = defaultConfig;
    }

    public Config(String file, String defaultConfig) {
        this.file = new File(configDir, file);
        this.defaultConfig = defaultConfig;
    }

    public abstract void read();

    public abstract void save(String config);

    public String readFile() {
        if (file.exists())
            try {
                return FileUtils.readFileToString(file);
            } catch (IOException ignored) {
            }
        else {
            resetFile();
        }
        return defaultConfig;
    }

    public void saveFile(String config) {
        try {
            if (!file.exists())
                file.createNewFile();
            FileUtils.writeStringToFile(file, config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Config resetFile() {
        try {
            if (!file.exists())
                file.createNewFile();
            FileUtils.writeStringToFile(file, defaultConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
}
