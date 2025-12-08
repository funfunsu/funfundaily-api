package com.funfun.schedule.mapper;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.funfun.schedule.dto.CheckinRecordDTO;
import com.funfun.schedule.entity.CheckinRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface CheckinRecordMapper {
    /**
     * 将Group实体转换为GroupDTO
     * @return 群组DTO
     */
    @Mapping(source = "extra", target = "extra", qualifiedByName = "stringToMap")
    CheckinRecordDTO toDTO(CheckinRecord record);
    List<CheckinRecordDTO> toDTOList(List<CheckinRecord> record);

    @Mapping(source = "extra", target = "extra", qualifiedByName = "mapToString")
    CheckinRecord toEntity(CheckinRecordDTO record);


    /**
     * 将字符串转换为列表
     * 假设字符串格式为逗号分隔，如 "1,2,3"
     * @param value 字符串值
     * @return 转换后的列表
     */
    @Named("stringToMap")
    default Map<String,Object> stringToMap(String value) {
        return JSON.parseObject(value, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * 将列表转换为字符串
     * @param map 字符串列表
     * @return 转换后的字符串
     */
    @Named("mapToString")
    default String mapToString(Map<String,Object> map) {
        return JSON.toJSONString(map);
    }

}
