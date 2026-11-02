package com.rescuesync.service;

import com.rescuesync.dto.AuthResponse;
import com.rescuesync.dto.LoginRequest;
import com.rescuesync.dto.RegisterRequest;
import com.rescuesync.dto.UserDto;
import com.rescuesync.entity.AdminUser;
import com.rescuesync.entity.User;
import com.rescuesync.repository.AdminUserRepository;
import com.rescuesync.repository.UserRepository;
import com.rescuesync.security.JwtService;
import com.rescuesync.security.RescueSyncPasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service orchestrating user registration, authentication, and session handling.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final RescueSyncPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       AdminUserRepository adminUserRepository,
                       RescueSyncPasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new standard user in rescuesync_db.
     * Enforces duplicate checks, secure PBKDF2 password hashing, and role='USER'.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Mandatory field validations
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        String fullName = request.getFullName() != null ? request.getFullName().trim() : username;

        // 2. Duplicate username check (against users and admin_users)
        if (userRepository.existsByUsernameIgnoreCase(username) ||
                adminUserRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken. Please choose another.");
        }

        // 3. Duplicate email check
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered.");
        }

        // 4. Secure PBKDF2 Password Hashing
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 5. Build entity: Strictly force role to 'USER' and account_status to 'ACTIVE'
        // Never allow client to request ADMIN privilege
        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(hashedPassword);
        newUser.setRole("USER");
        newUser.setAccountStatus("ACTIVE");

        User savedUser = userRepository.save(newUser);

        // 6. Generate signed JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", savedUser.getUserId());
        claims.put("role", savedUser.getRole());
        claims.put("fullName", savedUser.getFullName());
        claims.put("email", savedUser.getEmail());

        String token = jwtService.generateToken(savedUser.getUsername(), claims);

        return new AuthResponse(token, UserDto.fromEntity(savedUser));
    }

    /**
     * Authenticates an existing user or admin, verifying PBKDF2 or legacy hashes securely.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String password = request.getPassword() != null ? request.getPassword() : "";

        if (username.isEmpty() || password.isEmpty()) {
            throw new BadCredentialsException("Username and password must not be empty.");
        }

        // 1. Primary authentication against 'users' table
        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Reject inactive accounts
            if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
                throw new DisabledException("Account is inactive or disabled. Please contact an administrator.");
            }

            // Verify password using PBKDF2
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new BadCredentialsException("Invalid username or password.");
            }

            // Generate JWT
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getUserId());
            claims.put("role", user.getRole());
            claims.put("fullName", user.getFullName());
            claims.put("email", user.getEmail());

            String token = jwtService.generateToken(user.getUsername(), claims);
            return new AuthResponse(token, UserDto.fromEntity(user));
        }

        // 2. Fallback authentication against legacy 'admin_users' table
        Optional<AdminUser> adminOpt = adminUserRepository.findByUsernameIgnoreCase(username);
        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();

            if (!passwordEncoder.matches(password, admin.getPassword())) {
                throw new BadCredentialsException("Invalid username or password.");
            }

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", admin.getUserId());
            claims.put("role", admin.getRole());
            claims.put("fullName", admin.getFullName());
            claims.put("email", "");

            String token = jwtService.generateToken(admin.getUsername(), claims);
            UserDto adminDto = new UserDto(
                    admin.getUserId(),
                    admin.getFullName(),
                    admin.getUsername(),
                    "",
                    admin.getRole(),
                    "ACTIVE"
            );
            return new AuthResponse(token, adminDto);
        }

        throw new BadCredentialsException("Invalid username or password.");
    }

    /**
     * Retrieves currently authenticated user profile.
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username.trim());
        if (userOpt.isPresent()) {
            return UserDto.fromEntity(userOpt.get());
        }

        Optional<AdminUser> adminOpt = adminUserRepository.findByUsernameIgnoreCase(username.trim());
        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();
            return new UserDto(
                    admin.getUserId(),
                    admin.getFullName(),
                    admin.getUsername(),
                    "",
                    admin.getRole(),
                    "ACTIVE"
            );
        }

        return null;
    }
}

