package com.funfun.schedule.mapper;

import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.entity.ScheduleItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    public void testToDTOConversion() {
        // Create a test entity
        ScheduleItem entity = new ScheduleItem();
        entity.setId(1L);
        entity.setItemTitle("Test Title");
        entity.setItemDesc("Test Description");
        entity.setItemType("meeting");
        entity.setStartTime(new Date());
        entity.setEndTime(new Date());

        // Convert to DTO
        ScheduleItemDTO dto = scheduleItemMapper.toDTO(entity);

        // Verify the conversion
        assertNotNull(dto, "DTO should not be null");
        assertEquals(entity.getId(), dto.getId(), "ID should be mapped correctly");
        assertEquals(entity.getItemTitle(), dto.getItemTitle(), "Title should be mapped correctly");
        assertEquals(entity.getItemDesc(), dto.getItemDesc(), "Description should be mapped correctly");
    }

    @Test
    public void testToEntityConversion() {
        // Create a test DTO
        ScheduleItemDTO dto = new ScheduleItemDTO();
        dto.setId(1L);
        dto.setItemTitle("Test Title");
        dto.setItemDesc("Test Description");
        dto.setItemType("meeting");
        dto.setStartTime(new Date());
        dto.setEndTime(new Date());

        // Convert to entity
        ScheduleItem entity = scheduleItemMapper.toEntity(dto);

        // Verify the conversion
        assertNotNull(entity, "Entity should not be null");
        assertEquals(dto.getId(), entity.getId(), "ID should be mapped correctly");
        assertEquals(dto.getItemTitle(), entity.getItemTitle(), "Title should be mapped correctly");
        assertEquals(dto.getItemDesc(), entity.getItemDesc(), "Description should be mapped correctly");
    }

    @Test
    public void testListConversion() {
        // Create test entities
        ScheduleItem entity1 = new ScheduleItem();
        entity1.setId(1L);
        entity1.setItemTitle("Test Title 1");

        ScheduleItem entity2 = new ScheduleItem();
        entity2.setId(2L);
        entity2.setItemTitle("Test Title 2");

        List<ScheduleItem> entities = Arrays.asList(entity1, entity2);

        // Convert list to DTOs
        List<ScheduleItemDTO> dtos = scheduleItemMapper.toDTOList(entities);

        // Verify the list conversion
        assertNotNull(dtos, "DTO list should not be null");
        assertEquals(entities.size(), dtos.size(), "List size should match");
        assertEquals(entities.get(0).getItemTitle(), dtos.get(0).getItemTitle(), "First item title should match");
        assertEquals(entities.get(1).getItemTitle(), dtos.get(1).getItemTitle(), "Second item title should match");
    }
}