package com.rescuesync.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for legacy 'admin_users' table.
 */
@Entity
@Table(name = "admin_users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password", nullable = false, length = 255)
    @JsonIgnore
    private String password;

    @NotBlank
    @Size(max = 100)
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "role", nullable = false, length = 50)
    private String role = "ADMINISTRATOR";

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    public AdminUser() {
    }

    public AdminUser(String username, String password, String fullName, String role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    // Getters and Setters
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

