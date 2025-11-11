package org.example.store.utils;

import org.example.store.model.Role;
import org.example.store.model.User;

public class Session {
    private static User currentUser;

    public static void setUser(User user) {
        currentUser = user;
    }

    public static User getUser() {
        return currentUser;
    }

    public static Role getRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }
}
