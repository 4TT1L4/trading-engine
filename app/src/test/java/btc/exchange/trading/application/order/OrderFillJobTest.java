package btc.exchange.trading.application.order;

import static org.mockito.Mockito.*;

import btc.exchange.trading.application.config.AppProperties;
import btc.exchange.trading.infrastructure.marketdata.MarketDataClient;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderFillJobTest {

  @Test
  void pollAndFill_doesNothingWhenWorkerDisabled() {
    var props = new AppProperties("http://x", 1000L, false);
    var mdc = mock(MarketDataClient.class);
    var svc = mock(OrderService.class);

    var job = new OrderFillJob(props, mdc, svc);
    job.pollAndFill();

    verifyNoInteractions(mdc);
    verifyNoInteractions(svc);
  }

  @Test
  void pollAndFill_callsMarketDataAndFillsWhenEnabled() {
    var props = new AppProperties("http://x", 1000L, true);
    var mdc = mock(MarketDataClient.class);
    var svc = mock(OrderService.class);

    when(mdc.getCurrentBtcUsdPrice()).thenReturn(new BigDecimal("42000"));
    when(svc.fillEligibleOrders(new BigDecimal("42000"))).thenReturn(2);

    var job = new OrderFillJob(props, mdc, svc);
    job.pollAndFill();

    verify(mdc).getCurrentBtcUsdPrice();
    verify(svc).fillEligibleOrders(new BigDecimal("42000"));
    verifyNoMoreInteractions(mdc, svc);
  }
}
