package btc.exchange.trading.api.account.dto;

import btc.exchange.trading.api.order.dto.OrderResponse;

import java.math.BigDecimal;
import java.util.List;

public record AccountDetailsResponse(
        String id,
        String name,
        BigDecimal usdBalance,
        BigDecimal btcBalance,
        List<OrderResponse> orders
) {}