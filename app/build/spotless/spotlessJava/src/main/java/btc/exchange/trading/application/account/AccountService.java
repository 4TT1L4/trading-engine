package btc.exchange.trading.application.account;

import btc.exchange.trading.domain.account.Account;
import btc.exchange.trading.domain.account.AccountId;
import btc.exchange.trading.domain.common.DomainException;
import btc.exchange.trading.infrastructure.persistence.account.AccountRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

  private final AccountRepository accountRepository;

  public AccountService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public Account createAccount(String name, BigDecimal usdBalance) {
    if (name == null || name.isBlank()) {
      throw new DomainException("INVALID_ACCOUNT", "Account name must not be blank");
    }
    if (usdBalance == null || usdBalance.signum() < 0) {
      throw new DomainException("INVALID_ACCOUNT", "usdBalance must be >= 0");
    }
    var account = new Account(AccountId.newId(), name.trim(), usdBalance, BigDecimal.ZERO);
    return accountRepository.save(account);
  }

  public List<Account> listAccounts() {
    return accountRepository.findAll();
  }

  public Account getAccountById(String accountId) {
    return accountRepository
        .findById(new AccountId(accountId))
        .orElseThrow(
            () -> new DomainException("ACCOUNT_NOT_FOUND", "Account not found: " + accountId));
  }

  public Account requireAccount(String accountId) {
    return getAccountById(accountId);
  }

  /** Atomic balance update: subtract USD, add BTC. */
  public Account applyTrade(AccountId id, BigDecimal usdDelta, BigDecimal btcDelta) {
    return accountRepository
        .update(
            id,
            acc -> acc.withBalances(acc.usdBalance().add(usdDelta), acc.btcBalance().add(btcDelta)))
        .orElseThrow(
            () -> new DomainException("ACCOUNT_NOT_FOUND", "Account not found: " + id.value()));
  }
}
