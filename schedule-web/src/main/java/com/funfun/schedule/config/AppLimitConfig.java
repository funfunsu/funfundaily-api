package com.funfun.schedule.config; // 请根据你的实际包结构调整

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用程序限制相关的配置属性类。
 * 对应 application.yml 中 app.limit 下的配置。
 */
@Component // (可选) 如果你希望通过 @Autowired 注入这个类实例
@ConfigurationProperties(prefix = "app.limit") // 指定配置前缀
@Data
public class AppLimitConfig {

    /**
     * 计数相关的限制配置
     */
    private Count count = new Count(); // 初始化默认值，避免空指针
    private Count vipCount = new Count(); // 初始化默认值，避免空指针

    /**
     * 内部类，用于表示 count 相关的配置项
     * 对应 application.yml 中 app.limit.count 下的配置。
     */
    @Data
    public static class Count {

        /**
         * 组 (group) 的限制数量
         */
        private Integer group = 1;

        /**
         * 组成员 (group-member) 的限制数量
         */
        private Integer groupMember = 4;
    }
}