package btc.exchange.trading.infrastructure.persistence.order;

import btc.exchange.trading.domain.account.AccountId;
import btc.exchange.trading.domain.order.Order;
import btc.exchange.trading.domain.order.OrderId;
import btc.exchange.trading.domain.order.OrderStatus;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOrderRepository implements OrderRepository {

  private final ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();

  @Override
  public Order save(Order order) {
    orders.put(order.id().value(), order);
    return order;
  }

  @Override
  public Optional<Order> findById(OrderId id) {
    return Optional.ofNullable(orders.get(id.value()));
  }

  @Override
  public List<Order> findAll() {
    return new LinkedList<>(orders.values());
  }

  @Override
  public List<Order> findByStatus(OrderStatus status) {
    return orders.values().stream().filter(o -> o.status() == status).toList();
  }

  @Override
  public Optional<Order> update(OrderId id, UnaryOperator<Order> updater) {
    var updated = orders.computeIfPresent(id.value(), (k, v) -> updater.apply(v));
    return Optional.ofNullable(updated);
  }

  @Override
  public List<Order> findByAccountId(AccountId accountId) {
    return orders.values().stream().filter(o -> o.accountId().equals(accountId)).toList();
  }
}
