package com.funfun.schedule.mapper;

import com.funfun.schedule.dto.UniversalRecordDTO;
import com.funfun.schedule.entity.UniversalRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UniversalRecordMapper extends BaseMapper{
    @Mapping(source = "extra", target = "extra", qualifiedByName = "stringToJson")
    @Mapping(source = "content", target = "content", qualifiedByName = "stringToJson")
    UniversalRecordDTO toDTO(UniversalRecord item);

    @Mapping(source = "extra", target = "extra", qualifiedByName = "jsonToString")
    @Mapping(source = "content", target = "content", qualifiedByName = "jsonToString")
    UniversalRecord toEntity(  UniversalRecordDTO item);
}
