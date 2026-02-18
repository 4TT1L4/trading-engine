package btc.exchange.trading.api.account.dto;

import java.math.BigDecimal;

public record AccountResponse(
    String id, String name, BigDecimal usdBalance, BigDecimal btcBalance) {}
