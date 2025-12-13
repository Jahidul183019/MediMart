package services;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import models.OrderItem;
import utils.DBHelper;

import java.sql.*;
import java.util.List;

public class OrderService {

    /**
     * Places order into order_items table and auto-reduces stock
     */
    public boolean placeOrder(int userId, List<OrderItem> cart) {
        String insertItemSql =
                "INSERT INTO order_items(user_id, medicine_id, quantity, total_price, order_date) " +
                        "VALUES(?, ?, ?, ?, ?)";

        //  USE serial_number instead of medicine_id
        String updateStockSql =
                "UPDATE medicines " +
                        "SET quantity = quantity - ?, " +
                        "    last_updated = strftime('%s','now') " +
                        "WHERE serial_number = ? AND quantity >= ?";

        String orderDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date());

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement insertItem = conn.prepareStatement(insertItemSql);
                 PreparedStatement updateStock = conn.prepareStatement(updateStockSql)) {

                for (OrderItem item : cart) {

                    int medId = item.getMedicine().getId();   // serial_number
                    int qty = item.getQuantity();
                    double lineTotal = item.getTotalPrice();

                    // ---- INSERT ORDER ITEM ----
                    insertItem.setInt(1, userId);
                    insertItem.setInt(2, medId);
                    insertItem.setInt(3, qty);
                    insertItem.setDouble(4, lineTotal);
                    insertItem.setString(5, orderDate);
                    insertItem.addBatch();

                    // ---- UPDATE STOCK ----
                    updateStock.setInt(1, qty);
                    updateStock.setInt(2, medId);
                    updateStock.setInt(3, qty);
                    updateStock.addBatch();
                }

                insertItem.executeBatch();
                int[] results = updateStock.executeBatch();

                for (int r : results) {
                    if (r == 0) {
                        conn.rollback();
                        showError("Stock update failed for one or more medicines.");
                        return false;
                    }
                }

                conn.commit();
            }

            showSuccess("Order placed successfully!");
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database error while placing order: " + e.getMessage());
            return false;
        }
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(AlertType.INFORMATION);
        a.setTitle("Order Success");
        a.setHeaderText("Order completed");
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(AlertType.ERROR);
        a.setTitle("Order Error");
        a.setHeaderText("Order Failed");
        a.setContentText(msg);
        a.showAndWait();
    }
}
