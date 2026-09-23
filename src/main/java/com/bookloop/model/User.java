package com.bookloop.model;

import java.time.LocalDateTime;

/**
 * Represents a registered user of BookLoop.
 * Password is never stored in plain text — only the SHA-256 hash + salt are persisted.
 */
public class User {

    private int id;
    private String fullName;
    private String phone;
    private String email;
    private String passwordHash;
    private String salt;
    private int rewardPoints = 0;
    private LocalDateTime createdAt;

    public User() {}

    public User(String fullName, String phone, String email,
                String passwordHash, String salt) {
        this.fullName     = fullName;
        this.phone        = phone;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.salt         = salt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return fullName + " <" + email + ">"; }
}
