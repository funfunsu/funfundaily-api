package com.funfun.schedule;

import com.funfun.schedule.dto.GroupDTO;
import com.funfun.schedule.entity.Group;
import com.funfun.schedule.mapper.GroupMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for GroupMapper
 * Verifies that the mapper is correctly registered as a Spring bean and works as expected
 */
@SpringBootTest
public class GroupMapperTest {

    @Autowired
    private GroupMapper groupMapper;

    @Test
    public void testMapperBeanIsAutowired() {
        // Verify that the mapper bean is successfully autowired
        assertNotNull(groupMapper, "GroupMapper should be autowired");
    }

    @Test
    public void testToDTOConversion() {
        // Create a test entity
        Group entity = new Group();
        entity.setId(1L);
        entity.setGroupName("Test Group");
        entity.setGroupDesc("Test Group Description");
        entity.setCreateTime(new Date());
        entity.setCreator(1L);

        // Convert to DTO
        GroupDTO dto = groupMapper.toDTO(entity);

        // Verify the conversion
        assertNotNull(dto, "DTO should not be null");
        assertEquals(entity.getId(), dto.getId(), "ID should be mapped correctly");
        assertEquals(entity.getGroupName(), dto.getGroupName(), "Group name should be mapped correctly");
        assertEquals(entity.getGroupDesc(), dto.getGroupDesc(), "Group description should be mapped correctly");
    }

    @Test
    public void testToEntityConversion() {
        // Create a test DTO
        GroupDTO dto = new GroupDTO();
        dto.setId(1L);
        dto.setGroupName("Test Group");
        dto.setGroupDesc("Test Group Description");
        dto.setCreateTime(new Date());
        dto.setCreator(1L);

        // Convert to entity
        Group entity = groupMapper.toEntity(dto);

        // Verify the conversion
        assertNotNull(entity, "Entity should not be null");
        assertEquals(dto.getId(), entity.getId(), "ID should be mapped correctly");
        assertEquals(dto.getGroupName(), entity.getGroupName(), "Group name should be mapped correctly");
        assertEquals(dto.getGroupDesc(), entity.getGroupDesc(), "Group description should be mapped correctly");
    }

    @Test
    public void testListConversion() {
        // Create test entities
        Group entity1 = new Group();
        entity1.setId(1L);
        entity1.setGroupName("Test Group 1");

        Group entity2 = new Group();
        entity2.setId(2L);
        entity2.setGroupName("Test Group 2");

        List<Group> entities = Arrays.asList(entity1, entity2);

        // Convert list to DTOs
        List<GroupDTO> dtos = groupMapper.toDTOList(entities);

        // Verify the list conversion
        assertNotNull(dtos, "DTO list should not be null");
        assertEquals(entities.size(), dtos.size(), "List size should match");
        assertEquals(entities.get(0).getGroupName(), dtos.get(0).getGroupName(), "First group name should match");
        assertEquals(entities.get(1).getGroupName(), dtos.get(1).getGroupName(), "Second group name should match");
    }
}