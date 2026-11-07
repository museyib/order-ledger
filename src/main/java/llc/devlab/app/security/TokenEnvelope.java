package llc.devlab.app.security;

import java.time.Instant;

public record TokenEnvelope(
        String token,
        Instant expiresAt
) {
}
