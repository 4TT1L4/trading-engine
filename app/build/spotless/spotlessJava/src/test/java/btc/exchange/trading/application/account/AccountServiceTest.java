package btc.exchange.trading.application.account;

import static org.assertj.core.api.Assertions.*;

import btc.exchange.trading.domain.common.DomainException;
import btc.exchange.trading.infrastructure.persistence.account.InMemoryAccountRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

  private final AccountService accountService = new AccountService(new InMemoryAccountRepository());

  @Test
  void createAccount_trimsName_andSetsBtcToZero() {
    var acc = accountService.createAccount("  Alice  ", new BigDecimal("10.50"));

    assertThat(acc.name()).isEqualTo("Alice");
    assertThat(acc.usdBalance()).isEqualByComparingTo("10.50");
    assertThat(acc.btcBalance()).isEqualByComparingTo("0");
  }

  @Test
  void createAccount_rejectsBlankName() {
    assertThatThrownBy(() -> accountService.createAccount("   ", BigDecimal.TEN))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Account name must not be blank");
  }

  @Test
  void createAccount_rejectsNegativeUsdBalance() {
    assertThatThrownBy(() -> accountService.createAccount("Bob", new BigDecimal("-0.01")))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("usdBalance must be >= 0");
  }

  @Test
  void getAccountById_throwsWhenNotFound() {
    assertThatThrownBy(() -> accountService.getAccountById("missing"))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Account not found");
  }

  @Test
  void applyTrade_updatesBothBalancesAtomically() {
    var acc = accountService.createAccount("Trader", new BigDecimal("100"));
    var updated =
        accountService.applyTrade(acc.id(), new BigDecimal("-25"), new BigDecimal("0.005"));

    assertThat(updated.usdBalance()).isEqualByComparingTo("75");
    assertThat(updated.btcBalance()).isEqualByComparingTo("0.005");
  }
}
