package cn.tesseract.mycelium.lua;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.luaj.vm2.LuaValue;

public class LuaLogger {
    public static final Logger logger = LogManager.getLogger("Lua");

    public void info(String message) {
        logger.info(message);
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void warning(String message) {
        logger.warn(message);
    }

    public void error(String message) {
        logger.error(message);
    }
}
