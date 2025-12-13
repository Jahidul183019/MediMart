package models;

import java.util.Objects;

/**
 * Represents a user in the MediMart system.
 * Supports both login (basic fields) and profile management (address, avatar, updatedAt).
 */
public class User {
    private int id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String password;
    private String address;      
    private String avatarPath;   
    private long updatedAt;     

    /* =============================
       Constructors
       ============================= */

    // Full constructor (for full profile use)
    public User(int id, String firstName, String lastName, String phone, String email,
                String password, String address, String avatarPath, long updatedAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.address = address;
        this.avatarPath = avatarPath;
        this.updatedAt = updatedAt;
    }

    // Constructor used for signup/login (backward-compatible)
    public User(int id, String firstName, String lastName, String phone, String email, String password) {
        this(id, firstName, lastName, phone, email, password, null, null, 0L);
    }

    // Minimal constructor (for quick login case)
    public User(int id, String firstName, String email, String password) {
        this(id, firstName, "", "", email, password, null, null, 0L);
    }

    // Empty constructor (for frameworks/ORMs if needed)
    public User() {}

    /* =============================
       Getters & Setters
       ============================= */

    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getAddress() { return address; }
    public String getAvatarPath() { return avatarPath; }
    public long getUpdatedAt() { return updatedAt; }

    public void setId(int id) { this.id = id; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setAddress(String address) { this.address = address; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    /* =============================
       Utility methods
       ============================= */

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", avatarPath='" + avatarPath + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }
}
