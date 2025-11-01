package services;

import models.Medicine;
import utils.DBHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicineService {

    public List<Medicine> getAllMedicines() {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT * FROM medicines";

        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Medicine m = new Medicine(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("quantity"),
                        rs.getString("expiry")
                );
                medicines.add(m);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return medicines;
    }

    public void addMedicine(Medicine m) {
        String sql = "INSERT INTO medicines(name, category, price, quantity, expiry) VALUES(?,?,?,?,?)";

        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, m.getName());
            pstmt.setString(2, m.getCategory());
            pstmt.setDouble(3, m.getPrice());
            pstmt.setInt(4, m.getQuantity());
            pstmt.setString(5, m.getExpiryDate());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateQuantity(int id, int newQty) {
        String sql = "UPDATE medicines SET quantity=? WHERE id=?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newQty);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteMedicine(int id) {
        String sql = "DELETE FROM medicines WHERE id=?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
