package btc.exchange.trading.application.order;

import static org.assertj.core.api.Assertions.*;

import btc.exchange.trading.application.account.AccountService;
import btc.exchange.trading.domain.common.DomainException;
import btc.exchange.trading.domain.order.OrderStatus;
import btc.exchange.trading.infrastructure.persistence.account.InMemoryAccountRepository;
import btc.exchange.trading.infrastructure.persistence.order.InMemoryOrderRepository;
import java.math.BigDecimal;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

  private final InMemoryOrderRepository orderRepo = new InMemoryOrderRepository();
  private final AccountService accountService = new AccountService(new InMemoryAccountRepository());
  private final OrderService orderService = new OrderService(orderRepo, accountService);

  @Test
  void createOrder_rejectsMissingAccount() {
    assertThatThrownBy(() -> orderService.createOrder("missing", BigDecimal.ONE, BigDecimal.ONE))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Account not found");
  }

  @Test
  void createOrder_rejectsInvalidPriceOrAmount() {
    var acc = accountService.createAccount("A", new BigDecimal("100"));

    assertThatThrownBy(() -> orderService.createOrder(acc.id().value(), BigDecimal.ZERO, BigDecimal.ONE))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("priceLimitUsdPerBtc must be > 0");

    assertThatThrownBy(() -> orderService.createOrder(acc.id().value(), BigDecimal.ONE, BigDecimal.ZERO))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("amountBtc must be > 0");
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
    var order = orderService.createOrder(acc.id().value(), new BigDecimal("30000"), new BigDecimal("0.1"));

    int filled = orderService.fillEligibleOrders(new BigDecimal("31000"));
    assertThat(filled).isZero();

    var reloaded = orderService.getOrderById(order.id().value());
    assertThat(reloaded.status()).isEqualTo(OrderStatus.OPEN);
  }

  @Test
  void concurrentFill_doesNotDoubleSpend() throws Exception {
    var acc = accountService.createAccount("Concurrent", new BigDecimal("1000"));

    // cost per order = 40000 * 0.01 = 400 USD => at most 2 orders can fill
    for (int i = 0; i < 10; i++) {
      orderService.createOrder(acc.id().value(), new BigDecimal("50000"), new BigDecimal("0.01"));
    }

    int threads = 10;
    var pool = Executors.newFixedThreadPool(threads);
    var start = new CountDownLatch(1);
    var done = new CountDownLatch(threads);

    for (int i = 0; i < threads; i++) {
      pool.submit(() -> {
        try {
          start.await(3, TimeUnit.SECONDS);
          orderService.fillEligibleOrders(new BigDecimal("40000"));
        } catch (Exception ignored) {
        } finally {
          done.countDown();
        }
      });
    }

    start.countDown();
    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    pool.shutdownNow();

    long filledCount = orderRepo.findByStatus(OrderStatus.FILLED).size();
    assertThat(filledCount).isEqualTo(2);

    var after = accountService.getAccountById(acc.id().value());
    assertThat(after.usdBalance()).isEqualByComparingTo("200");
    assertThat(after.btcBalance()).isEqualByComparingTo("0.02");

    // no order should be stuck in EXECUTING
    assertThat(orderRepo.findByStatus(OrderStatus.EXECUTING)).isEmpty();
  }
}
