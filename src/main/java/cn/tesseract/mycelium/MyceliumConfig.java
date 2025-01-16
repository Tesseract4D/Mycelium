package cn.tesseract.mycelium;

import cn.tesseract.mycelium.config.Comment;
import cn.tesseract.mycelium.config.ConfigProperties;

public class MyceliumConfig extends ConfigProperties {
    @Comment("Noclip in creative mode when fly.")
    public boolean creativeNoclip = true;
    @Comment("Noclip in survival mode when fly as well, someone wants it so here is the config.")
    public boolean survivalNoclip = false;
    @Comment("Fix inventory effect gui black block when selecting certain item.")
    public boolean effectBlackBlockFix = true;
    @Comment("Speed up language reload.")
    public boolean fastLang = true;

    public MyceliumConfig() {
        super("mycelium");
    }
}
