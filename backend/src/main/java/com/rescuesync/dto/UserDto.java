package com.rescuesync.dto;

import com.rescuesync.entity.User;

/**
 * Public User Data Transfer Object.
 * Strictly never exposes password or sensitive internal data.
 */
public class UserDto {

    private Integer userId;
    private String fullName;
    private String username;
    private String email;
    private String role;
    private String accountStatus;

    public UserDto() {
    }

    public UserDto(Integer userId, String fullName, String username, String email, String role, String accountStatus) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.role = role;
        this.accountStatus = accountStatus;
    }

    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return new UserDto(
            user.getUserId(),
            user.getFullName(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getAccountStatus()
        );
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }
}

