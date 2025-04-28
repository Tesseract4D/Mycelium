package cn.tesseract.mycelium.lua;

import cn.tesseract.mycelium.MyceliumCoreMod;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import cpw.mods.fml.common.asm.transformers.AccessTransformer;

import java.io.File;
import java.io.IOException;

public class LuaAccessTransformer extends AccessTransformer {
    public LuaAccessTransformer() throws IOException {
        File[] files = MyceliumCoreMod.scriptDir.listFiles();
        if (files != null)
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith("_at.cfg")) {
                    processATFile(Resources.asCharSource(file.toURI().toURL(), Charsets.UTF_8));
                    MyceliumCoreMod.logger.info("Loaded rules from AccessTransformer config file \"{}\"", file.getName());
                }
            }
    }
}
