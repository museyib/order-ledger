package llc.devlab.app.service;

import llc.devlab.app.model.entity.PurchaseOrder;
import llc.devlab.app.model.enums.OrderStatus;

public interface OrderHistoryRecorder {

    void append(PurchaseOrder order, OrderStatus previousStatus, OrderStatus newStatus, String actorEmail, String reason);
}
