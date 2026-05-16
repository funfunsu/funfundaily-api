package com.funfun.schedule.mapper;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.ScheduleItemUpdateScope;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.dto.ScheduleItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 日程项实体转换Mapper
 * 使用MapStruct简化对象之间的转换
 */
@Mapper(componentModel = "spring")
public interface ScheduleItemMapper extends BaseMapper{

    // 用于直接调用，也支持Spring自动注入
    ScheduleItemMapper INSTANCE = Mappers.getMapper(ScheduleItemMapper.class);

    /**
     * 将ScheduleItem实体转换为ScheduleItemDTO
     * @param scheduleItem 日程项实体
     * @return 日程项DTO
     */
    @Mapping(source = "repeatKeys", target = "repeatKeys", qualifiedByName = "stringToList")
    @Mapping(source = "extra", target = "extra", qualifiedByName = "stringToJson")
    @Mapping(source = "updateScope", target = "updateScope", qualifiedByName = "stringToScope")
    ScheduleItemDTO toDTO(ScheduleItem scheduleItem);

    /**
     * 将ScheduleItemDTO转换为ScheduleItem实体
     * @param scheduleItemDTO 日程项DTO
     * @return 日程项实体
     */
    @Mapping(source = "repeatKeys", target = "repeatKeys", qualifiedByName = "listToString")
    @Mapping(target = "userId",ignore = true)
    @Mapping(target = "groupId",ignore = true)
    @Mapping(source = "extra", target = "extra", qualifiedByName = "jsonToString")
    @Mapping(source = "updateScope", target = "updateScope", qualifiedByName = "scopeToString")
    ScheduleItem toEntity(ScheduleItemDTO scheduleItemDTO);

    /**
     * 将ScheduleItem实体列表转换为ScheduleItemDTO列表
     * @param scheduleItems 日程项实体列表
     * @return 日程项DTO列表
     */
    List<ScheduleItemDTO> toDTOList(List<ScheduleItem> scheduleItems);

    /**
     * 将ScheduleItemDTO列表转换为ScheduleItem实体列表
     * @param scheduleItemDTOs 日程项DTO列表
     * @return 日程项实体列表
     */
    List<ScheduleItem> toEntityList(List<ScheduleItemDTO> scheduleItemDTOs);


    ScheduleItemDTO copy(ScheduleItemDTO source);
    /**
     * 将字符串转换为列表
     * 假设字符串格式为逗号分隔，如 "1,2,3"
     * @param value 字符串值
     * @return 转换后的列表
     */
    @Named("stringToList")
    default List<String> stringToList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 将列表转换为字符串
     * 转换为逗号分隔的字符串，如 "1,2,3"
     * @param list 字符串列表
     * @return 转换后的字符串
     */
    @Named("listToString")
    default String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.joining(","));
    }

    /**
     * 将列表转换为字符串
     * @param map 字符串列表
     * @return 转换后的字符串
     */
    @Named("scopeToString")
    default String scopeToString(ScheduleItemUpdateScope map) {
        if (map == null){
            return null;
        }
        return JSON.toJSONString(map);
    }

    /**
     * 将列表转换为字符串
     * @param str 字符串列表
     * @return 转换后的字符串
     */
    @Named("stringToScope")
    default ScheduleItemUpdateScope stringToScope(String str) {
        if (str == null){
            return null;
        }
        return JSON.parseObject(str,ScheduleItemUpdateScope.class);
    }
}