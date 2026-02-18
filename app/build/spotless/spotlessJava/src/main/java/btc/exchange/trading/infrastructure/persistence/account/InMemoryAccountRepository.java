package btc.exchange.trading.infrastructure.persistence.account;

import btc.exchange.trading.domain.account.Account;
import btc.exchange.trading.domain.account.AccountId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryAccountRepository implements AccountRepository {

  private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();

  @Override
  public Account save(Account account) {
    accounts.put(account.id().value(), account);
    return account;
  }

  @Override
  public Optional<Account> findById(AccountId id) {
    return Optional.ofNullable(accounts.get(id.value()));
  }

  @Override
  public List<Account> findAll() {
    return new LinkedList<>(accounts.values());
  }

  @Override
  public Optional<Account> update(AccountId id, UnaryOperator<Account> updater) {
    var updated = accounts.computeIfPresent(id.value(), (k, v) -> updater.apply(v));
    return Optional.ofNullable(updated);
  }
}
