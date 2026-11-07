package llc.devlab.app.security;

import java.time.Instant;

public record RefreshTokenEnvelope(
        String token,
        Instant expiresAt
) {
}
