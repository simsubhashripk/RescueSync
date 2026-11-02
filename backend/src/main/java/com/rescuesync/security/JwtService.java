package com.rescuesync.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(
            String username,
            Map<String, Object> extraClaims) {

        return Jwts.builder()
                .claims(extraClaims != null ? extraClaims : new HashMap<>())
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());

        return generateToken(userDetails.getUsername(), claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.getSubject());
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver) {

        Claims claims = extractAllClaims(token);

        if (claims == null) {
            return null;
        }

        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        String username = extractUsername(token);

        return username != null
                && username.equalsIgnoreCase(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {

        Date expiration = extractClaim(
                token,
                claims -> claims.getExpiration()
        );

        return expiration != null && expiration.before(new Date());
    }
}