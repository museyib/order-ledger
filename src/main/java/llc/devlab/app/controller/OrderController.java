package llc.devlab.app.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import llc.devlab.app.model.dto.CreateOrderRequest;
import llc.devlab.app.model.dto.OrderDto;
import llc.devlab.app.model.dto.OrderStatusHistoryDto;
import llc.devlab.app.model.dto.OrderSummaryDto;
import llc.devlab.app.model.dto.PageResponse;
import llc.devlab.app.model.dto.TransitionOrderRequest;
import llc.devlab.app.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    ResponseEntity<OrderDto> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @GetMapping("/{orderId}")
    OrderDto getById(@PathVariable long orderId) {
        return orderService.getById(orderId);
    }

    @GetMapping
    PageResponse<OrderSummaryDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort
    ) {
        return orderService.list(page, size, sort);
    }

    @GetMapping("/{orderId}/history")
    List<OrderStatusHistoryDto> history(@PathVariable long orderId) {
        return orderService.getHistory(orderId);
    }

    @PostMapping("/{orderId}/transitions")
    OrderDto transition(@PathVariable long orderId, @Valid @RequestBody TransitionOrderRequest request) {
        return orderService.transition(orderId, request);
    }
}
