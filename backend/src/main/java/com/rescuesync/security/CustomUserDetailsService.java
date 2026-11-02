package com.rescuesync.security;

import com.rescuesync.entity.AdminUser;
import com.rescuesync.entity.User;
import com.rescuesync.repository.AdminUserRepository;
import com.rescuesync.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Custom UserDetailsService loading user credentials from MySQL rescuesync_db.
 * Checks primary 'users' table, with transparent fallback to 'admin_users'.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;

    public CustomUserDetailsService(UserRepository userRepository, AdminUserRepository adminUserRepository) {
        this.userRepository = userRepository;
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username cannot be empty");
        }

        // 1. Primary lookup in 'users' table
        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username.trim());
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            boolean active = "ACTIVE".equalsIgnoreCase(u.getAccountStatus());
            List<GrantedAuthority> authorities = mapRoleToAuthorities(u.getRole());

            return org.springframework.security.core.userdetails.User.builder()
                    .username(u.getUsername())
                    .password(u.getPasswordHash())
                    .disabled(!active)
                    .accountExpired(false)
                    .credentialsExpired(false)
                    .accountLocked(!active)
                    .authorities(authorities)
                    .build();
        }

        // 2. Fallback lookup in legacy 'admin_users' table
        Optional<AdminUser> adminOpt = adminUserRepository.findByUsernameIgnoreCase(username.trim());
        if (adminOpt.isPresent()) {
            AdminUser a = adminOpt.get();
            List<GrantedAuthority> authorities = mapRoleToAuthorities(a.getRole());

            return org.springframework.security.core.userdetails.User.builder()
                    .username(a.getUsername())
                    .password(a.getPassword())
                    .disabled(false)
                    .authorities(authorities)
                    .build();
        }

        throw new UsernameNotFoundException("User not found with username: " + username);
    }

    private List<GrantedAuthority> mapRoleToAuthorities(String rawRole) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        String upperRole = rawRole != null ? rawRole.trim().toUpperCase() : "USER";

        if ("ADMINISTRATOR".equals(upperRole) || "ADMIN".equals(upperRole)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return authorities;
    }
}

