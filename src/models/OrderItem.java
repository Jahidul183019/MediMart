package models;

import javafx.beans.property.*;

public class OrderItem {

    private ObjectProperty<Medicine> medicineProperty;  // Medicine property
    private IntegerProperty quantity;  // Quantity of the medicine
    private DoubleProperty totalPrice;  // Total price for this order item (quantity * price)

    // Constructor
    public OrderItem(Medicine medicine, int quantity) {
        this.medicineProperty = new SimpleObjectProperty<>(medicine);  // Initialize medicineProperty
        this.quantity = new SimpleIntegerProperty(quantity);
        this.totalPrice = new SimpleDoubleProperty();  // Initialize totalPrice

        // Recalculate total price when medicine or quantity changes
        updateTotalPrice();

        // Add listeners to update total price if either medicine or quantity changes
        this.medicineProperty.addListener((observable, oldValue, newValue) -> updateTotalPrice());
        this.quantity.addListener((observable, oldValue, newValue) -> updateTotalPrice());
    }

    // Getter for the medicine
    public Medicine getMedicine() {
        return medicineProperty.get();
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
    }

    // Getter for total price
    public double getTotalPrice() {
        return totalPrice.get();
    }

    public DoubleProperty totalPriceProperty() {
        return totalPrice;
    }

    // Method to update total price when quantity or medicine changes
    public void updateTotalPrice() {
        Medicine medicine = getMedicine();
        if (medicine != null && medicine.getId() != 0) {
            totalPrice.set(medicine.getPrice() * quantity.get());
        } else {
            totalPrice.set(0.0);  // Default to 0 if no medicine is set
        }
    }

    // Expose medicineProperty for binding in CartView
    public ObjectProperty<Medicine> medicineProperty() {
        return medicineProperty;
    }

    // Set the medicine and trigger price recalculation
    public void setMedicine(Medicine medicine) {
        this.medicineProperty.set(medicine);
        updateTotalPrice();
    }

    // Method to retrieve the medicineId, which is important for database operations
    public int getMedicineId() {
        Medicine medicine = getMedicine();
        return (medicine != null) ? medicine.getId() : 0;  // Ensure that we return a valid medicineId
    }
}
