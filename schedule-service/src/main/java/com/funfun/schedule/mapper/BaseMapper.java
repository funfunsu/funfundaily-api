package com.funfun.schedule.mapper;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.mapstruct.Named;

import java.util.Map;

public interface BaseMapper {
    /**
     * 将字符串转换为列表
     * 假设字符串格式为逗号分隔，如 "1,2,3"
     * @param value 字符串值
     * @return 转换后的列表
     */
    @Named("stringToMap")
    default Map<String,Object> stringToMap(String value) {
        if (value == null || value.isBlank()){
            return null;
        }
        return JSON.parseObject(value, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * 将列表转换为字符串
     * @param map 字符串列表
     * @return 转换后的字符串
     */
    @Named("mapToString")
    default String mapToString(Map<String,Object> map) {
        if (map == null){
            return null;
        }
        return JSON.toJSONString(map);
    }
}
