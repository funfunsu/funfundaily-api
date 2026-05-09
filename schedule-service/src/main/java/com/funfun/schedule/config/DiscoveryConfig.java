package com.funfun.schedule.config;

import com.funfun.schedule.dto.DiscoveryItemDTO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 发现页配置：app.discovery.items
 *
 * 维护方式：直接在 application.yml 的 app.discovery.items 下增删条目。
 * 字段与 DiscoveryItemDTO 对齐（itemTitle / uri / itemType / status / id）。
 */
@Component
@ConfigurationProperties(prefix = "app.discovery")
@Data
public class DiscoveryConfig {
    private List<DiscoveryItemDTO> items = new ArrayList<>();
}
