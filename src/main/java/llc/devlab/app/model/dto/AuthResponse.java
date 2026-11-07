package llc.devlab.app.model.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        AccountSummaryDto account
) {
}
