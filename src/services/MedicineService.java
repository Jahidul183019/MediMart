package services;

import models.Medicine;
import utils.DBHelper;
import utils.AppException;
import utils.FileLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// NEW: file I/O failsafe imports
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;

public class MedicineService {

    /* =======================
       Live-update listeners (in-process)
       ======================= */
    private final List<DBChangeListener> listeners =
            Collections.synchronizedList(new ArrayList<>());

    public void addChangeListener(DBChangeListener l) {
        if (l != null) listeners.add(l);
    }

    public void removeChangeListener(DBChangeListener l) {
        listeners.remove(l);
    }

    private void onDataChanged() {
        // 1) Notify in-process listeners (CustomerDashboard inside same JVM)
        List<DBChangeListener> snapshot;
        synchronized (listeners) { snapshot = new ArrayList<>(listeners); }
        for (DBChangeListener l : snapshot) {
            try { l.onChange(); } catch (Exception ignored) {}
        }
        // 2) Cross-process broadcast via sockets (Customer app on other machines)
        tryBroadcastRefresh();
    }

    /* =======================
       Queries
       ======================= */

    // NOTE: serial_number is the PK in DB.
    // In the model, Medicine.id == medicines.serial_number
    public List<Medicine> getAllMedicines() {
        String sql =
                "SELECT serial_number, name, category, price, quantity, expiry, image_path " +
                        "FROM medicines";

        List<Medicine> medicines = new ArrayList<>();

        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                medicines.add(new Medicine(
                        rs.getInt("serial_number"),            // model id
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("quantity"),
                        rs.getString("expiry"),
                        rsSafeGet(rs, "image_path")
                ));
            }

            // Snapshot to file (best-effort; never throws outward)
            try { writeSnapshot(medicines); } catch (Exception snapEx) {
                FileLogger.warn("Snapshot write failed: " + snapEx.getMessage());
            }

