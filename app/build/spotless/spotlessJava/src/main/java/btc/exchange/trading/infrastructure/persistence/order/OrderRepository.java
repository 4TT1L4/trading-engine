package btc.exchange.trading.infrastructure.persistence.order;

import btc.exchange.trading.domain.account.AccountId;
import btc.exchange.trading.domain.order.Order;
import btc.exchange.trading.domain.order.OrderId;
import btc.exchange.trading.domain.order.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface OrderRepository {
  Order save(Order order);

  Optional<Order> findById(OrderId id);

  List<Order> findAll();

  List<Order> findByStatus(OrderStatus status);

  List<Order> findByAccountId(AccountId accountId);

  Optional<Order> update(OrderId id, UnaryOperator<Order> updater);
}
