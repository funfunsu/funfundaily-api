package com.funfun.schedule.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    /**
     * 返回一天的起始时间 (yyyy-MM-dd 00:00:00)
     * @param date 给定的日期时间
     * @return 该天的开始时间 (LocalDateTime)
     */
    public static LocalDateTime getStartOfDay(LocalDateTime date) {
        if (date == null) {
            return null; // 或者抛出异常，取决于你的业务需求
        }
        return date.toLocalDate().atStartOfDay(); // atStartOfDay() 返回当天时间 00:00:00
    }

    /**
     * 返回一天的结束时间 (yyyy-MM-dd 23:59:59)
     * @param date 给定的日期时间
     * @return 该天的结束时间 (LocalDateTime)
     */
    public static LocalDateTime getEndOfDay(LocalDateTime date) {
        if (date == null) {
            return null; // 或者抛出异常，取决于你的业务需求
        }
        // 使用 LocalTime.MAX (23:59:59.999999999) 作为当天的最后一刻通常更安全和标准
        // 如果你明确需要 23:59:59，可以使用: return date.toLocalDate().atTime(23, 59, 59);
        return date.toLocalDate().atTime(LocalTime.MAX);
    }

    /**
     * 将 LocalDateTime 对象格式化为 "yyyy-MM-dd" 格式的字符串。
     *
     * @param localDateTime 要转换的 LocalDateTime 对象
     * @return 格式化后的字符串，如果输入为 null 则返回 null
     */
    public static String formatToLocalDateStr(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null; // 或者根据需要返回默认值，例如 ""
        }
        // 定义格式器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 使用格式器格式化 LocalDateTime
        return localDateTime.format(formatter);
    }
}