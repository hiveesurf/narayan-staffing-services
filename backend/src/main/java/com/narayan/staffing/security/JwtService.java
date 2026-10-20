package com.narayan.staffing.security;

import com.narayan.staffing.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtService {

    private final SecretKey key;
    private final long accessTtl;

    public JwtService(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(normalize(properties.getJwt().getSecret()));
        this.accessTtl = properties.getJwt().getAccessExpiration();
    }

    public String accessToken(StaffUserPrincipal principal) {
        Instant now = Instant.now();
        List<String> roles = principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .toList();
        List<String> perms = principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();
        return Jwts.builder()
                .subject(principal.getEmail())
                .claim("uid", principal.getId().toString())
                .claim("name", principal.getFullName())
                .claim("roles", roles)
                .claim("perms", perms)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTtl)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public UUID userId(Claims claims) {
        return UUID.fromString(claims.get("uid", String.class));
    }

    private static byte[] normalize(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is required");
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return raw;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
