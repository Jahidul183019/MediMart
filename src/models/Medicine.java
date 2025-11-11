package models;

import javafx.beans.property.*;

public class Medicine {
    private final IntegerProperty id;
    private final StringProperty  name;
    private final StringProperty  category;
    private final DoubleProperty  price;
    private final IntegerProperty quantity;
    private final StringProperty  expiryDate;

    // NEW: optional local or absolute path to an image file
    private final StringProperty  imagePath;

    // ---- Existing constructor (kept for compatibility) ----
    public Medicine(int id, String name, String category, double price, int quantity, String expiryDate) {
        this(id, name, category, price, quantity, expiryDate, null);
    }

    // ---- New constructor with imagePath ----
    public Medicine(int id, String name, String category, double price, int quantity,
                    String expiryDate, String imagePath) {
        this.id         = new SimpleIntegerProperty(id);
        this.name       = new SimpleStringProperty(name);
        this.category   = new SimpleStringProperty(category);
        this.price      = new SimpleDoubleProperty(price);
        this.quantity   = new SimpleIntegerProperty(quantity);
        this.expiryDate = new SimpleStringProperty(expiryDate);
        this.imagePath  = new SimpleStringProperty(imagePath); // may be null
    }

    // --- Getters ---
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getCategory() { return category.get(); }
    public double getPrice() { return price.get(); }
    public int getQuantity() { return quantity.get(); }
    public String getExpiryDate() { return expiryDate.get(); }
    /** NEW */ public String getImagePath() { return imagePath.get(); }

    // --- Setters ---
    public void setName(String name) { this.name.set(name); }
    public void setCategory(String category) { this.category.set(category); }
    public void setPrice(double price) { this.price.set(price); }
    public void setQuantity(int quantity) { this.quantity.set(quantity); }
    public void setExpiryDate(String expiryDate) { this.expiryDate.set(expiryDate); }
    /** NEW */ public void setImagePath(String imagePath) { this.imagePath.set(imagePath); }

    // --- Property Getters ---
    public IntegerProperty idProperty() { return id; }
    public StringProperty  nameProperty() { return name; }
    public StringProperty  categoryProperty() { return category; }
    public DoubleProperty  priceProperty() { return price; }
    public IntegerProperty quantityProperty() { return quantity; }
    public StringProperty  expiryDateProperty() { return expiryDate; }
    /** NEW */ public StringProperty  imagePathProperty() { return imagePath; }

    @Override
    public String toString() {
        String img = (getImagePath() == null || getImagePath().isBlank()) ? "no-image" : getImagePath();
        return name.get() + " | " + category.get() + " | $" + price.get()
                + " | Qty: " + quantity.get() + " | Exp: " + expiryDate.get()
                + " | Img: " + img;
    }
}
