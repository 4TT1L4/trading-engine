package btc.exchange.trading.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "exchange")
public record AppProperties(
        String baseUrl,
        long pollIntervalMs,
        boolean fillWorkerEnabled
) {}
