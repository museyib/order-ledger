package llc.devlab.app.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import llc.devlab.app.model.entity.Account;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenEnvelope generateAccessToken(Account account) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(jwtProperties.accessTokenExpirationMinutes() * 60);

        String token = Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(account.getEmail())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claims(Map.of(
                        "type", "access",
                        "accountId", account.getId(),
                        "role", account.getRole().name(),
                        "displayName", account.getDisplayName()
                ))
                .signWith(signingKey)
                .compact();

        return new TokenEnvelope(token, expiresAt);
    }

    public Claims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!"access".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Unsupported token type.");
        }

        return claims;
    }
}
