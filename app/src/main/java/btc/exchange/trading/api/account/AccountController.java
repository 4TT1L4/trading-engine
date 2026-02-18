package btc.exchange.trading.api.account;

import btc.exchange.trading.api.account.dto.AccountDetailsResponse;
import btc.exchange.trading.api.account.dto.AccountResponse;
import btc.exchange.trading.api.order.dto.OrderResponse;
import btc.exchange.trading.api.account.dto.CreateAccountRequest;
import btc.exchange.trading.application.account.AccountService;
import btc.exchange.trading.application.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Accounts")
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final OrderService orderService;

public AccountController(AccountService accountService, OrderService orderService) {
    this.accountService = accountService;
    this.orderService = orderService;
}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest req) {
        var a = accountService.createAccount(req.name(), req.usdBalance());
        return new AccountResponse(a.id().value(), a.name(), a.usdBalance(), a.btcBalance());
    }

@GetMapping("/{accountId}")
public AccountDetailsResponse getById(@PathVariable String accountId) {

    var account = accountService.getAccountById(accountId);
    var orders = orderService.getOrdersForAccount(accountId);

    var orderDtos = orders.stream()
            .map(o -> new OrderResponse(
                    o.id().value(),
                    o.accountId().value(),
                    o.priceLimitUsdPerBtc(),
                    o.amountBtc(),
                    o.status()
            ))
            .toList();

    return new AccountDetailsResponse(
            account.id().value(),
            account.name(),
            account.usdBalance(),
            account.btcBalance(),
            orderDtos
    );
}

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.listAccounts().stream()
                .map(a -> new AccountResponse(a.id().value(), a.name(), a.usdBalance(), a.btcBalance()))
                .toList();
    }
}
