package llc.devlab.app.model.dto;

import llc.devlab.app.model.enums.AccountRole;

public record AccountSummaryDto(
        Long id,
        String email,
        String displayName,
        AccountRole role
) {
}
