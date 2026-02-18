package btc.exchange.trading.application.order;

import btc.exchange.trading.application.account.AccountService;
import btc.exchange.trading.domain.account.AccountId;
import btc.exchange.trading.domain.common.DomainException;
import btc.exchange.trading.domain.order.Order;
import btc.exchange.trading.domain.order.OrderId;
import btc.exchange.trading.domain.order.OrderStatus;
import btc.exchange.trading.infrastructure.persistence.order.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {

    private final static String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";

    private final OrderRepository orderRepository;
    private final AccountService accountService;

    private final ConcurrentHashMap<String, Object> accountLocks = new ConcurrentHashMap<>();
    
    public OrderService(OrderRepository orderRepository, AccountService accountService) {
        this.orderRepository = orderRepository;
        this.accountService = accountService;
    }

    private Object lockFor(String accountId) {
        return accountLocks.computeIfAbsent(accountId, k -> new Object());
    }

    public Order createOrder(String accountId, BigDecimal priceLimitUsdPerBtc, BigDecimal amountBtc) {
        // ensure account exists
        accountService.requireAccount(accountId);

        if (priceLimitUsdPerBtc == null || priceLimitUsdPerBtc.signum() <= 0) {
            throw new DomainException("INVALID_ORDER", "priceLimitUsdPerBtc must be > 0");
        }
        if (amountBtc == null || amountBtc.signum() <= 0) {
            throw new DomainException("INVALID_ORDER", "amountBtc must be > 0");
        }

        var order = new Order(
                OrderId.newId(),
                new AccountId(accountId),
                priceLimitUsdPerBtc,
                amountBtc,
                OrderStatus.OPEN
        );
        return orderRepository.save(order);
    }

    public List<Order> listOrders() {
        return orderRepository.findAll();
    }

    public List<Order> listOrdersByStatus(String status) {
        OrderStatus st;
        try {
            st = OrderStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw new DomainException("INVALID_STATUS", "Unknown status: " + status);
        }
        return orderRepository.findByStatus(st);
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new DomainException(ORDER_NOT_FOUND, "Order not found: " + orderId));
    }

    /**
     * BUY-only execution rule:
     * - execute when currentPrice <= priceLimit
     * - need enough USD: cost = currentPrice * amountBtc
     * - mark order FILLED exactly once
     */
    public int fillEligibleOrders(BigDecimal currentPriceUsdPerBtc) {
        var open = orderRepository.findByStatus(OrderStatus.OPEN);
        int filled = 0;

        for (var o : open) {
            if (currentPriceUsdPerBtc.compareTo(o.priceLimitUsdPerBtc()) > 0) {
                continue; // price is above limit, do not fill
            }

            boolean didFill = tryFill(o.id(), currentPriceUsdPerBtc);
            if (didFill) filled++;
        }

        return filled;
    }

    public List<Order> getOrdersForAccount(String accountId) {
        return orderRepository.findByAccountId(new AccountId(accountId));
    }
    
    private boolean tryFill(OrderId orderId, BigDecimal execPriceUsdPerBtc) {
        // 1) claim
        var claimed = orderRepository.update(orderId, existing -> {
            if (existing.status() != OrderStatus.OPEN) return existing;
            return existing.withStatus(OrderStatus.EXECUTING);
        }).orElseThrow(() -> new DomainException(ORDER_NOT_FOUND, "Order not found: " + orderId.value()));
    
        if (claimed.status() != OrderStatus.EXECUTING) return false;
    
        // 2) price re-check
        if (execPriceUsdPerBtc.compareTo(claimed.priceLimitUsdPerBtc()) > 0) {
            orderRepository.update(orderId, o -> {
                if (o.status() != OrderStatus.EXECUTING) return o;
                return o.withStatus(OrderStatus.OPEN);
            });
            return false;
        }
    
        var costUsd = execPriceUsdPerBtc.multiply(claimed.amountBtc());
        var accId = claimed.accountId();
    
        boolean ok;
        synchronized (lockFor(accId.value())) {
            var acc = accountService.getAccountById(accId.value());
            ok = acc.usdBalance().compareTo(costUsd) >= 0;
            if (ok) {
                accountService.applyTrade(accId, costUsd.negate(), claimed.amountBtc());
            }
        }
    
        if (!ok) {
            orderRepository.update(orderId, o -> {
                if (o.status() != OrderStatus.EXECUTING) return o;
                return o.withStatus(OrderStatus.OPEN);
            });
            return false;
        }
    
        // 3) finalize only if still executing
        var finalized = orderRepository.update(orderId, o -> {
            if (o.status() != OrderStatus.EXECUTING) return o;
            return o.withStatus(OrderStatus.FILLED);
        }).orElseThrow(() -> new DomainException(ORDER_NOT_FOUND, "Order not found: " + orderId.value()));
    
        return finalized.status() == OrderStatus.FILLED;
    }
    
    
}
