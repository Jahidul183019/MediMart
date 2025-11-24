package utils;

import models.User;
import java.util.concurrent.atomic.AtomicReference;

public final class Session {
    private Session() {}

    private static final AtomicReference<User> CURRENT = new AtomicReference<>();

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

    // Logout and clear the session
    public static void logout() {
        CURRENT.set(null);
    }
}
