package com.naji.security.jwt;

import com.naji.exception.exceptions.ResourceNotFoundException;
import com.naji.player.Player;
import com.naji.player.PlayerRepository;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestHeader;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class JWTUtils {
    private static final Logger logger = LoggerFactory.getLogger(JWTUtils.class);

    @Autowired
    private PlayerRepository playerRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expirationMs}")
    private int jwtExpirationMs;


    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public String getTokenFromHeader(@RequestHeader("Authorization") String authHeader) {
        logger.debug("Authorization Header: {}", authHeader);
        if (Objects.nonNull(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        Player player = playerRepository.findByUserName(username)
                .orElseThrow(() -> new ResourceNotFoundException("No player found with the provided username"));

        Long playerId = player.getId();
        String playerRole = player.getRole();
        claims.put("playerId", playerId);
        claims.put("role", playerRole);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException e) {
            logger.error("JWT Exception: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("An error occurred while getting username from token: {}", e.getMessage());
        }
        return null;
    }

    public String getPasswordFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("password", String.class);
        } catch (JwtException e) {
            logger.error("JWT Exception: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("An error occurred while getting password from token: {}", e.getMessage());
        }
        return null;
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public Long getPlayerIdFromToken(String token) {
        return ((Number) getUserDetailsFromToken(token).get("playerId")).longValue();
    }

    public Map<String, Object> getUserDetailsFromToken(String token) {
        try {
            Map<String, Object> claims = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            logger.info("Parsed claims from token: {}", claims); // Log the claims for debugging
            return claims;
        } catch (JwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        }
        return new HashMap<>();
    }


    public boolean validateJwtToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public boolean isAuthorizedToken(String token) {
        return validateJwtToken(token);
    }
}
