package services;

import models.Medicine;
import utils.DBHelper;

import java.sql.*;
import java.util.List;

public class OrderService {

    // ✅ Place order + reduce stock
    public boolean placeOrder(int userId, List<Medicine> cart) {
        String orderSql = "INSERT INTO orders(user_id, total) VALUES(?, ?)";
        String itemSql  = "INSERT INTO order_items(order_id, medicine_id, quantity) VALUES(?,?,?)";
        String stockSql = "UPDATE medicines SET quantity = quantity - ? WHERE id=?";

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);

            double total = cart.stream().mapToDouble(m -> m.getPrice() * m.getQuantity()).sum();

            // Save order
            PreparedStatement orderStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);
            orderStmt.setInt(1, userId);
            orderStmt.setDouble(2, total);
            orderStmt.executeUpdate();

            ResultSet rs = orderStmt.getGeneratedKeys();
            rs.next();
            int orderId = rs.getInt(1);

            // Insert items & reduce stock
            PreparedStatement itemStmt = conn.prepareStatement(itemSql);
            PreparedStatement stockStmt = conn.prepareStatement(stockSql);

            for (Medicine m : cart) {
                itemStmt.setInt(1, orderId);
                itemStmt.setInt(2, m.getId());
                itemStmt.setInt(3, m.getQuantity());
                itemStmt.executeUpdate();

                stockStmt.setInt(1, m.getQuantity());
                stockStmt.setInt(2, m.getId());
                stockStmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
