package com.funfun.schedule.mapper;

import com.funfun.schedule.dto.UniversalRecordDTO;
import com.funfun.schedule.entity.UniversalRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UniversalRecordMapper extends BaseMapper{
    @Mapping(source = "extra", target = "extra", qualifiedByName = "stringToJson")
    UniversalRecordDTO toDTO(UniversalRecord item);
    List<UniversalRecordDTO> toDTOList(List<UniversalRecord> item);

    @Mapping(source = "extra", target = "extra", qualifiedByName = "jsonToString")
    UniversalRecord toEntity(  UniversalRecordDTO item);
}
