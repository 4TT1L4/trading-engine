package btc.exchange.trading.infrastructure.marketdata;

import btc.exchange.trading.application.config.AppProperties;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpMarketDataClient implements MarketDataClient {

  private final RestClient restClient;

  public HttpMarketDataClient(AppProperties props) {
    this.restClient = RestClient.builder().baseUrl(props.baseUrl()).build();
  }

  @Override
  public BigDecimal getCurrentBtcUsdPrice() {
    // expects: { "symbol": "...", "price": 50123.45, "ts": 123 }
    Map<?, ?> body =
        restClient
            .get()
            .uri("/price")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(Map.class);

    Object price = body.get("price");
    return new BigDecimal(String.valueOf(price));
  }
}
