package btc.exchange.trading.application.order;

import static org.assertj.core.api.Assertions.*;

import btc.exchange.trading.application.account.AccountService;
import btc.exchange.trading.domain.common.DomainException;
import btc.exchange.trading.domain.order.Order;
import btc.exchange.trading.domain.order.OrderStatus;
import btc.exchange.trading.infrastructure.persistence.account.InMemoryAccountRepository;
import btc.exchange.trading.infrastructure.persistence.order.InMemoryOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

  private static final ExecutorService fillExecutor = Executors.newFixedThreadPool(4);

  private final InMemoryOrderRepository orderRepo = new InMemoryOrderRepository();
  private final AccountService accountService = new AccountService(new InMemoryAccountRepository());
  private final OrderService orderService =
      new OrderService(orderRepo, accountService, fillExecutor);

  @AfterAll
  static void shutdownExecutor() {
    fillExecutor.shutdown();
    try {
      if (!fillExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        fillExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      fillExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void createOrder_rejectsMissingAccount() {
    assertThatThrownBy(() -> orderService.createOrder("missing", BigDecimal.ONE, BigDecimal.ONE))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Account not found");
  }

  @Test
  void createOrder_rejectsInvalidPriceOrAmount() {
    var acc = accountService.createAccount("A", new BigDecimal("100"));

    assertThatThrownBy(
            () -> orderService.createOrder(acc.id().value(), BigDecimal.ZERO, BigDecimal.ONE))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("priceLimitUsdPerBtc must be > 0");

    assertThatThrownBy(
            () -> orderService.createOrder(acc.id().value(), BigDecimal.ONE, BigDecimal.ZERO))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("amountBtc must be > 0");
  }

  @Test
  void createOrder_rejectsInsufficientFunds() {
    var acc = accountService.createAccount("A", new BigDecimal("100"));
    // Order requires 30000 * 0.01 = 300 USD; account has 100
    assertThatThrownBy(
            () ->
                orderService.createOrder(
                    acc.id().value(), new BigDecimal("30000"), new BigDecimal("0.01")))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "INSUFFICIENT_FUNDS")
        .hasMessageContaining("Insufficient USD balance");
    assertThat(accountService.getAccountById(acc.id().value()).usdBalance())
        .isEqualByComparingTo("100");
  }

  @Test
  void createOrder_locksUsdAndFillAddsBtc() {
    var acc = accountService.createAccount("A", new BigDecimal("10000"));
    // Lock 30000 * 0.1 = 3000 USD
    var order =
        orderService.createOrder(acc.id().value(), new BigDecimal("30000"), new BigDecimal("0.1"));

    var afterCreate = accountService.getAccountById(acc.id().value());
    assertThat(afterCreate.usdBalance()).isEqualByComparingTo("7000");
    assertThat(afterCreate.btcBalance()).isEqualByComparingTo("0");

    int filled = orderService.fillEligibleOrders(new BigDecimal("30000"));
    assertThat(filled).isOne();

    var afterFill = accountService.getAccountById(acc.id().value());
    assertThat(afterFill.usdBalance()).isEqualByComparingTo("7000");
    assertThat(afterFill.btcBalance()).isEqualByComparingTo("0.1");
    assertThat(orderService.getOrderById(order.id().value()).status())
        .isEqualTo(OrderStatus.FILLED);
  }

  @Test
  void listOrdersByStatus_rejectsUnknownStatus() {
    assertThatThrownBy(() -> orderService.listOrdersByStatus("nope"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Unknown status");
  }

  @Test
  void getOrdersForAccount_filtersCorrectly() {
    var a1 = accountService.createAccount("A1", new BigDecimal("1000"));
    var a2 = accountService.createAccount("A2", new BigDecimal("1000"));

    orderService.createOrder(a1.id().value(), new BigDecimal("30000"), new BigDecimal("0.01"));
    orderService.createOrder(a1.id().value(), new BigDecimal("30000"), new BigDecimal("0.02"));
    orderService.createOrder(a2.id().value(), new BigDecimal("30000"), new BigDecimal("0.03"));

    assertThat(orderService.getOrdersForAccount(a1.id().value())).hasSize(2);
    assertThat(orderService.getOrdersForAccount(a2.id().value())).hasSize(1);
  }

  @Test
  void fillEligibleOrders_doesNotFillWhenPriceAboveLimit() {
    var acc = accountService.createAccount("A", new BigDecimal("10000"));
    var order =
        orderService.createOrder(acc.id().value(), new BigDecimal("30000"), new BigDecimal("0.1"));

    int filled = orderService.fillEligibleOrders(new BigDecimal("31000"));
    assertThat(filled).isZero();

    var reloaded = orderService.getOrderById(order.id().value());
    assertThat(reloaded.status()).isEqualTo(OrderStatus.OPEN);
    // USD stayed locked (3000 deducted at create)
    assertThat(accountService.getAccountById(acc.id().value()).usdBalance())
        .isEqualByComparingTo("7000");
  }

  @Test
  void openOrders_returnedSortedByPriceAscending() {
    var acc = accountService.createAccount("A", new BigDecimal("100000"));
    orderService.createOrder(acc.id().value(), new BigDecimal("32000"), new BigDecimal("0.01"));
    orderService.createOrder(acc.id().value(), new BigDecimal("30000"), new BigDecimal("0.01"));
    orderService.createOrder(acc.id().value(), new BigDecimal("31000"), new BigDecimal("0.01"));

    List<Order> open = orderRepo.findByStatus(OrderStatus.OPEN);
    assertThat(open).hasSize(3);
    assertThat(open.get(0).priceLimitUsdPerBtc()).isEqualByComparingTo("30000");
    assertThat(open.get(1).priceLimitUsdPerBtc()).isEqualByComparingTo("31000");
    assertThat(open.get(2).priceLimitUsdPerBtc()).isEqualByComparingTo("32000");
  }

  @Test
  void fillEligibleOrders_stopsWhenMarketPriceExceeded_onlyLowerOrEqualPricesFilled() {
    var acc = accountService.createAccount("A", new BigDecimal("100000"));
    orderService.createOrder(acc.id().value(), new BigDecimal("29000"), new BigDecimal("0.01"));
    orderService.createOrder(acc.id().value(), new BigDecimal("30000"), new BigDecimal("0.01"));
    orderService.createOrder(acc.id().value(), new BigDecimal("31000"), new BigDecimal("0.01"));

    int filled = orderService.fillEligibleOrders(new BigDecimal("30000"));
    assertThat(filled).isEqualTo(2);

    var open =
        orderService.getOrdersForAccount(acc.id().value()).stream()
            .filter(o -> o.status() == OrderStatus.OPEN)
            .toList();
    assertThat(open).hasSize(1);
    assertThat(open.get(0).priceLimitUsdPerBtc()).isEqualByComparingTo("29000");
    assertThat(accountService.getAccountById(acc.id().value()).btcBalance())
        .isEqualByComparingTo("0.02");
  }

  @Test
  void fillEligibleOrders_concurrentFill_multipleAccountsBothFilled() {
    var a1 = accountService.createAccount("A1", new BigDecimal("50000"));
    var a2 = accountService.createAccount("A2", new BigDecimal("50000"));
    orderService.createOrder(a1.id().value(), new BigDecimal("30000"), new BigDecimal("0.1"));
    orderService.createOrder(a2.id().value(), new BigDecimal("30000"), new BigDecimal("0.2"));

    int filled = orderService.fillEligibleOrders(new BigDecimal("29000"));
    assertThat(filled).isEqualTo(2);

    assertThat(orderService.getOrdersForAccount(a1.id().value()).get(0).status())
        .isEqualTo(OrderStatus.FILLED);
    assertThat(orderService.getOrdersForAccount(a2.id().value()).get(0).status())
        .isEqualTo(OrderStatus.FILLED);
    assertThat(accountService.getAccountById(a1.id().value()).btcBalance())
        .isEqualByComparingTo("0.1");
    assertThat(accountService.getAccountById(a2.id().value()).btcBalance())
        .isEqualByComparingTo("0.2");
  }

  @Test
  void createOrder_concurrentCallsOnlyOneSucceedsWhenFundsAllowOne() throws Exception {
    var acc = accountService.createAccount("A", new BigDecimal("300"));
    // Each order needs 30000*0.01 = 300 USD; only one can succeed
    var created = new java.util.concurrent.atomic.AtomicInteger(0);
    var insufficient = new java.util.concurrent.atomic.AtomicInteger(0);
    Runnable run =
        () -> {
          try {
            orderService.createOrder(
                acc.id().value(), new BigDecimal("30000"), new BigDecimal("0.01"));
            created.incrementAndGet();
          } catch (DomainException e) {
            if ("INSUFFICIENT_FUNDS".equals(e.code())) insufficient.incrementAndGet();
            else throw e;
          }
        };

    var t1 = new Thread(run);
    var t2 = new Thread(run);
    t1.start();
    t2.start();
    t1.join();
    t2.join();

    assertThat(created.get()).isOne();
    assertThat(insufficient.get()).isOne();
    var orders = orderService.getOrdersForAccount(acc.id().value());
    assertThat(orders).hasSize(1);
    assertThat(accountService.getAccountById(acc.id().value()).usdBalance())
        .isEqualByComparingTo("0");
  }
}
