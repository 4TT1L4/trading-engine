package btc.exchange.trading.domain.account;

import java.util.UUID;

public record AccountId(String value) {
    public static AccountId newId() {
        return new AccountId(UUID.randomUUID().toString());
    }
}
