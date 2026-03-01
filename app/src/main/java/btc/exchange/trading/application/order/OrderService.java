package btc.exchange.trading.application.order;

import btc.exchange.trading.application.account.AccountService;
import btc.exchange.trading.application.config.AppProperties;
import btc.exchange.trading.domain.account.AccountId;
import btc.exchange.trading.domain.common.DomainException;
import btc.exchange.trading.domain.order.Order;
import btc.exchange.trading.domain.order.OrderId;
import btc.exchange.trading.domain.order.OrderStatus;
import btc.exchange.trading.infrastructure.persistence.order.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService implements DisposableBean {

  private static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
  private static final String ORDER_NOT_FOUND_TEXT = "Order not found";

  private final OrderRepository orderRepository;
  private final AccountService accountService;
  private final ExecutorService fillExecutor;

  private final ConcurrentHashMap<String, Object> accountLocks = new ConcurrentHashMap<>();

  @Autowired
  public OrderService(
      OrderRepository orderRepository, AccountService accountService, AppProperties appProperties) {
    this.orderRepository = orderRepository;
    this.accountService = accountService;
    this.fillExecutor =
        Executors.newFixedThreadPool(
            Math.max(1, appProperties.fillWorkerThreads()),
            r -> {
              var t = new Thread(r, "order-fill-worker");
              t.setDaemon(false);
              return t;
            });
  }

  /** Constructor for tests: inject a custom executor. */
  public OrderService(
      OrderRepository orderRepository, AccountService accountService, ExecutorService fillExecutor) {
    this.orderRepository = orderRepository;
    this.accountService = accountService;
    this.fillExecutor = fillExecutor;
  }

  @Override
  public void destroy() {
    fillExecutor.shutdown();
    try {
      if (!fillExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
        fillExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      fillExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private Object lockFor(String accountId) {
    return accountLocks.computeIfAbsent(accountId, k -> new Object());
  }

  public Order createOrder(String accountId, BigDecimal priceLimitUsdPerBtc, BigDecimal amountBtc) {
    accountService.requireAccount(accountId);

    if (priceLimitUsdPerBtc == null || priceLimitUsdPerBtc.signum() <= 0) {
      throw new DomainException("INVALID_ORDER", "priceLimitUsdPerBtc must be > 0");
    }
    if (amountBtc == null || amountBtc.signum() <= 0) {
      throw new DomainException("INVALID_ORDER", "amountBtc must be > 0");
    }

    BigDecimal lockedUsd = priceLimitUsdPerBtc.multiply(amountBtc);
    Order order;

    synchronized (lockFor(accountId)) {
      var acc = accountService.getAccountById(accountId);
      if (acc.usdBalance().compareTo(lockedUsd) < 0) {
        throw new DomainException(
            "INSUFFICIENT_FUNDS",
            "Insufficient USD balance: required " + lockedUsd + ", available " + acc.usdBalance());
      }
      accountService.applyTrade(new AccountId(accountId), lockedUsd.negate(), BigDecimal.ZERO);
      order =
          new Order(
              OrderId.newId(),
              new AccountId(accountId),
              priceLimitUsdPerBtc,
              amountBtc,
              lockedUsd,
              OrderStatus.OPEN);
      order = orderRepository.save(order);
    }
    return order;
  }

  public List<Order> listOrders() {
    return orderRepository.findAll();
  }

  public List<Order> listOrdersByStatus(String status) {
    OrderStatus st = parseOrderStatus(status).orElseThrow(() -> new DomainException("INVALID_STATUS", "Unknown status: " + status));
    return orderRepository.findByStatus(st);
  }

  private static Optional<OrderStatus> parseOrderStatus(String status) {
    if (status == null || status.isBlank()) return Optional.empty();
    try {
      return Optional.of(OrderStatus.valueOf(status.toUpperCase()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public Order getOrderById(String orderId) {
    return orderRepository
        .findById(new OrderId(orderId))
        .orElseThrow(
            () -> new DomainException(ORDER_NOT_FOUND, ORDER_NOT_FOUND_TEXT + ": " + orderId));
  }

  /**
   * BUY-only execution rule: execute when currentPrice <= priceLimit. USD was already locked at
   * order creation; on fill we only credit BTC to the account. Open orders are processed
   * concurrently, partitioned by account (same-account orders run on the same worker). Orders are
   * processed in price order (lowest first).
   */
  public int fillEligibleOrders(BigDecimal currentPriceUsdPerBtc) {
    List<Order> open = orderRepository.findByStatus(OrderStatus.OPEN);
    if (open.isEmpty()) return 0;

    Map<AccountId, List<Order>> byAccount =
        open.stream().collect(Collectors.groupingBy(Order::accountId));

    List<Future<Integer>> futures =
        byAccount.values().stream()
            .map(
                ordersForAccount ->
                    fillExecutor.submit(
                        () ->
                            fillOrdersForAccount(ordersForAccount, currentPriceUsdPerBtc)))
            .toList();

    try {
      int sum = 0;
      for (Future<Integer> f : futures) {
        sum += f.get();
      }
      return sum;
    } catch (Exception e) {
      throw new RuntimeException("Fill execution failed", e);
    }
  }

  /** Process orders for one account in price order; fill when market <= limit. */
  private int fillOrdersForAccount(
      List<Order> ordersForAccount, BigDecimal currentPriceUsdPerBtc) {
    int filled = 0;
    for (Order o : ordersForAccount) {
      if (tryFill(o.id(), currentPriceUsdPerBtc)) {
        filled++;
      }
    }
    return filled;
  }

  public List<Order> getOrdersForAccount(String accountId) {
    return orderRepository.findByAccountId(new AccountId(accountId));
  }

  private boolean tryFill(OrderId orderId, BigDecimal execPriceUsdPerBtc) {
    return claimExecuting(orderId)
        .map(
            claimed -> {
              if (execPriceUsdPerBtc.compareTo(claimed.priceLimitUsdPerBtc()) > 0) {
                revertToOpen(orderId);
                return false;
              }
              return applyFillAndFinalize(orderId, claimed);
            })
        .orElse(false);
  }

  private Optional<Order> claimExecuting(OrderId orderId) {
    return orderRepository
        .update(
            orderId,
            existing ->
                existing.status() == OrderStatus.OPEN
                    ? existing.withStatus(OrderStatus.EXECUTING)
                    : existing)
        .filter(claimed -> claimed.status() == OrderStatus.EXECUTING);
  }

  private void revertToOpen(OrderId orderId) {
    orderRepository.update(
        orderId,
        o ->
            o.status() == OrderStatus.EXECUTING ? o.withStatus(OrderStatus.OPEN) : o);
  }

  private boolean applyFillAndFinalize(OrderId orderId, Order claimed) {
    var accId = claimed.accountId();
    synchronized (lockFor(accId.value())) {
      accountService.applyTrade(accId, BigDecimal.ZERO, claimed.amountBtc());
    }
    return orderRepository
        .update(
            orderId,
            o ->
                o.status() == OrderStatus.EXECUTING
                    ? o.withStatus(OrderStatus.FILLED)
                    : o)
        .filter(o -> o.status() == OrderStatus.FILLED)
        .isPresent();
  }
}
