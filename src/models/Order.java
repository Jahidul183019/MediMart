package models;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private List<OrderItem> items;

    // Constructor
    public Order() {
        this.items = new ArrayList<>();
    }

    // Add item to the cart
    public void addToCart(Medicine medicine, int quantity) {
        OrderItem orderItem = new OrderItem(medicine, quantity);
        items.add(orderItem);
    }

    // Get all items in the cart
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
}
