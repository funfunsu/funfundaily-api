//package com.funfun.schedule.config;
//
//import com.alibaba.fastjson2.JSONFactory;
//import org.springframework.context.annotation.Configuration;
//
//import jakarta.annotation.PostConstruct;
//import java.text.SimpleDateFormat;
//import java.util.Locale;
//import java.util.TimeZone;
//
//@Configuration
//public class Fastjson2Config {
//
//    @PostConstruct
//    public void configureFastjson2DateFormat() {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
//        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8")); // 设置为您期望的时区
//        JSONFactory.getDefaultObjectReaderProvider().setDateFormat(sdf);
//    }
//}