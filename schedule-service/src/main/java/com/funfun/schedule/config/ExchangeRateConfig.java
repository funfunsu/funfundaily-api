package com.funfun.schedule.config;

import com.funfun.schedule.enums.AssetMarket;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 各市场 → 人民币的汇率配置。
 *
 * <p>YAML：
 * <pre>
 * app:
 *   financial-plan:
 *     exchange-rates:
 *       US: 7.20
 *       HK: 0.92
 *       CN: 1.00
 * </pre>
 *
 * <p>未配置时使用代码内默认值兜底；CN 永远是 1.0。
 */
@Component
@ConfigurationProperties(prefix = "app.financial-plan")
@Data
public class ExchangeRateConfig {

    /** 各市场 → 人民币的汇率。key 为 {@link AssetMarket} 的名称。 */
    private Map<String, BigDecimal> exchangeRates = new HashMap<>();

    private static final Map<AssetMarket, BigDecimal> DEFAULTS = new EnumMap<>(AssetMarket.class);

    static {
        DEFAULTS.put(AssetMarket.US, new BigDecimal("7.20"));
        DEFAULTS.put(AssetMarket.HK, new BigDecimal("0.92"));
        DEFAULTS.put(AssetMarket.CN, BigDecimal.ONE);
    }

    /** 解析指定市场到人民币的汇率；未配置或无效时返回默认值。 */
    public BigDecimal resolveRate(AssetMarket market) {
        if (market == null) {
            return BigDecimal.ONE;
        }
        BigDecimal configured = exchangeRates.get(market.name());
        if (configured != null && configured.signum() > 0) {
            return configured;
        }
        return DEFAULTS.getOrDefault(market, BigDecimal.ONE);
    }
}
