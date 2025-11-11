package utils;

import java.sql.*;

public class DBHelper {

    private static final String DB_URL = "jdbc:sqlite:medimart.db";
    private static final int BUSY_TIMEOUT_MS = 5000; // helps when reads/writes overlap
    private static Connection conn;

    /**
     * Get a singleton connection to SQLite.
     * Applies PRAGMAs once, ensures tables exist, runs light migrations, and ensures indexes.
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to SQLite database!");
            applyPragmas(conn);
            createTables(conn);
            runMigrations(conn);   // safe, idempotent tweaks
            ensureIndexes(conn);   // create useful indexes (no-op if they exist)
        }
        return conn;
    }

    /**
     * PRAGMAs recommended for desktop apps:
     * - WAL: better concurrent read/write
     * - synchronous=NORMAL: good blend of safety and speed
     * - foreign_keys=ON: enforce FK constraints
     * - busy_timeout: avoid 'database is locked' right away
     *
     * Optional perf hints (safe to keep):
     * - temp_store=MEMORY: temp structures in RAM
     * - mmap_size: enable memory-mapped I/O if supported
     * - cache_size: set page cache size (~20MB here)
     */
    private static void applyPragmas(Connection c) {
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode = WAL");
            st.execute("PRAGMA synchronous = NORMAL");
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS);

            // Optional performance hints:
            st.execute("PRAGMA temp_store = MEMORY");
            st.execute("PRAGMA mmap_size = 268435456"); // 256 MB
            st.execute("PRAGMA cache_size = -20000");    // ~20 MB (negative => KB)
        } catch (SQLException e) {
            System.err.println("Error applying PRAGMAs: " + e.getMessage());
        }
    }

    /**
     * Create baseline tables if they don't exist.
     * - For a fresh DB, we include medicines.image_path, medicines.last_updated
     *   and users.address, users.avatar_path, users.updated_at.
     * - Existing DBs are handled by runMigrations().
     */
    private static void createTables(Connection c) throws SQLException {
        try (Statement stmt = c.createStatement()) {
            // users (fresh schema includes profile fields)
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

            // medicines (fresh schema includes image_path & last_updated)
            // Note: expiry column name kept as-is to match your current code.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS medicines (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name         TEXT NOT NULL,
                    category     TEXT NOT NULL,
                    price        REAL NOT NULL,
                    quantity     INTEGER NOT NULL,
                    expiry       TEXT NOT NULL,
                    image_path   TEXT,
                    last_updated INTEGER
                )
            """);

            // orders
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    userId INTEGER NOT NULL,
                    medicineId INTEGER NOT NULL,
                    quantity INTEGER NOT NULL,
                    orderDate TEXT NOT NULL,
                    FOREIGN KEY (userId) REFERENCES users(id),
                    FOREIGN KEY (medicineId) REFERENCES medicines(id)
                )
            """);
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Light, idempotent migrations that won’t break existing DBs.
     * - Adds medicines.image_path if missing.
     * - Adds medicines.last_updated if missing and backfills it once.
     * - Adds users.address / users.avatar_path / users.updated_at if missing.
     *   Backfills updated_at once.
     */
    private static void runMigrations(Connection c) {
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

        // === users.address ===
        if (!columnExists(c, "users", "address")) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE users ADD COLUMN address TEXT");
                System.out.println("Migrated: added users.address");
            } catch (SQLException e) {
                System.err.println("Migration note (users.address): " + e.getMessage());
            }
        }

        // === users.avatar_path ===
        if (!columnExists(c, "users", "avatar_path")) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE users ADD COLUMN avatar_path TEXT");
                System.out.println("Migrated: added users.avatar_path");
            } catch (SQLException e) {
                System.err.println("Migration note (users.avatar_path): " + e.getMessage());
            }
        }

        // === users.updated_at ===
        if (!columnExists(c, "users", "updated_at")) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE users ADD COLUMN updated_at INTEGER");
                st.execute("""
                    UPDATE users
                    SET updated_at = CAST(strftime('%s','now') AS INTEGER)
                    WHERE updated_at IS NULL
                """);
                System.out.println("Migrated: added users.updated_at + backfilled.");
            } catch (SQLException e) {
                System.err.println("Migration note (users.updated_at): " + e.getMessage());
            }
        }
    }

    /** Create useful indexes (idempotent). */
    private static void ensureIndexes(Connection c) {
        try (Statement st = c.createStatement()) {
            // Users: fast lookup by email & phone
            st.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone)");

            // Medicines: fast filters/sorts
            st.execute("CREATE INDEX IF NOT EXISTS idx_meds_name ON medicines(name)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_meds_category ON medicines(category)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_meds_price ON medicines(price)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_meds_lastupd ON medicines(last_updated)");

            // Orders: join helpers
            st.execute("CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(userId)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_orders_med ON orders(medicineId)");
        } catch (SQLException e) {
            System.err.println("Error creating indexes: " + e.getMessage());
        }
    }

    /**
     * Helper: check if a column exists in a table.
     */
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

    /**
     * Close the singleton connection safely.
     */
    public static synchronized void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            conn = null;
        }
    }
}
