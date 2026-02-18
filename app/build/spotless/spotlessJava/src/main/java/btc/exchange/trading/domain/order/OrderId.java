package btc.exchange.trading.domain.order;

import java.util.UUID;

public record OrderId(String value) {
  public static OrderId newId() {
    return new OrderId(UUID.randomUUID().toString());
  }
}
