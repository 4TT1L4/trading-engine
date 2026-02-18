package btc.exchange.trading.domain.order;

import btc.exchange.trading.domain.account.AccountId;

import java.math.BigDecimal;

/**
 * BUY-only limit order: buy BTC, sell USD.
 */
public record Order(
        OrderId id,
        AccountId accountId,
        BigDecimal priceLimitUsdPerBtc,
        BigDecimal amountBtc,
        OrderStatus status
) {
    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, accountId, priceLimitUsdPerBtc, amountBtc, newStatus);
    }
}
