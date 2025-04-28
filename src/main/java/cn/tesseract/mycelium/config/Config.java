package cn.tesseract.mycelium.config;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.world.gen.feature.WorldGenBlockBlob;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public abstract class Config {
    public static final File configDir = new File(Launch.minecraftHome, "config");

    static {
        configDir.mkdir();
    }

    public final File file;

    public Config(File file) {
        this.file = file;
    }

    public Config(String name) {
        this.file = new File(configDir, name);
    }

    public abstract void read();

    public abstract void save();

    public String readFile() {
        if (file.exists())
            try {
                return FileUtils.readFileToString(file);
            } catch (IOException ignored) {
            }
        else {
            resetFile();
        }
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

    public void resetFile() {
        try {
            file.delete();
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}