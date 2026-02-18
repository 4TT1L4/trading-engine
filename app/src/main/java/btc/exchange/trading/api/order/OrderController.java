package btc.exchange.trading.api.order;

import btc.exchange.trading.api.order.dto.CreateOrderRequest;
import btc.exchange.trading.api.order.dto.OrderResponse;
import btc.exchange.trading.application.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Orders")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        var o = orderService.createOrder(req.accountId(), req.priceLimitUsdPerBtc(), req.amountBtc());
        return new OrderResponse(o.id().value(), o.accountId().value(), o.priceLimitUsdPerBtc(), o.amountBtc(), o.status());
    }

    @GetMapping("/{orderId}")
    public OrderResponse getById(@PathVariable String orderId) {
        var o = orderService.getOrderById(orderId);
        return new OrderResponse(o.id().value(), o.accountId().value(), o.priceLimitUsdPerBtc(), o.amountBtc(), o.status());
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(name = "status", required = false) String status) {
        var orders = (status == null) ? orderService.listOrders()
                : orderService.listOrdersByStatus(status);

        return orders.stream()
                .map(o -> new OrderResponse(o.id().value(), o.accountId().value(), o.priceLimitUsdPerBtc(), o.amountBtc(), o.status()))
                .toList();
    }
}
