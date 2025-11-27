package com.funfun.schedule.mapper;


import com.funfun.schedule.dto.UserDTO;
import com.funfun.schedule.dto.UserInfoDTO;
import com.funfun.schedule.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    /**
     * 将Group实体转换为GroupDTO
     * @param group 群组实体
     * @return 群组DTO
     */
    UserDTO toDTO(User user);

    UserInfoDTO toSimpleDTO(User user);
    List<UserInfoDTO> toSimpleList(List<User> user);


}
