package btc.exchange.trading.api.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank String accountId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal priceLimitUsdPerBtc,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amountBtc
) {}
