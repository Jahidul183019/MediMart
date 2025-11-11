package utils;

import models.User;
import java.util.concurrent.atomic.AtomicReference;

public final class Session {
    private Session() {}

    private static final AtomicReference<User> CURRENT = new AtomicReference<>();

    public static User getCurrentUser() { return CURRENT.get(); }

    public static void setCurrentUser(User u) { CURRENT.set(u); }

    public static void updateCurrentUser(User updated) {
        if (updated != null) CURRENT.set(updated);
    }

    public static boolean isLoggedIn() { return CURRENT.get() != null; }

    public static int requireUserId() {
        User u = CURRENT.get();
        if (u == null) throw new IllegalStateException("No user in session");
        return u.getId();
    }

    public static void logout() { CURRENT.set(null); }
}
