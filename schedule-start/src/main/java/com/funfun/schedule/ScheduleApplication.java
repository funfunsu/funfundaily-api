package com.funfun.schedule;

import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.TypeDef;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@TypeDef(name = "json", typeClass = JsonType.class)
@SpringBootApplication
public class ScheduleApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));

        SpringApplication.run(ScheduleApplication.class, args);
    }

}
