package com.bookloop.util;

import com.bookloop.model.User;

/**
 * In-memory session holder for the currently logged-in user.
 * Cleared on logout or application restart.
 */
public final class SessionManager {

    private static User currentUser;

    private SessionManager() {}

    public static User getCurrentUser()               { return currentUser; }
    public static void  setCurrentUser(User user)     { currentUser = user; }
    public static boolean isLoggedIn()                { return currentUser != null; }
    public static void  clear()                       { currentUser = null; }
}
