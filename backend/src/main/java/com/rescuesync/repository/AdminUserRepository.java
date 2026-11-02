package com.rescuesync.repository;

import com.rescuesync.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Integer> {
    Optional<AdminUser> findByUsernameIgnoreCase(String username);
}

