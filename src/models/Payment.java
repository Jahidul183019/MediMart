package models;

public class Payment {
    private final int id;  // Unique payment ID
    private final double amount;  // Total amount paid
    private final boolean success;  // Payment success flag
    private final String timestamp;  // Timestamp of payment

    // Constructor
    public Payment(int id, double amount, boolean success, String timestamp) {
        this.id = id;
        this.amount = amount;
        this.success = success;
        this.timestamp = timestamp;
    }

    // Getters
    public int getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
