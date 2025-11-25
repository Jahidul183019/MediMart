package utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.OrderItem;
import models.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class Session {
    private Session() {}

    private static final AtomicReference<User> CURRENT = new AtomicReference<>();

    // ==========================
    //   PER-USER CART STORAGE
    // ==========================

    // All carts keyed by userId
    private static final Map<Integer, ObservableList<OrderItem>> USER_CARTS =
            new ConcurrentHashMap<>();

    // Guest cart (when no one is logged in)
    private static final ObservableList<OrderItem> GUEST_CART =
            FXCollections.observableArrayList();

    /**
     * Get cart for current user.
     * - If logged in → cart is tied to userId and reused whenever they come back.
     * - If not logged in → use a single guest cart.
     */
    public static ObservableList<OrderItem> getCart() {
        User user = CURRENT.get();
        if (user == null) {
            return GUEST_CART;
        }
        int userId = user.getId();
        return USER_CARTS.computeIfAbsent(userId, id -> FXCollections.observableArrayList());
    }

    /**
     * Clear only the current user's cart.
     * Call this when the user presses "Clear cart" or removes items manually.
     */
    public static void clearCurrentCart() {
        User user = CURRENT.get();
        if (user == null) {
            GUEST_CART.clear();
            return;
        }
        ObservableList<OrderItem> cart = USER_CARTS.get(user.getId());
        if (cart != null) {
            cart.clear();
        }
    }

    // ==========================
    //   EXISTING SESSION LOGIC
    // ==========================

    // Get the current logged-in user
    public static User getCurrentUser() {
        return CURRENT.get();
    }

    // Set the current logged-in user
    public static void setCurrentUser(User u) {
        CURRENT.set(u);
    }

    // Update current user with a new User object
    public static void updateCurrentUser(User updated) {
        if (updated != null) CURRENT.set(updated);
    }

    // Check if the user is logged in
    public static boolean isLoggedIn() {
        return CURRENT.get() != null;
    }

    // Get the current user's ID, if logged in
    public static int getCurrentUserId() {
        User user = CURRENT.get();
        if (user == null) {
            throw new IllegalStateException("No user is currently logged in.");
        }
        return user.getId();
    }

    // Set current user ID (called after login)
    public static void setCurrentUserId(int userId) {
        User currentUser = CURRENT.get();
        if (currentUser != null) {
            currentUser.setId(userId);  // Update the user ID
        }
    }

    // Logout: do NOT delete the user's stored cart
    // Cart stays mapped to userId until items are removed from cart UI.
    public static void logout() {
        CURRENT.set(null);
        // Optional: clear guest cart when logging out
        GUEST_CART.clear();
    }
}
