package cn.tesseract.mycelium.config;

import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public abstract class Config {
    public static final String configDir = "config" + File.separator;

    static {
        new File(Launch.minecraftHome, configDir).mkdirs();
    }

    public final File file;
    public final String defaultConfig;

    public Config(String file, String defaultConfig) {
        this.file = new File(Launch.minecraftHome, configDir + file);
        this.defaultConfig = defaultConfig;
    }

    public abstract Config read();

    public abstract Config save(String config);

    public String readFile() {
        if (file.exists())
            try {
                return FileUtils.readFileToString(file);
            } catch (IOException ignored) {
            }
        else
            resetFile();
        return "";
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
