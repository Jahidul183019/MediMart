package models;

import javafx.beans.property.*;

public class OrderItem {
    private Medicine medicine;  // Medicine object
    private ObjectProperty<Medicine> medicineProperty;  // Medicine property
    private IntegerProperty quantity;  // Quantity of the medicine
    private DoubleProperty totalPrice;  // Total price for this order item (quantity * price)

    public OrderItem(Medicine medicine, int quantity) {
        this.medicine = medicine;
        this.medicineProperty = new SimpleObjectProperty<>(medicine);  // Initialize medicineProperty
        this.quantity = new SimpleIntegerProperty(quantity);
        this.totalPrice = new SimpleDoubleProperty(quantity * medicine.getPrice());  // Calculate total price
    }

    // Getter for the medicine
    public Medicine getMedicine() {
        return medicine;
    }

    // Getter and setter for quantity
    public int getQuantity() {
        return quantity.get();
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity.set(quantity);
        updateTotalPrice();  // Recalculate total price when quantity changes
    }

    // Getter for total price
    public double getTotalPrice() {
        return totalPrice.get();
    }

    public DoubleProperty totalPriceProperty() {
        return totalPrice;
    }

    // Method to update total price when quantity changes
    private void updateTotalPrice() {
        totalPrice.set(medicine.getPrice() * quantity.get());
    }

    // Expose medicineProperty for binding in CartView
    public ObjectProperty<Medicine> medicineProperty() {
        return medicineProperty;
    }
}
