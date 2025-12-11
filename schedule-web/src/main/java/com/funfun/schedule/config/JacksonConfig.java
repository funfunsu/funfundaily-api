package com.funfun.schedule.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

/**
 * Jackson配置类，用于自定义序列化和反序列化规则
 * 修改以支持 GMT+8 时区 和 yyyy-MM-dd'T'HH:mm:ss 格式
 */
@Configuration
public class JacksonConfig {

    /**
     * 日期时间格式 - 包含 'T' 分隔符
     */
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"; // <-- 修改这里

    /**
     * 目标时区 - GMT+8
     */
    private static final ZoneId TARGET_ZONE_ID = ZoneId.of("GMT+8");
    // 或者使用常量字符串 "GMT+8" 直接传入
    private static final TimeZone TARGET_TIME_ZONE = TimeZone.getTimeZone(TARGET_ZONE_ID); // 缓存 TimeZone 对象

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // --- 配置反序列化时的默认日期格式和时区 ---
        // 这是关键：告诉 Jackson 如何解析没有 @JsonFormat 且符合 DATE_TIME_FORMAT 的日期字符串
        // 必须使用单引号包裹 'T'
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
        dateFormat.setTimeZone(TARGET_TIME_ZONE); // 设置为 GMT+8
        objectMapper.setDateFormat(dateFormat); // <-- 这行是解决反序列化问题的关键
        // 禁用 WRITE_DATES_AS_TIMESTAMPS，强制使用字符串格式而非毫秒数
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // --- 配置结束 ---


        // 创建自定义模块
        SimpleModule simpleModule = new SimpleModule();

        // 注册Long类型序列化器，将Long转为String
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 注册Date类型序列化器，将Date转为指定格式的字符串 (使用 GMT+8)
        simpleModule.addSerializer(Date.class, new DateToStringSerializer()); // <-- 会使用下面修改后的序列化器

        // 注册ScheduleItem自定义序列化器，处理repeatKeys字段 (内部日期也使用 GMT+8)
        // simpleModule.addSerializer(ScheduleItem.class, new ScheduleItemSerializer(objectMapper));

        // 处理Java 8日期时间类型 (如果需要，也可以在这里设置时区)
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        // 为 LocalDateTime 设置相同的格式化器
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));

        // 注册模块
        objectMapper.registerModule(simpleModule);
        objectMapper.registerModule(javaTimeModule);

        // 其他可能需要的配置
        // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper;
    }

    /**
     * 自定义Date类型序列化器 - 修改为使用 GMT+8 时区 和 yyyy-MM-dd'T'HH:mm:ss 格式
     */
    public static class DateToStringSerializer extends StdSerializer<Date> {

        protected DateToStringSerializer() {
            super(Date.class);
        }

        @Override
        public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value != null) {
                // 使用 GMT+8 时区进行格式化
                String formattedDate = value.toInstant()
                        .atZone(TARGET_ZONE_ID) // Convert Instant to ZonedDateTime in GMT+8
                        .toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)); // <-- 使用更新后的格式
                gen.writeString(formattedDate);
            }
        }
    }
}