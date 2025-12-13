package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;

public class ImageStorage {

    // App-local folder (next to DB or under user.home)
    public static File imagesDir() {
        File base = new File("medimart_data/images");
        if (!base.exists()) base.mkdirs();
        return base;
    }

    public static String saveImage(File source) throws IOException {
        if (source == null || !source.exists()) return null;
        String ext = extOf(source.getName());
        String fileName = UUID.randomUUID().toString().replace("-", "") + (ext == null ? "" : "." + ext);
        File dest = new File(imagesDir(), fileName);
        Files.copy(source.toPath(), dest.toPath());
        return dest.getAbsolutePath(); // store absolute path in DB (simple + robust)
    }

    private static String extOf(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0) return null;
        return name.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}
