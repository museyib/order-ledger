package llc.devlab.app.mapper;

import llc.devlab.app.model.dto.OrderDto;
import llc.devlab.app.model.dto.OrderLineDto;
import llc.devlab.app.model.dto.OrderStatusHistoryDto;
import llc.devlab.app.model.dto.OrderSummaryDto;
import llc.devlab.app.model.entity.OrderLine;
import llc.devlab.app.model.entity.OrderStatusHistory;
import llc.devlab.app.model.entity.PurchaseOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface OrderMapper {

    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    OrderDto toDto(PurchaseOrder order);

    OrderSummaryDto toSummary(PurchaseOrder order);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "productSkuSnapshot")
    @Mapping(target = "productName", source = "productNameSnapshot")
    @Mapping(target = "unitPrice", source = "unitPriceSnapshot")
    OrderLineDto toDto(OrderLine line);

    OrderStatusHistoryDto toDto(OrderStatusHistory history);
}
