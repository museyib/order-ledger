package llc.devlab.app.model.dto;

import java.time.Instant;

import llc.devlab.app.model.enums.OrderStatus;

public record OrderStatusHistoryDto(
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant transitionAt,
        String actorEmail,
        String reason
) {
}
