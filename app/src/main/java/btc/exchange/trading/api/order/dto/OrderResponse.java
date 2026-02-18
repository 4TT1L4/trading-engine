package btc.exchange.trading.api.order.dto;

import btc.exchange.trading.domain.order.OrderStatus;

import java.math.BigDecimal;

public record OrderResponse(
        String id,
        String accountId,
        BigDecimal priceLimitUsdPerBtc,
        BigDecimal amountBtc,
        OrderStatus status
) {}
