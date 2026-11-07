package llc.devlab.app.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import lombok.RequiredArgsConstructor;
import llc.devlab.app.exception.UnauthorizedException;
import llc.devlab.app.model.entity.Account;
import llc.devlab.app.model.entity.RefreshToken;
import llc.devlab.app.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public RefreshTokenEnvelope issue(Account account) {
        String rawToken = generateToken();
        Instant expiresAt = Instant.now().plusSeconds(jwtProperties.refreshTokenExpirationDays() * 24 * 60 * 60);

        RefreshToken refreshToken = RefreshToken.builder()
                .account(account)
                .tokenHash(hash(rawToken))
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
        return new RefreshTokenEnvelope(rawToken, expiresAt);
    }

    @Transactional
    public Account rotate(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid."));

        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired or revoked.");
        }

        refreshToken.setRevokedAt(Instant.now());
        return refreshToken.getAccount();
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte element : hash) {
                builder.append(String.format("%02x", element));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required.", exception);
        }
    }
}
