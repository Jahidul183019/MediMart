package models;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Map;

public class Cart {

    private final ObservableList<OrderItem> items;  // List of items in the cart
    private double deliveryAmount;  // Delivery cost
    private double taxAmount;       // Tax
    private double serviceFeeAmount; // Service fee
    private double discount;         // Discount applied to cart

    // Constructor
    public Cart() {
        this.items = FXCollections.observableArrayList();
        this.deliveryAmount = 0;
        this.taxAmount = 0;
        this.serviceFeeAmount = 0;
        this.discount = 0;
    }

    // Add an item to the cart
    public void addItem(Medicine medicine, int quantity) {
        for (OrderItem item : items) {
            if (item.getMedicine().getId() == medicine.getId()) {
                item.setQuantity(item.getQuantity() + quantity);  // Increase quantity if already in cart
                return;
            }
        }
        items.add(new OrderItem(medicine, quantity));  // Add new item to cart
    }

    // Remove an item from the cart
    public void removeItem(OrderItem item) {
        items.remove(item);
    }

    // Get all items in the cart
    public ObservableList<OrderItem> getItems() {
        return items;
    }

    // Calculate the total price of the cart (sum of all items' total price)
    public double getTotalPrice() {
        return items.stream().mapToDouble(OrderItem::getTotalPrice).sum();
    }

    // Apply discount to the total price
    public void applyDiscount(double discount) {
        this.discount = discount;
    }

    // Apply tax to the total price
    public void applyTax(double tax) {
        this.taxAmount = tax;
    }

    // Apply service fee to the total price
    public void applyServiceFee(double serviceFee) {
        this.serviceFeeAmount = serviceFee;
    }

    // Getters and setters for deliveryAmount, taxAmount, serviceFeeAmount, and discount
    public double getDeliveryAmount() {
        return deliveryAmount;
    }

    public void setDeliveryAmount(double deliveryAmount) {
        this.deliveryAmount = deliveryAmount;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public double getServiceFeeAmount() {
        return serviceFeeAmount;
    }

    public void setServiceFeeAmount(double serviceFeeAmount) {
        this.serviceFeeAmount = serviceFeeAmount;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    // Method to return the cart history as a Map of medicineId -> quantity
    public Map<Integer, Integer> getBuyHistory() {
        // Implement this method if you need to access cart items as a Map
        return null;
    }
}
