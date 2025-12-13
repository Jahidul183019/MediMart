package models;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private int orderId;
    private List<OrderItem> items;
    private String customerName;
    private String customerAddress;
    private String customerPhone;
    private String orderStatus;

    // Constructor
    public Order() {
        this.items = new ArrayList<>();
        this.orderStatus = "Pending";  // Default status
    }

    // Add item to the order
    public void addToCart(Medicine medicine, int quantity) {
        OrderItem orderItem = new OrderItem(medicine, quantity);
        items.add(orderItem);
    }

    // Get all items in the order
    public List<OrderItem> getItems() {
        return items;
    }

    // Get the total price of the order
    public double getTotalPrice() {
        double total = 0;
        for (OrderItem item : items) {
            total += item.getTotalPrice();
        }
        return total;
    }

    // Getters and setters for order properties
    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
