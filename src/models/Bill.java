package models;

import services.OrderService;
import utils.DBHelper;

import java.sql.*;
import java.util.Map;

import utils.Session;

public class Bill {
    private Cart cart;
    private Payment payment;

    // Constructor to initialize the Cart and Payment
    public Bill(Cart cart, Payment payment) {
        this.cart = cart;
        this.payment = payment;
    }

    /**
     * Generate a formatted receipt for the order.
     */
    public String generateReceipt(Map<Integer, Medicine> medicineMap) {
        StringBuilder sb = new StringBuilder();

        sb.append("============================================\n");
        sb.append("                MediMart                    \n");
        sb.append("============================================\n");
        sb.append("User ID     : ").append(Session.getCurrentUserId()).append("\n");
        sb.append("Payment ID  : ").append(payment.getId()).append("\n");
        sb.append("Date/Time   : ")
                .append(payment.getTimestamp().substring(0, 19).replace("T", " "))
                .append("\n");
        sb.append("Payment Status: ")
                .append(payment.isSuccess() ? "Paid" : "Pending").append("\n");
        sb.append("--------------------------------------------\n");
        sb.append(String.format("%-20s %5s %10s\n", "Item", "Qty", "Price"));
        sb.append("--------------------------------------------\n");

        double total = 0;

        // Loop through cart items and append them to the receipt
        for (Map.Entry<Integer, Integer> entry : cart.getBuyHistory().entrySet()) {
            int medicineId = entry.getKey();
            int qty = entry.getValue();

            Medicine medicine = medicineMap.get(medicineId);
            if (medicine != null) {
                double price = medicine.getPrice() * qty;
                total += price;

                sb.append(String.format("%-20s %5d %10.2f\n", medicine.getName(), qty, price));
            }
        }

        sb.append("--------------------------------------------\n");

        // Add Delivery, Tax, and other fees
        sb.append(String.format("%-25s %10.2f\n", "Delivery Fee:", cart.getDeliveryAmount()));
        sb.append(String.format("%-25s %10.2f\n", "Tax Amount:", cart.getTaxAmount()));
        sb.append(String.format("%-25s %10.2f\n", "Service Fee:", cart.getServiceFeeAmount()));
        sb.append(String.format("%-25s %10.2f\n", "Discount:", cart.getDiscount()));

        double finalTotal = total +
                cart.getDeliveryAmount() +
                cart.getTaxAmount() +
                cart.getServiceFeeAmount() -
                cart.getDiscount();

        sb.append("--------------------------------------------\n");
        sb.append(String.format("%-20s %15.2f\n", "TOTAL:", finalTotal));
        sb.append("============================================\n");
        sb.append("            Thank you for shopping!         \n");
        sb.append("============================================\n");

        return sb.toString();
    }

    /**
     * Store the order details in the database after payment.
     */
    public boolean storeOrderInDatabase(int userId, Map<Integer, Integer> cartItems, Map<Integer, Medicine> medicineMap) {
        // Calculate total amount
        double totalAmount = cartItems.entrySet().stream()
                .mapToDouble(entry -> medicineMap.get(entry.getKey()).getPrice() * entry.getValue())
                .sum();

        // Add order to database
        String orderSql = "INSERT INTO orders (userId, total, orderDate) VALUES (?, ?, ?)";
        String itemSql = "INSERT INTO order_items (order_id, medicine_id, quantity) VALUES (?, ?, ?)";
        String stockSql = "UPDATE medicines SET quantity = quantity - ? WHERE serial_number = ?";

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);

            // Insert order into orders table
            PreparedStatement orderStmt = conn.prepareStatement(orderSql, PreparedStatement.RETURN_GENERATED_KEYS);
            orderStmt.setInt(1, userId);
            orderStmt.setDouble(2, totalAmount);
            orderStmt.setString(3, payment.getTimestamp());
            orderStmt.executeUpdate();

            // Get generated order ID
            ResultSet rs = orderStmt.getGeneratedKeys();
            rs.next();
            int orderId = rs.getInt(1);

            // Insert order items into order_items table
            PreparedStatement itemStmt = conn.prepareStatement(itemSql);
            for (Map.Entry<Integer, Integer> entry : cartItems.entrySet()) {
                itemStmt.setInt(1, orderId);
                itemStmt.setInt(2, entry.getKey());
                itemStmt.setInt(3, entry.getValue());
                itemStmt.executeUpdate();
            }

            // Update stock quantities in medicines table
            PreparedStatement stockStmt = conn.prepareStatement(stockSql);
            for (Map.Entry<Integer, Integer> entry : cartItems.entrySet()) {
                stockStmt.setInt(1, entry.getValue());
                stockStmt.setInt(2, entry.getKey());
                stockStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
