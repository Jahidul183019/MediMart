package models;

import javafx.beans.property.*;

public class OrderHistoryRow {

    private final IntegerProperty orderId = new SimpleIntegerProperty();
    private final StringProperty orderDate = new SimpleStringProperty();
    private final StringProperty medicineName = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final DoubleProperty totalPrice = new SimpleDoubleProperty();

    public OrderHistoryRow(int orderId, String orderDate,
                           String medicineName, int quantity, double totalPrice) {
        this.orderId.set(orderId);
        this.orderDate.set(orderDate);
        this.medicineName.set(medicineName);
        this.quantity.set(quantity);
        this.totalPrice.set(totalPrice);
    }

    // --- Getters & Properties ---
    public int getOrderId() { return orderId.get(); }
    public IntegerProperty orderIdProperty() { return orderId; }

    public String getOrderDate() { return orderDate.get(); }
    public StringProperty orderDateProperty() { return orderDate; }

    public String getMedicineName() { return medicineName.get(); }
    public StringProperty medicineNameProperty() { return medicineName; }

    public int getQuantity() { return quantity.get(); }
    public IntegerProperty quantityProperty() { return quantity; }

    public double getTotalPrice() { return totalPrice.get(); }
    public DoubleProperty totalPriceProperty() { return totalPrice; }

    // --- OPTIONAL setters (helpful for future UI refresh) ---
    public void setOrderId(int id) { this.orderId.set(id); }
    public void setOrderDate(String date) { this.orderDate.set(date); }
    public void setMedicineName(String name) { this.medicineName.set(name); }
    public void setQuantity(int qty) { this.quantity.set(qty); }
    public void setTotalPrice(double price) { this.totalPrice.set(price); }
}
