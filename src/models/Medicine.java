package models;

public class Medicine {
    private int id;
    private String name;
    private String category;
    private double price;
    private int quantity;
    private String expiryDate;

    public Medicine(int id, String name, String category, double price, int quantity, String expiryDate) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getExpiryDate() { return expiryDate; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setPrice(double price) { this.price = price; }
    public void setCategory(String category) { this.category = category; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    @Override
    public String toString() {
        return name + " | " + category + " | $" + price + " | Qty: " + quantity;
    }
}
