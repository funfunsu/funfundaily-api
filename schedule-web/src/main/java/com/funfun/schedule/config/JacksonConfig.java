package com.funfun.schedule.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.funfun.schedule.entity.ScheduleItem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Jackson配置类，用于自定义序列化规则
 */
@Configuration
public class JacksonConfig {

    /**
     * 日期时间格式
     */
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 创建自定义模块
        SimpleModule simpleModule = new SimpleModule();
        
        // 注册Long类型序列化器，将Long转为String
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        
        // 注册Date类型序列化器，将Date转为指定格式的字符串
        simpleModule.addSerializer(Date.class, new DateToStringSerializer());
        
        // 注册ScheduleItem自定义序列化器，处理repeatKeys字段
        simpleModule.addSerializer(ScheduleItem.class, new ScheduleItemSerializer(objectMapper));
        
        // 处理Java 8日期时间类型
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        
        // 注册模块
        objectMapper.registerModule(simpleModule);
        objectMapper.registerModule(javaTimeModule);
        
        return objectMapper;
    }

    /**
     * 自定义Date类型序列化器
     */
    public static class DateToStringSerializer extends StdSerializer<Date> {
        
        protected DateToStringSerializer() {
            super(Date.class);
        }
        
        @Override
        public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
                // 使用UTC时区来确保一致性
                String formattedDate = formatter.format(value.toInstant().atZone(java.time.ZoneId.of("UTC")).toLocalDateTime());
                gen.writeString(formattedDate);
            }
        }
    }
    
    /**
     * ScheduleItem自定义序列化器，处理repeatKeys字段将JSON数组字符串转换为实际的JSON数组
     */
    public static class ScheduleItemSerializer extends StdSerializer<ScheduleItem> {
        
        private final ObjectMapper objectMapper;
        
        public ScheduleItemSerializer(ObjectMapper objectMapper) {
            super(ScheduleItem.class);
            this.objectMapper = objectMapper;
        }
        
        @Override
        public void serialize(ScheduleItem value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            
            // 序列化其他字段
            gen.writeStringField("id", String.valueOf(value.getId()));
            gen.writeStringField("itemTitle", value.getItemTitle());
            gen.writeStringField("itemDesc", value.getItemDesc());
            gen.writeStringField("location", value.getLocation());
            gen.writeStringField("repeatType", value.getRepeatType());
            
            // 处理repeatKeys字段，将JSON数组字符串转换为实际的JSON数组
            if (value.getRepeatKeys() != null && !value.getRepeatKeys().isEmpty()) {
                try {
                    JsonNode node = objectMapper.readTree(value.getRepeatKeys());
                    gen.writeFieldName("repeatKeys");
                    gen.writeTree(node);
                } catch (Exception e) {
                    // 如果解析失败，仍然按字符串处理
                    gen.writeStringField("repeatKeys", value.getRepeatKeys());
                }
            } else {
                gen.writeNullField("repeatKeys");
            }
            
            // 序列化日期字段 - 使用UTC时区确保一致性
            if (value.getRepeatStartDay() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
                String formattedDate = formatter.format(value.getRepeatStartDay().toInstant().atZone(java.time.ZoneId.of("UTC")).toLocalDateTime());
                gen.writeStringField("repeatStartDay", formattedDate);
            } else {
                gen.writeNullField("repeatStartDay");
            }
            
            if (value.getRepeatEndDay() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
                String formattedDate = formatter.format(value.getRepeatEndDay().toInstant().atZone(java.time.ZoneId.of("UTC")).toLocalDateTime());
                gen.writeStringField("repeatEndDay", formattedDate);
            } else {
                gen.writeNullField("repeatEndDay");
            }
            
            gen.writeStringField("itemType", value.getItemType());
            
            if (value.getStartTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
                String formattedDate = formatter.format(value.getStartTime().toInstant().atZone(java.time.ZoneId.of("UTC")).toLocalDateTime());
                gen.writeStringField("startTime", formattedDate);
            } else {
                gen.writeNullField("startTime");
            }
            
            if (value.getEndTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
                String formattedDate = formatter.format(value.getEndTime().toInstant().atZone(java.time.ZoneId.of("UTC")).toLocalDateTime());
                gen.writeStringField("endTime", formattedDate);
            } else {
                gen.writeNullField("endTime");
            }
            
            
            gen.writeStringField("userId", String.valueOf(value.getUserId()));
            gen.writeStringField("groupId", String.valueOf(value.getGroupId()));
            
            gen.writeEndObject();
        }
    }
}