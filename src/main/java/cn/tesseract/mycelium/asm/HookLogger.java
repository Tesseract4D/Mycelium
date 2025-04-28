package cn.tesseract.mycelium.asm;

public interface HookLogger {

    void debug(String message);

    void warning(String message);

    void severe(String message);

    void severe(String message, Throwable cause);

    class SystemOutLogger implements HookLogger {

        @Override
        public void debug(String message) {
            System.out.println("[DEBUG] " + message);
        }

        @Override
        public void warning(String message) {
            System.out.println("[WARNING] " + message);
        }

        @Override
        public void severe(String message) {
            System.out.println("[SEVERE] " + message);
        }

        @Override
        public void severe(String message, Throwable cause) {
            severe(message);
            System.out.println(cause);
        }
    }
}
