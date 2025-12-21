package com.funfun.schedule;

import com.funfun.schedule.mapper.ScheduleItemMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for ScheduleItemMapper
 * Verifies that the mapper is correctly registered as a Spring bean and works as expected
 */
@SpringBootTest
public class ScheduleItemMapperTest {

    @Autowired
    private ScheduleItemMapper scheduleItemMapper;

    @Test
    public void testMapperBeanIsAutowired() {
        // Verify that the mapper bean is successfully autowired
        assertNotNull(scheduleItemMapper, "ScheduleItemMapper should be autowired");
    }

}