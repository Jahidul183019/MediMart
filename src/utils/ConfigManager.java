// src/utils/ConfigManager.java
package utils;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String PATH = "config/app.properties";
    private static Properties props;

    public static synchronized String get(String key, String def) {
        ensureLoaded();
        return props.getProperty(key, def);
    }

    private static void ensureLoaded() {
        if (props != null) return;
        props = new Properties();
        try (FileInputStream fis = new FileInputStream(PATH)) {
            props.load(fis);
        } catch (IOException ignored) {
            // first run? create minimal file:
            try (FileOutputStream fos = new FileOutputStream(PATH)) {
                props.setProperty("socket.enabled","true");
                props.setProperty("socket.port","5050");
                props.store(fos, "MediMart config");
            } catch (IOException ignored2) {}
        }
    }
}
