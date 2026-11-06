package com.fitnessbuddy.security;

import com.fitnessbuddy.user.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey secretKey;
    private final JwtParser parser;
    private final Duration accessTokenTtl;

    public JwtService(AppSecurityProperties properties) {
        this.secretKey = decodeKey(properties.jwtSecret());
        // A JwtParser is immutable and thread-safe: build it once, not per request.
        this.parser = Jwts.parser().verifyWith(secretKey).build();
        this.accessTokenTtl = properties.accessTokenTtl();
    }

    /**
     * The payload of a JWT is only Base64-encoded, not encrypted: anyone holding
     * the token can read it. So it carries only what the server needs on every
     * request (the user id and role) and no personal data such as the email.
     */
    public String generateJwtToken(UserEntity user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(ROLE_CLAIM, user.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Verifies the signature and expiry, then extracts the identity: one parse per
     * request. jjwt throws (and we return empty) for a bad signature, an expired
     * token, a malformed token, or a token signed with a different key.
     */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            return Optional.of(new AuthenticatedUser(
                    Integer.valueOf(claims.getSubject()),
                    claims.get(ROLE_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static SecretKey decodeKey(String base64Secret) {
        try {
            // hmacShaKeyFor rejects keys shorter than 256 bits (WeakKeyException).
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "app.security.jwt-secret (JWT_SECRET) must be a Base64-encoded key of at least "
                            + "32 random bytes. Generate one with: openssl rand -base64 32", e);
        }
    }
}
