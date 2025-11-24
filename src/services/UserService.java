package services;

import models.User;
import utils.DBHelper;
import org.mindrot.jbcrypt.BCrypt;
import utils.AppException;
import utils.FileLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    /* ======================
       Signup / Login
       ====================== */

    // Backwards-compatible signup (no address/avatar provided)
    public boolean signup(String firstName, String lastName, String phone, String email, String password) {
        return signup(firstName, lastName, phone, email, password, null, null);
    }

    // Signup with optional address & avatarPath, stamps updated_at
    public boolean signup(String firstName, String lastName, String phone, String email,
                          String password, String address, String avatarPath) {
        final String sql = """
            INSERT INTO users(firstName, lastName, phone, email, password, address, avatar_path, updated_at)
            VALUES(?,?,?,?,?,?,?, CAST(strftime('%s','now') AS INTEGER))
        """;

        final String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nz(firstName));
            pstmt.setString(2, nz(lastName));
            pstmt.setString(3, nz(phone));
            pstmt.setString(4, nz(email));
            pstmt.setString(5, hashedPassword);
            setNullable(pstmt, 6, address);
            setNullable(pstmt, 7, avatarPath);

            pstmt.executeUpdate();
            FileLogger.info("Signup success for " + email);
            return true;

        } catch (SQLException e) {
            FileLogger.error("Signup failed for " + email + ": " + e.getMessage(), e);
            return false;
        }
    }

    public User login(String email, String password) {
        final String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                if (storedPassword != null && BCrypt.checkpw(password, storedPassword)) {
                    if (needsRehash(storedPassword)) {
                        updatePasswordByEmail(email, password); // will hash inside
                    }
                    return mapUserBasic(rs);
                } else {
                    FileLogger.info("Login invalid password for " + email);
                    return null;
                }
            }
        } catch (SQLException e) {
            FileLogger.error("Login failed for " + email + ": " + e.getMessage(), e);
        }
        return null;
    }

    /* ======================
       Queries / Getters
       ====================== */

    public boolean isEmailRegistered(String email) {
        final String sql = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            FileLogger.error("isEmailRegistered error for " + email + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean isPhoneRegistered(String phone) {
        final String sql = "SELECT id FROM users WHERE phone = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            FileLogger.error("isPhoneRegistered error for " + phone + ": " + e.getMessage(), e);
            return false;
        }
    }

    public User getUserById(int id) {
        final String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            var rs = ps.executeQuery();
            if (rs.next()) return mapUserBasic(rs);
        } catch (SQLException e) {
            FileLogger.error("getUserById(" + id + ") failed: " + e.getMessage(), e);
        }
        return null;
    }

    public String getAddressById(int id) {
        final String sql = "SELECT address FROM users WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            var rs = ps.executeQuery();
            return rs.next() ? rs.getString("address") : null;
        } catch (SQLException e) {
            FileLogger.error("getAddressById(" + id + ") failed: " + e.getMessage(), e);
            return null;
        }
    }

    public String getAvatarPathById(int id) {
        final String sql = "SELECT avatar_path FROM users WHERE id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            var rs = ps.executeQuery();
            return rs.next() ? rs.getString("avatar_path") : null;
        } catch (SQLException e) {
            FileLogger.error("getAvatarPathById(" + id + ") failed: " + e.getMessage(), e);
            return null;
        }
    }

    /* ======================
       Profile Updates
       ====================== */

    public boolean updateProfile(int id,
                                 String firstName,
                                 String lastName,
                                 String phone,
                                 String address,
                                 String avatarPath) {
        final String sql = """
            UPDATE users
               SET firstName = ?, lastName = ?, phone = ?, address = ?, avatar_path = ?,
                   updated_at = CAST(strftime('%s','now') AS INTEGER)
             WHERE id = ?
        """;
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nz(firstName));
            ps.setString(2, nz(lastName));
            ps.setString(3, nz(phone));
            setNullable(ps, 4, address);
            setNullable(ps, 5, avatarPath);
            ps.setInt(6, id);

            boolean ok = ps.executeUpdate() > 0;
            if (!ok) FileLogger.warn("updateProfile: no row updated for id=" + id);
            return ok;

        } catch (SQLException e) {
            FileLogger.error("updateProfile failed for id=" + id + ": " + e.getMessage(), e);
            throw new AppException("Could not update profile. Please try again.", e);
        }
    }

    // Overload: accept User directly (used by ProfileView)
    public boolean updateProfile(User u) {
        if (u == null) return false;
        return updateProfile(u.getId(), u.getFirstName(), u.getLastName(),
                u.getPhone(), safeGetAddress(u), safeGetAvatar(u));
    }

    public boolean updateAddress(int id, String address) {
        final String sql = """
            UPDATE users
               SET address = ?, updated_at = CAST(strftime('%s','now') AS INTEGER)
             WHERE id = ?
        """;
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullable(ps, 1, address);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            FileLogger.error("updateAddress failed for id=" + id + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean updateAvatarPath(int id, String avatarPath) {
        final String sql = """
            UPDATE users
               SET avatar_path = ?, updated_at = CAST(strftime('%s','now') AS INTEGER)
             WHERE id = ?
        """;
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullable(ps, 1, avatarPath);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            FileLogger.error("updateAvatarPath failed for id=" + id + ": " + e.getMessage(), e);
            return false;
        }
    }

    // Friendly alias for UI code
    public boolean updateAvatar(int id, String path) {
        return updateAvatarPath(id, path);
    }

    /* ======================
       Password Updates
       ====================== */

    public boolean updatePasswordByEmail(String email, String newPassword) {
        final String sql = """
            UPDATE users
               SET password = ?, updated_at = CAST(strftime('%s','now') AS INTEGER)
             WHERE email = ?
        """;
        final String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());

        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hashed);
            pstmt.setString(2, email);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            FileLogger.error("updatePasswordByEmail failed for " + email + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean updatePasswordByPhone(String phone, String newPassword) {
        final String sql = """
            UPDATE users
               SET password = ?, updated_at = CAST(strftime('%s','now') AS INTEGER)
             WHERE phone = ?
        """;
        final String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());

        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hashed);
            pstmt.setString(2, phone);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            FileLogger.error("updatePasswordByPhone failed for " + phone + ": " + e.getMessage(), e);
            return false;
        }
    }

    // Validate old password and set new one
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        final String sqlGet = "SELECT password FROM users WHERE id=?";
        final String sqlUpd = """
            UPDATE users SET password=?, updated_at=CAST(strftime('%s','now') AS INTEGER)
             WHERE id=?
        """;
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement get = conn.prepareStatement(sqlGet)) {

            get.setInt(1, userId);
            ResultSet rs = get.executeQuery();
            if (rs.next()) {
                String oldHash = rs.getString("password");
                if (oldHash != null && BCrypt.checkpw(oldPassword, oldHash)) {
                    String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                    try (PreparedStatement upd = conn.prepareStatement(sqlUpd)) {
                        upd.setString(1, newHash);
                        upd.setInt(2, userId);
                        return upd.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            FileLogger.error("changePassword failed for id=" + userId + ": " + e.getMessage(), e);
        }
        return false;
    }

    /* ======================
       Avatar File Storage
       ====================== */

    /** Copy chosen avatar image into app storage (./avatars/avatar_{id}.ext) and return absolute path. */
    public Path saveAvatarToAppStorage(int userId, Path sourcePath) throws IOException {
        Path dir = Path.of("avatars");
        if (!Files.exists(dir)) Files.createDirectories(dir);

        String ext = getFileExtension(sourcePath.getFileName().toString());
        Path target = dir.resolve("avatar_" + userId + ext);

        Files.copy(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath();
    }

    private static String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot) : "";
    }

    /* ======================
       Helpers
       ====================== */

    private static boolean needsRehash(String hash) {
        // Very simple policy example: treat anything not starting with $2a$, $2b$, or $2y$ as needing rehash.
        return !(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }

    private static void setNullable(PreparedStatement ps, int idx, String val) throws SQLException {
        if (val == null || val.trim().isEmpty()) ps.setNull(idx, java.sql.Types.VARCHAR);
        else ps.setString(idx, val.trim());
    }

    // Map the known fields; attempt to read address & avatar_path if present (safe if model lacks setters)
    private static User mapUserBasic(ResultSet rs) throws SQLException {
        User u = new User(
                rs.getInt("id"),
                rs.getString("firstName"),
                rs.getString("lastName"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("password")
        );
        try { u.setAddress(rs.getString("address")); } catch (Throwable ignored) {}
        try { u.setAvatarPath(rs.getString("avatar_path")); } catch (Throwable ignored) {}
        return u;
    }

    // Safely read from model (compatible even if getters are missing in older model)
    private static String safeGetAddress(User u) {
        try { return u.getAddress(); } catch (Throwable t) { return null; }
    }
    private static String safeGetAvatar(User u) {
        try { return u.getAvatarPath(); } catch (Throwable t) { return null; }
    }
}
