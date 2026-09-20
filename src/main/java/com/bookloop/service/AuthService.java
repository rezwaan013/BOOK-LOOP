package com.bookloop.service;

import com.bookloop.dao.UserDAO;
import com.bookloop.model.User;
import com.bookloop.util.PasswordUtil;
import com.bookloop.util.SessionManager;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Business logic for user registration and login.
 * No SQL lives here — delegates to UserDAO.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Registers a new user, hashes their password, and starts a session.
     *
     * @throws IllegalArgumentException if the email is already taken
     */
    public User register(String fullName, String phone, String email, String password)
            throws SQLException {
        if (userDAO.findByEmail(email).isPresent())
            throw new IllegalArgumentException("An account with this email already exists.");
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash(password, salt);
        User user = new User(fullName, phone, email, hash, salt);
        userDAO.save(user);
        SessionManager.setCurrentUser(user);
        return user;
    }

    /**
     * Validates credentials and starts a session if correct.
     *
     * @throws IllegalArgumentException if email not found or password incorrect
     */
    public User login(String email, String password) throws SQLException {
        Optional<User> opt = userDAO.findByEmail(email);
        if (opt.isEmpty())
            throw new IllegalArgumentException("No account found with that email.");
        User user = opt.get();
        if (!PasswordUtil.verify(password, user.getSalt(), user.getPasswordHash()))
            throw new IllegalArgumentException("Incorrect password.");
        SessionManager.setCurrentUser(user);
        return user;
    }

    /** Clears the current session. */
    public void logout() { SessionManager.clear(); }
}