            return medicines;

        } catch (SQLException e) {
            FileLogger.error("DB error in getAllMedicines: " + e.getMessage(), e);

            // Fallback: attempt to read last good snapshot
            try {
                List<Medicine> cached = readSnapshot();
                if (!cached.isEmpty()) {
                    FileLogger.warn("DB read failed; served data from snapshot.");
                    return cached;
                }
            } catch (Exception ignored) {
                // ignore fallback failure; we’ll throw AppException below
            }

            throw new AppException("Failed to load medicines. Please try again.", e);
        }
    }

    // id parameter == medicines.serial_number
    public Medicine getMedicineById(int id) {
        String sql =
                "SELECT serial_number, name, category, price, quantity, expiry, image_path " +
                        "FROM medicines WHERE serial_number = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Medicine(
                        rs.getInt("serial_number"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("quantity"),
                        rs.getString("expiry"),
                        rsSafeGet(rs, "image_path")
                );
            }
            return null;

        } catch (SQLException e) {
            FileLogger.error("DB error in getMedicineById(" + id + "): " + e.getMessage(), e);
            throw new AppException("Failed to load medicine details. Please try again.", e);
        }
    }

    /* =======================
       Mutations (with failsafe queue on failure)
       ======================= */

    public boolean addMedicine(Medicine m) {
        if (m == null) return false;
        String sql = """
                INSERT INTO medicines(name, category, price, quantity, expiry, image_path, last_updated)
                VALUES(?,?,?,?,?, ?, CAST(strftime('%s','now') AS INTEGER))
                """;
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, safe(m.getName()));
            stmt.setString(2, safe(m.getCategory()));
            stmt.setDouble(3, m.getPrice());
            stmt.setInt(4, m.getQuantity());
            stmt.setString(5, safe(m.getExpiryDate()));
            setNullableString(stmt, 6, m.getImagePath());

            boolean ok = stmt.executeUpdate() > 0;
            if (ok) {
                FileLogger.info("Added medicine: " + m.getName());
                onDataChanged();
            }
            return ok;

        } catch (SQLException e) {
            FileLogger.error("DB error adding medicine: " + e.getMessage(), e);
            try { appendFailsafe("add", m); } catch (Exception ioEx) {
                FileLogger.warn("Failsafe log failed: " + ioEx.getMessage());
            }
            throw new AppException("Could not add medicine (database error).", e);
        }
    }

    // Returns generated serial_number
    public Integer addMedicineReturningId(Medicine m) {
        if (m == null) return null;
        String sql = """
                INSERT INTO medicines(name, category, price, quantity, expiry, image_path, last_updated)
                VALUES(?,?,?,?,?, ?, CAST(strftime('%s','now') AS INTEGER))
                """;
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, safe(m.getName()));
            stmt.setString(2, safe(m.getCategory()));
            stmt.setDouble(3, m.getPrice());
            stmt.setInt(4, m.getQuantity());
            stmt.setString(5, safe(m.getExpiryDate()));
            setNullableString(stmt, 6, m.getImagePath());

            int count = stmt.executeUpdate();
            if (count > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    Integer id = keys.next() ? keys.getInt(1) : null;
                    FileLogger.info("Added medicine (id=" + id + "): " + m.getName());
                    onDataChanged();
                    return id;
                }
            }
            return null;

        } catch (SQLException e) {
            FileLogger.error("DB error addMedicineReturningId: " + e.getMessage(), e);
            try { appendFailsafe("addReturningId", m); } catch (Exception ioEx) {
                FileLogger.warn("Failsafe log failed: " + ioEx.getMessage());
            }
            throw new AppException("Could not add medicine (database error).", e);
        }
    }

    // Update basic fields (no image_path)
    public boolean updateMedicine(int id, String name, String category,
                                  double price, int quantity, String expiry) {
        String sql = """
                UPDATE medicines
                SET name=?, category=?, price=?, quantity=?, expiry=?,
                    last_updated = CAST(strftime('%s','now') AS INTEGER)
                WHERE serial_number = ?
                """;
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, safe(name));
            stmt.setString(2, safe(category));
            stmt.setDouble(3, price);
            stmt.setInt(4, quantity);
            stmt.setString(5, safe(expiry));
            stmt.setInt(6, id);

            boolean ok = stmt.executeUpdate() > 0;
            if (ok) {
                FileLogger.info("Updated medicine (id=" + id + ")");
                onDataChanged();
            }
            return ok;

        } catch (SQLException e) {
            FileLogger.error("DB error updateMedicine(id=" + id + "): " + e.getMessage(), e);
            try {
                appendFailsafe("update",
                        new Medicine(id, name, category, price, quantity, expiry, null));
            } catch (Exception ioEx) {
                FileLogger.warn("Failsafe log failed: " + ioEx.getMessage());
            }
            throw new AppException("Could not update medicine (database error).", e);
        }
    }

    // Full update including image_path
    public boolean updateMedicine(Medicine m) {
        if (m == null) return false;
        String sql = """
                UPDATE medicines
                SET name=?, category=?, price=?, quantity=?, expiry=?, image_path=?,
                    last_updated = CAST(strftime('%s','now') AS INTEGER)
                WHERE serial_number = ?
                """;
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, safe(m.getName()));
            stmt.setString(2, safe(m.getCategory()));
            stmt.setDouble(3, m.getPrice());
            stmt.setInt(4, m.getQuantity());
            stmt.setString(5, safe(m.getExpiryDate()));
            setNullableString(stmt, 6, m.getImagePath());
            stmt.setInt(7, m.getId());  // model id == serial_number

            boolean ok = stmt.executeUpdate() > 0;
            if (ok) {
                FileLogger.info("Updated medicine (with image) id=" + m.getId());
                onDataChanged();
            }
            return ok;

        } catch (SQLException e) {
            FileLogger.error("DB error updateMedicine(with image) id=" +
                    (m != null ? m.getId() : null) + ": " + e.getMessage(), e);
            try { appendFailsafe("updateFull", m); }
            catch (Exception ioEx) { FileLogger.warn("Failsafe log failed: " + ioEx.getMessage()); }
            throw new AppException("Could not update medicine (database error).", e);
        }
    }

    // Update only quantity (used by admin or other services)
    public boolean updateQuantity(int id, int qty) {
        String sql = """
                UPDATE medicines
                SET quantity=?, last_updated = CAST(strftime('%s','now') AS INTEGER)
                WHERE serial_number = ?
                """;
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, qty);
            stmt.setInt(2, id);

            boolean ok = stmt.executeUpdate() > 0;
            if (ok) {
                FileLogger.info("Updated quantity id=" + id + " -> " + qty);
                onDataChanged();
            }
            return ok;

        } catch (SQLException e) {
            FileLogger.error("DB error updateQuantity(id=" + id + "): " + e.getMessage(), e);
            try {
                appendFailsafe("updateQty",
                        new Medicine(id, null, null, 0.0, qty, null, null));
            } catch (Exception ioEx) {
                FileLogger.warn("Failsafe log failed: " + ioEx.getMessage());
            }
            throw new AppException("Could not update quantity (database error).", e);
        }
    }

    // Update only image path
    public boolean updateImagePath(int id, String imagePath) {
        String sql = """
                UPDATE medicines
                SET image_path=?, last_updated = CAST(strftime('%s','now') AS INTEGER)
                WHERE serial_number = ?
                """;
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setNullableString(stmt, 1, imagePath);
            stmt.setInt(2, id);

            boolean ok = stmt.executeUpdate() > 0;
            if (ok) {
                FileLogger.info("Updated image_path id=" + id + " -> " + imagePath);
                onDataChanged();
            }
            return ok;

        } catch (SQLException e) {
            FileLogger.error("DB error updateImagePath(id=" + id + "): " + e.getMessage(), e);
            try {
                appendFailsafe("updateImage",
                        new Medicine(id, null, null, 0.0, 0, null, imagePath));
            } catch (Exception ioEx) {
                FileLogger.warn("Failsafe log failed: " + ioEx.getMessage());
            }
            throw new AppException("Could not update image (database error).", e);
        }
    }

    public boolean deleteMedicine(int id) {
        String sql = "DELETE FROM medicines WHERE serial_number = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            boolean ok = stmt.executeUpdate() > 0;
            if (ok) {
                FileLogger.info("Deleted medicine id=" + id);
                onDataChanged();
            }
            return ok;

        } catch (SQLException e) {
            FileLogger.error("DB error deleteMedicine(id=" + id + "): " + e.getMessage(), e);
            try {
                appendFailsafe("delete",
                        new Medicine(id, null, null, 0.0, 0, null, null));
            } catch (Exception ioEx) {
                FileLogger.warn("Failsafe log failed: " + ioEx.getMessage());
            }
            throw new AppException("Could not delete medicine (database error).", e);
        }
    }

    /* =======================
       Helpers
       ======================= */

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static void setNullableString(PreparedStatement ps, int idx, String val)
            throws SQLException {
        if (val == null || val.trim().isEmpty()) {
            ps.setNull(idx, Types.VARCHAR);
        } else {
            ps.setString(idx, val.trim());
        }
    }

    private static String rsSafeGet(ResultSet rs, String col) {
        try {
            String v = rs.getString(col);
            return rs.wasNull() ? null : v;
        } catch (SQLException ex) {
            return null;
        }
    }

    /* =======================
       File I/O failsafe (snapshot + offline queue)
       ======================= */

    private static final Path SNAPSHOT_FILE =
            Paths.get("data", "medicines_snapshot.json");
    private static final Path OFFLINE_QUEUE =
            Paths.get("data", "offline_queue.jsonl");

    private void ensureDataDir() throws Exception {
        Files.createDirectories(Paths.get("data"));
    }

    // Best-effort snapshot writer (JSON array)
    private void writeSnapshot(List<Medicine> list) throws Exception {
        ensureDataDir();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"savedAt\":").append(Instant.now().getEpochSecond())
                .append(",\"items\":[");
        for (int i = 0; i < list.size(); i++) {
            Medicine m = list.get(i);
            sb.append(toJson(m));
            if (i < list.size() - 1) sb.append(',');
        }
        sb.append("]}");
        Files.writeString(SNAPSHOT_FILE, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // Read snapshot (returns empty list if missing/bad)
    private List<Medicine> readSnapshot() throws Exception {
        if (!Files.exists(SNAPSHOT_FILE)) return List.of();
        String json = Files.readString(SNAPSHOT_FILE, StandardCharsets.UTF_8);
        // very small/no-deps parser (expects the structure we wrote)
        List<Medicine> out = new ArrayList<>();
        int arrStart = json.indexOf("\"items\":[");
        if (arrStart < 0) return List.of();
        int start = json.indexOf('[', arrStart);
        int end = json.lastIndexOf(']');
        if (start < 0 || end < 0 || end <= start) return List.of();
        String arr = json.substring(start + 1, end).trim();
        if (arr.isEmpty()) return List.of();

        // split by top-level commas
        int brace = 0;
        StringBuilder item = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') brace++;
            if (c == '}') brace--;
            if (c == ',' && brace == 0) {
                parseMedicineJson(item.toString().trim(), out);
                item.setLength(0);
            } else {
                item.append(c);
            }
        }
        if (item.length() > 0) parseMedicineJson(item.toString().trim(), out);
        return out;
    }

    // append a JSON line describing a failed mutation
    private void appendFailsafe(String op, Medicine m) throws Exception {
        ensureDataDir();
        String line = "{\"ts\":" + Instant.now().getEpochSecond()
                + ",\"op\":\"" + escape(op) + "\",\"payload\":" + toJson(m) + "}\n";
        Files.writeString(OFFLINE_QUEUE, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String toJson(Medicine m) {
        if (m == null) return "null";
        return new StringBuilder()
                .append("{")
                .append("\"id\":").append(m.getId()).append(',')
                .append("\"name\":").append(str(m.getName())).append(',')
                .append("\"category\":").append(str(m.getCategory())).append(',')
                .append("\"price\":").append(m.getPrice()).append(',')
                .append("\"quantity\":").append(m.getQuantity()).append(',')
                .append("\"expiry\":").append(str(m.getExpiryDate())).append(',')
                .append("\"imagePath\":").append(str(m.getImagePath()))
                .append("}")
                .toString();
    }

    private static String str(String s) {
        return (s == null) ? "null" : "\"" + escape(s) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // tiny parser for our flat Medicine JSON (only keys we emit)
    private static void parseMedicineJson(String obj, List<Medicine> out) {
        try {
            int id = intVal(obj, "\"id\":", 0);
            String name = strVal(obj, "\"name\":");
            String cat = strVal(obj, "\"category\":");
            double price = doubleVal(obj, "\"price\":", 0.0);
            int qty = intVal(obj, "\"quantity\":", 0);
            String exp = strVal(obj, "\"expiry\":");
            String img = strVal(obj, "\"imagePath\":");
            out.add(new Medicine(id, name, cat, price, qty, exp, img));
        } catch (Exception ignored) {}
    }

    private static int intVal(String s, String key, int def) {
        int i = s.indexOf(key);
        if (i < 0) return def;
        i += key.length();
        int j = i;
        while (j < s.length() && "-0123456789".indexOf(s.charAt(j)) >= 0) j++;
        try { return Integer.parseInt(s.substring(i, j)); }
        catch (Exception e) { return def; }
    }

    private static double doubleVal(String s, String key, double def) {
        int i = s.indexOf(key);
        if (i < 0) return def;
        i += key.length();
        int j = i;
        while (j < s.length() && "-0123456789.".indexOf(s.charAt(j)) >= 0) j++;
        try { return Double.parseDouble(s.substring(i, j)); }
        catch (Exception e) { return def; }
    }

    private static String strVal(String s, String key) {
        int i = s.indexOf(key);
        if (i < 0) return null;
        i += key.length();
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        if (i < s.length() && s.charAt(i) == 'n') return null; // "null"
        if (i >= s.length() || s.charAt(i) != '"') return null;
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '\\' && i < s.length()) {
                char n = s.charAt(i++);
                sb.append(n == '"' ? '"' : n == '\\' ? '\\' : n);
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /* =======================
       Socket broadcast (safe even if class not present)
       ======================= */

    private void tryBroadcastRefresh() {
        try {
            Class<?> cls = Class.forName("net.MedicineSync");
            Object inst = cls.getMethod("getInstance").invoke(null);
            cls.getMethod("broadcastRefresh").invoke(inst);
        } catch (ClassNotFoundException noNet) {
            // sockets not on classpath – ignore
        } catch (Throwable t) {
            FileLogger.warn("broadcastRefresh failed: " + t.getMessage());
        }
    }
}
