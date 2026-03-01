package btc.exchange.trading.api.order;

import btc.exchange.trading.api.order.dto.CreateOrderRequest;
import btc.exchange.trading.api.order.dto.OrderResponse;
import btc.exchange.trading.application.order.OrderService;
import btc.exchange.trading.domain.order.Order;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    return toResponse(
        orderService.createOrder(req.accountId(), req.priceLimitUsdPerBtc(), req.amountBtc()));
  }

  @GetMapping("/{orderId}")
  public OrderResponse getById(@PathVariable String orderId) {
    return toResponse(orderService.getOrderById(orderId));
  }

  @GetMapping
  public List<OrderResponse> list(@RequestParam(name = "status", required = false) String status) {
    var orders =
        Optional.ofNullable(status)
            .filter(s -> !s.isBlank())
            .map(orderService::listOrdersByStatus)
            .orElseGet(orderService::listOrders);

    return orders.stream().map(this::toResponse).toList();
  }

  private OrderResponse toResponse(Order o) {
    return new OrderResponse(
        o.id().value(), o.accountId().value(), o.priceLimitUsdPerBtc(), o.amountBtc(), o.status());
  }
}
