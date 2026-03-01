package btc.exchange.trading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import btc.exchange.trading.application.account.AccountService;
import btc.exchange.trading.application.order.OrderService;
import btc.exchange.trading.domain.account.Account;
import btc.exchange.trading.domain.common.DomainException;
import btc.exchange.trading.domain.order.Order;
import btc.exchange.trading.domain.order.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmokeTest {

  @Autowired private AccountService accountService;

  @Autowired private OrderService orderService;

  @Test
  void contextLoads_and_basicFlowWorks() {
    // create account (BTC starts at 0 in AccountService)
    Account account = accountService.createAccount("test-user", new BigDecimal("10000"));

    assertThat(account).isNotNull();
    assertThat(account.usdBalance()).isEqualByComparingTo("10000");
    assertThat(account.btcBalance()).isEqualByComparingTo("0");

    // create order
    Order order =
        orderService.createOrder(
            account.id().value(), new BigDecimal("30000"), new BigDecimal("0.1"));

    assertThat(order.status()).isEqualTo(OrderStatus.OPEN);

    // execute fill at lower price (should execute)
    int filled = orderService.fillEligibleOrders(new BigDecimal("29000"));
    assertThat(filled).isEqualTo(1);

    // verify order updated
    Order updated = orderService.getOrderById(order.id().value());
    assertThat(updated.status()).isEqualTo(OrderStatus.FILLED);

    // USD was locked at order creation (30000*0.1=3000); fill only adds BTC
    Account after = accountService.getAccountById(account.id().value());
    assertThat(after.usdBalance()).isEqualByComparingTo("7000");
    assertThat(after.btcBalance()).isEqualByComparingTo("0.1");
  }

  @Test
  void createOrder_rejectsWhenInsufficientFunds() {
    Account account = accountService.createAccount("low-funds", new BigDecimal("100"));
    // Order would need 30000*0.1 = 3000 USD; account has only 100
    assertThatThrownBy(
            () ->
                orderService.createOrder(
                    account.id().value(), new BigDecimal("30000"), new BigDecimal("0.1")))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Insufficient USD balance");

    Account after = accountService.getAccountById(account.id().value());
    assertThat(after.usdBalance()).isEqualByComparingTo("100");
    assertThat(after.btcBalance()).isEqualByComparingTo("0");
  }

  @Test
  void listOperations_work() {
    Account account = accountService.createAccount("list-test", new BigDecimal("5000"));

    orderService.createOrder(account.id().value(), new BigDecimal("20000"), new BigDecimal("0.05"));

    List<Order> orders = orderService.listOrders();
    List<Account> accounts = accountService.listAccounts();

    assertThat(accounts).isNotEmpty();
    assertThat(orders).isNotEmpty();
  }
}
