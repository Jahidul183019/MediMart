// src/utils/FileLogger.java
package utils;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogger {
    private static final Path LOG_DIR = Paths.get("logs");
    private static final Path LOG_FILE = LOG_DIR.resolve("app.log");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        try { Files.createDirectories(LOG_DIR); } catch (IOException ignored) {}
    }

    public static void info(String msg) { write("INFO", msg, null); }
    public static void warn(String msg) { write("WARN", msg, null); }
    public static void error(String msg, Throwable t) { write("ERROR", msg, t); }

    private static synchronized void write(String level, String msg, Throwable t) {
        String line = "[" + TS.format(LocalDateTime.now()) + "][" + level + "] " + msg + System.lineSeparator();
        try (FileWriter fw = new FileWriter(LOG_FILE.toFile(), true)) {
            fw.write(line);
            if (t != null) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                fw.write(sw.toString());
                fw.write(System.lineSeparator());
            }
        } catch (IOException ignored) {}
    }
}
