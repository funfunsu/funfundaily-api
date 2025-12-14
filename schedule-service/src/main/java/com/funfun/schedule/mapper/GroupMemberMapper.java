package com.funfun.schedule.mapper;


import com.funfun.schedule.dto.GroupMemberDTO;
import com.funfun.schedule.dto.UserDTO;
import com.funfun.schedule.dto.UserInfoDTO;
import com.funfun.schedule.entity.GroupMember;
import com.funfun.schedule.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupMemberMapper extends BaseMapper{
    /**
     * 将Group实体转换为GroupDTO
     * @return 群组DTO
     */
    GroupMemberDTO toDTO(GroupMember user);
    List<GroupMemberDTO>toDTOList(List<GroupMember> user);


}
