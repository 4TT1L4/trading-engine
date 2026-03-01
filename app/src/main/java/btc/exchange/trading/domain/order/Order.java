package btc.exchange.trading.domain.order;

import btc.exchange.trading.domain.account.AccountId;
import java.math.BigDecimal;

/**
 * BUY-only limit order: buy BTC, sell USD. USD is locked at creation and released as BTC on fill.
 */
public record Order(
    OrderId id,
    AccountId accountId,
    BigDecimal priceLimitUsdPerBtc,
    BigDecimal amountBtc,
    BigDecimal lockedUsd,
    OrderStatus status) {
  public Order withStatus(OrderStatus newStatus) {
    return new Order(id, accountId, priceLimitUsdPerBtc, amountBtc, lockedUsd, newStatus);
  }
}
