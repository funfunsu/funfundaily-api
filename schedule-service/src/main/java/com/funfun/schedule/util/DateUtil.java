package com.funfun.schedule.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.IsoFields;

public class DateUtil {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    /**
     * 获取给定日期是一年的第几周 (ISO 8601标准)
     * 周一为一周的开始。
     * 一年的第一周是包含该年第一个星期四的那一周。
     *
     * @param localDate 要计算周数的日期
     * @return 一年中的第几周
     */
    public static int getWeekOfYear(LocalDate localDate) {
        // 使用 Java 8 时间 API 的 IsoFields.WEEK_OF_WEEK_BASED_YEAR 直接获取
        // 这是计算 ISO 8601 标准周数最直接和推荐的方式
        return localDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }
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
    public static LocalDate parse(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null; // 或抛出异常
        }

        return LocalDate.parse(dateString);
    }

}