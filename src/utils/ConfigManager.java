package utils;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigManager {
    private static final String PATH = "config/app.properties";
    private static Properties props;
    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());

    public static synchronized String get(String key, String def) {
        ensureLoaded();
        return props.getProperty(key, def);
    }

    private static void ensureLoaded() {
        if (props != null) return;

        // Ensure the config directory exists
        Path configPath = Paths.get(PATH).getParent();
        if (configPath != null && !Files.exists(configPath)) {
            try {
                Files.createDirectories(configPath); // Create the config folder if it doesn't exist
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to create config directory", e);
                return;
            }
        }

        props = new Properties();

        try (FileInputStream fis = new FileInputStream(PATH)) {
            props.load(fis);  // Load the properties from the file
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load config file, creating new one...", e);

            // First run? Create a minimal default configuration file
            try (FileOutputStream fos = new FileOutputStream(PATH)) {
                props.setProperty("socket.enabled", "true");
                props.setProperty("socket.port", "5050");
                props.store(fos, "MediMart config");
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Failed to write default config file", ex);
            }
        }
    }
}
