package com.funfun.schedule.mapper;

import com.funfun.schedule.entity.Group;
import com.funfun.schedule.dto.GroupDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 群组实体转换Mapper
 * 使用MapStruct简化对象之间的转换
 */
@Mapper(componentModel = "spring")
public interface GroupMapper {

    // 用于直接调用，也支持Spring自动注入
    GroupMapper INSTANCE = Mappers.getMapper(GroupMapper.class);

    /**
     * 将Group实体转换为GroupDTO
     * @param group 群组实体
     * @return 群组DTO
     */
    GroupDTO toDTO(Group group);

    /**
     * 将GroupDTO转换为Group实体
     * @param groupDTO 群组DTO
     * @return 群组实体
     */
    Group toEntity(GroupDTO groupDTO);

    /**
     * 将Group实体列表转换为GroupDTO列表
     * @param groups 群组实体列表
     * @return 群组DTO列表
     */
    List<GroupDTO> toDTOList(List<Group> groups);

    /**
     * 将GroupDTO列表转换为Group实体列表
     * @param groupDTOs 群组DTO列表
     * @return 群组实体列表
     */
    List<Group> toEntityList(List<GroupDTO> groupDTOs);
}