package com.funfun.schedule.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.funfun.schedule.entity.ScheduleItem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary; // Import Primary annotation

import java.io.IOException;
import java.text.SimpleDateFormat; // Import SimpleDateFormat
import java.time.LocalDateTime;
import java.time.ZoneId; // Import ZoneId for clarity
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone; // Import TimeZone

/**
 * Jackson配置类，用于自定义序列化规则
 * 修改以支持 GMT+8 时区
 */
@Configuration
public class JacksonConfig {

    /**
     * 日期时间格式
     */
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 目标时区 - GMT+8
     */
    private static final ZoneId TARGET_ZONE_ID = ZoneId.of("GMT+8");
    // 或者使用常量字符串 "GMT+8" 直接传入

    @Bean
    @Primary // Add @Primary to ensure this ObjectMapper is used by default
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // --- 配置反序列化时的默认日期格式和时区 ---
        // 这是关键：告诉 Jackson 如何解析没有 @JsonFormat 且无时区信息的日期字符串
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone(TARGET_ZONE_ID)); // 设置为 GMT+8
        objectMapper.setDateFormat(dateFormat);
        // --- 配置结束 ---


        // 创建自定义模块
        SimpleModule simpleModule = new SimpleModule();

        // 注册Long类型序列化器，将Long转为String
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 注册Date类型序列化器，将Date转为指定格式的字符串 (使用 GMT+8)
        simpleModule.addSerializer(Date.class, new DateToStringSerializer());

        // 注册ScheduleItem自定义序列化器，处理repeatKeys字段 (内部日期也使用 GMT+8)
//        simpleModule.addSerializer(ScheduleItem.class, new ScheduleItemSerializer(objectMapper));

        // 处理Java 8日期时间类型 (如果需要，也可以在这里设置时区)
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        // 如果也需要为 Java 8 Time API 设置默认时区，可以考虑自定义序列化器
        // 但对于反序列化，通常依赖 ObjectMapper 的时区设置或字段上的 @JsonFormat
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));

        // 注册模块
        objectMapper.registerModule(simpleModule);
        objectMapper.registerModule(javaTimeModule);

        // 其他可能需要的配置
        // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper;
    }

    /**
     * 自定义Date类型序列化器 - 修改为使用 GMT+8 时区
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
                        .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
                gen.writeString(formattedDate);
            }
        }
    }

//    /**
//     * ScheduleItem自定义序列化器，处理repeatKeys字段将JSON数组字符串转换为实际的JSON数组
//     * 修改日期序列化部分以使用 GMT+8 时区
//     */
//    public static class ScheduleItemSerializer extends StdSerializer<ScheduleItem> {
//
//        private final ObjectMapper objectMapper;
//
//        public ScheduleItemSerializer(ObjectMapper objectMapper) {
//            super(ScheduleItem.class);
//            this.objectMapper = objectMapper;
//        }
//
//        @Override
//        public void serialize(ScheduleItem value, JsonGenerator gen, SerializerProvider provider) throws IOException {
//            gen.writeStartObject();
//
//            // 序列化其他字段
//            gen.writeStringField("id", String.valueOf(value.getId()));
//            gen.writeStringField("itemTitle", value.getItemTitle());
//            gen.writeStringField("itemDesc", value.getItemDesc());
//            gen.writeStringField("location", value.getLocation());
//            gen.writeStringField("repeatType", value.getRepeatType());
//
//            // 处理repeatKeys字段，将JSON数组字符串转换为实际的JSON数组
//            if (value.getRepeatKeys() != null && !value.getRepeatKeys().isEmpty()) {
//                try {
//                    JsonNode node = objectMapper.readTree(value.getRepeatKeys());
//                    gen.writeFieldName("repeatKeys");
//                    gen.writeTree(node);
//                } catch (Exception e) {
//                    // 如果解析失败，仍然按字符串处理
//                    gen.writeStringField("repeatKeys", value.getRepeatKeys());
//                }
//            } else {
//                gen.writeNullField("repeatKeys");
//            }
//
//            // --- 修改日期序列化以使用 GMT+8 时区 ---
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
//
//            if (value.getRepeatStartDay() != null) {
//                String formattedDate = value.getRepeatStartDay().toInstant()
//                        .atZone(TARGET_ZONE_ID)
//                        .toLocalDateTime()
//                        .format(formatter);
//                gen.writeStringField("repeatStartDay", formattedDate);
//            } else {
//                gen.writeNullField("repeatStartDay");
//            }
//
//            if (value.getRepeatEndDay() != null) {
//                String formattedDate = value.getRepeatEndDay().toInstant()
//                        .atZone(TARGET_ZONE_ID)
//                        .toLocalDateTime()
//                        .format(formatter);
//                gen.writeStringField("repeatEndDay", formattedDate);
//            } else {
//                gen.writeNullField("repeatEndDay");
//            }
//            // --- 修改结束 ---
//
//            gen.writeStringField("itemType", value.getItemType());
//
//            // --- 修改 startTime/endTime 序列化以使用 GMT+8 时区 ---
//            if (value.getStartTime() != null) {
//                String formattedDate = value.getStartTime().toInstant()
//                        .atZone(TARGET_ZONE_ID)
//                        .toLocalDateTime()
//                        .format(formatter);
//                gen.writeStringField("startTime", formattedDate);
//            } else {
//                gen.writeNullField("startTime");
//            }
//
//            if (value.getEndTime() != null) {
//                String formattedDate = value.getEndTime().toInstant()
//                        .atZone(TARGET_ZONE_ID)
//                        .toLocalDateTime()
//                        .format(formatter);
//                gen.writeStringField("endTime", formattedDate);
//            } else {
//                gen.writeNullField("endTime");
//            }
//            // --- 修改结束 ---
//
//
//            gen.writeStringField("userId", String.valueOf(value.getUserId()));
//            gen.writeStringField("groupId", String.valueOf(value.getGroupId()));
//
//            gen.writeEndObject();
//        }
//    }
}