package utils;

import java.sql.*;

public class DBHelper {

    private static final String DB_URL = "jdbc:sqlite:medimart.db";
    private static final int BUSY_TIMEOUT_MS = 5000;
    private static Connection conn;

    /**
     * Get a singleton connection to SQLite.
     * Applies PRAGMAs once, ensures tables exist, runs migrations, ensures indexes.
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to SQLite!");
            applyPragmas(conn);
            createTables(conn);
            runMigrations(conn);
            ensureIndexes(conn);
        }
        return conn;
    }

    private static void applyPragmas(Connection c) {
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode = WAL");
            st.execute("PRAGMA synchronous = NORMAL");
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS);

            st.execute("PRAGMA temp_store = MEMORY");
            st.execute("PRAGMA mmap_size = 268435456"); // 256 MB
            st.execute("PRAGMA cache_size = -20000");    // ~20 MB
        } catch (SQLException e) {
            System.err.println("Error applying PRAGMAs: " + e.getMessage());
        }
    }

    /**
     * Create baseline tables if they don't exist.
     */
    private static void createTables(Connection c) throws SQLException {
        try (Statement stmt = c.createStatement()) {

            // users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    firstName   TEXT NOT NULL,
                    lastName    TEXT NOT NULL,
                    phone       TEXT NOT NULL,
                    email       TEXT UNIQUE NOT NULL,
                    password    TEXT NOT NULL,
                    address     TEXT,
                    avatar_path TEXT,
                    updated_at  INTEGER
                )
            """);

            // medicines table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS medicines (
                    serial_number INTEGER PRIMARY KEY AUTOINCREMENT,
                    name          TEXT NOT NULL,
                    category      TEXT NOT NULL,
                    price         REAL NOT NULL,
                    quantity      INTEGER NOT NULL,
                    expiry        TEXT NOT NULL,
                    image_path    TEXT,
                    last_updated  INTEGER
                )
            """);

            // ONE table for orders + items
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    order_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER NOT NULL,
                    medicine_id INTEGER NOT NULL,
                    quantity    INTEGER NOT NULL,
                    total_price REAL NOT NULL,
                    order_date  TEXT NOT NULL,
                    FOREIGN KEY (medicine_id) REFERENCES medicines(serial_number),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Migrations for already-existing DBs (old schema).
     */
    private static void runMigrations(Connection c) {
        try (Statement st = c.createStatement()) {

            // --- order_items.user_id (old table may not have it)
            if (!columnExists(c, "order_items", "user_id")) {
                st.execute("ALTER TABLE order_items ADD COLUMN user_id INTEGER");
                System.out.println("Migrated: added order_items.user_id");
            }

            // --- order_items.total_price
            if (!columnExists(c, "order_items", "total_price")) {
                st.execute("ALTER TABLE order_items ADD COLUMN total_price REAL");
                System.out.println("Migrated: added order_items.total_price");
            }

            // --- order_items.order_date
            if (!columnExists(c, "order_items", "order_date")) {
                st.execute("ALTER TABLE order_items ADD COLUMN order_date TEXT");
                System.out.println("Migrated: added order_items.order_date");
            }

        } catch (SQLException e) {
            System.err.println("Migration note (order_items): " + e.getMessage());
        }

        // === medicines.image_path ===
        if (!columnExists(c, "medicines", "image_path")) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE medicines ADD COLUMN image_path TEXT");
                System.out.println("Migrated: added medicines.image_path");
            } catch (SQLException e) {
                System.err.println("Migration note (medicines.image_path): " + e.getMessage());
            }
        }

        // === medicines.last_updated ===
        if (!columnExists(c, "medicines", "last_updated")) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE medicines ADD COLUMN last_updated INTEGER");
                st.execute("""
                    UPDATE medicines
                    SET last_updated = CAST(strftime('%s','now') AS INTEGER)
                    WHERE last_updated IS NULL
                """);
                System.out.println("Migrated: added medicines.last_updated + backfilled.");
            } catch (SQLException e) {
                System.err.println("Migration note (medicines.last_updated): " + e.getMessage());
            }
        }
    }

    private static void ensureIndexes(Connection c) {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone)");

            st.execute("CREATE INDEX IF NOT EXISTS idx_meds_name ON medicines(name)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_meds_category ON medicines(category)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_meds_price ON medicines(price)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_meds_lastupd ON medicines(last_updated)");

            st.execute("CREATE INDEX IF NOT EXISTS idx_order_items_user ON order_items(user_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_order_items_med ON order_items(medicine_id)");
        } catch (SQLException e) {
            System.err.println("Error creating indexes: " + e.getMessage());
        }
    }

    private static boolean columnExists(Connection c, String table, String column) {
        String sql = "PRAGMA table_info(" + table + ")";
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (column.equalsIgnoreCase(name)) return true;
            }
        } catch (SQLException e) {
            System.err.println("Error checking column existence: " + e.getMessage());
        }
        return false;
    }

    public static synchronized void closeConnection() {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    System.out.println("Connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            } finally {
                conn = null;
            }
        }
    }
}
