package com.rescuesync.controller;

import com.rescuesync.dto.AuthResponse;
import com.rescuesync.dto.LoginRequest;
import com.rescuesync.dto.MessageResponse;
import com.rescuesync.dto.RegisterRequest;
import com.rescuesync.dto.UserDto;
import com.rescuesync.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user registration, authentication, and session identity.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * User Registration endpoint.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse(false, "Registration failed: " + e.getMessage()));
        }
    }

    /**
     * User Login endpoint.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse(false, "Invalid username or password."));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse(false, "Authentication error: " + e.getMessage()));
        }
    }

    /**
     * Returns current user profile based on validated JWT token.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse(false, "Not authenticated"));
        }

        String username = authentication.getName();
        UserDto userDto = authService.getCurrentUser(username);
        if (userDto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(false, "User not found"));
        }

        return ResponseEntity.ok(userDto);
    }
}

