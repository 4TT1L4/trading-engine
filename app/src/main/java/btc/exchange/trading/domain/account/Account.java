package btc.exchange.trading.domain.account;

import java.math.BigDecimal;

public record Account(
        AccountId id,
        String name,
        BigDecimal usdBalance,
        BigDecimal btcBalance
) {
    public Account withBalances(BigDecimal usd, BigDecimal btc) {
        return new Account(id, name, usd, btc);
    }
}
