package btc.exchange.trading.api.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateAccountRequest(
    @NotBlank String name,
    @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal usdBalance) {}
