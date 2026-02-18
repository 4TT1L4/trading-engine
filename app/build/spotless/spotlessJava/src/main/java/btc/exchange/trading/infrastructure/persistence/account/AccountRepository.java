package btc.exchange.trading.infrastructure.persistence.account;

import btc.exchange.trading.domain.account.Account;
import btc.exchange.trading.domain.account.AccountId;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface AccountRepository {
  Account save(Account account);

  Optional<Account> findById(AccountId id);

  List<Account> findAll();

  /**
   * Atomic update for in-memory consistency. Returns updated account, or empty if account doesn't
   * exist.
   */
  Optional<Account> update(AccountId id, UnaryOperator<Account> updater);
}
