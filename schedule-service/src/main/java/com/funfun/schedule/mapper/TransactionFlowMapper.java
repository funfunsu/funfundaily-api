package com.funfun.schedule.mapper;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.TransactionFlowDTO;
import com.funfun.schedule.entity.TransactionFlow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionFlowMapper {

    @Mapping(source = "extra", target = "extra", qualifiedByName = "stringToJsonObject")
    TransactionFlowDTO toDTO(TransactionFlow entity);

    List<TransactionFlowDTO> toDTOList(List<TransactionFlow> list);

    @Mapping(source = "extra", target = "extra", qualifiedByName = "jsonObjectToString")
    TransactionFlow toEntity(TransactionFlowDTO dto);

    @Named("stringToJsonObject")
    static JSONObject stringToJsonObject(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            return JSON.parseObject(text);
        } catch (Exception e) {
            return null;
        }
    }

    @Named("jsonObjectToString")
    static String jsonObjectToString(JSONObject obj) {
        return obj == null ? null : obj.toJSONString();
    }
}
