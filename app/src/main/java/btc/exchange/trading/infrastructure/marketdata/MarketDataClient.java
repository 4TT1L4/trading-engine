package btc.exchange.trading.infrastructure.marketdata;

import java.math.BigDecimal;

public interface MarketDataClient {
    BigDecimal getCurrentBtcUsdPrice();
}
