package utils;

import models.Medicine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportExporter {

    public static Path exportMedicinesCsv(List<Medicine> list) throws IOException {
        Path dir = Paths.get("exports");
        Files.createDirectories(dir);

        String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        Path file = dir.resolve("medicines_" + ts + ".csv");

        try (BufferedWriter bw = Files.newBufferedWriter(file);
             PrintWriter out = new PrintWriter(bw)) {

            out.println("ID,Name,Category,Price,Quantity,Expiry,ImagePath");

            for (Medicine m : list) {
                out.printf("%d,%s,%s,%.2f,%d,%s,%s%n",
                        m.getId(),
                        csv(m.getName()),
                        csv(m.getCategory()),
                        m.getPrice(),
                        m.getQuantity(),
                        csv(m.getExpiryDate()),
                        csv(m.getImagePath())
                );
            }
        }
        return file;
    }

    private static String csv(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
