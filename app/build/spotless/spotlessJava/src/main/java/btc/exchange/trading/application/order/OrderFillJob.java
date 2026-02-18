package btc.exchange.trading.application.order;

import btc.exchange.trading.application.config.AppProperties;
import btc.exchange.trading.infrastructure.marketdata.MarketDataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderFillJob {

  private static final Logger log = LoggerFactory.getLogger(OrderFillJob.class);

  private final AppProperties props;
  private final MarketDataClient marketDataClient;
  private final OrderService orderService;

  public OrderFillJob(
      AppProperties props, MarketDataClient marketDataClient, OrderService orderService) {
    this.props = props;
    this.marketDataClient = marketDataClient;
    this.orderService = orderService;
  }

  @Scheduled(fixedDelayString = "${exchange.poll-interval-ms:1000}")
  public void pollAndFill() {
    if (!props.fillWorkerEnabled()) return;

    var price = marketDataClient.getCurrentBtcUsdPrice();
    int filled = orderService.fillEligibleOrders(price);

    if (filled > 0) {
      log.info("Filled {} order(s) at price={}", filled, price);
    }
  }
}
