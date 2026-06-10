package com.funfun.schedule.service.impl;

import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.UserInfoDTO;
import com.funfun.schedule.entity.Group;
import com.funfun.schedule.entity.GroupMember;
import com.funfun.schedule.entity.User;
import com.funfun.schedule.enums.GroupRole;
import com.funfun.schedule.enums.GroupType;
import com.funfun.schedule.mapper.UserMapper;
import com.funfun.schedule.repository.GroupMemberRepository;
import com.funfun.schedule.repository.ScheduleGroupRepository;
import com.funfun.schedule.repository.UserRepository;
import com.funfun.schedule.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * UserService接口的实现类，实现用户相关的业务逻辑
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleGroupRepository scheduleGroupRepository;
    @Autowired
    private GroupMemberRepository groupMemberRepository;


    @Autowired
    private UserMapper userMapper;


    @Override
    @Transactional
    public User createUser(User user) {
        // 检查openid是否已存在
        if (userRepository.existsByOpenid(user.getOpenid())) {
            throw new RuntimeException("该openid已被注册");
        }
        // 检查手机号是否已被使用（如果手机号不为空）
        if (user.getPhone() != null && userRepository.existsByPhone(user.getPhone())) {
            throw new RuntimeException("该手机号已被使用");
        }
        // 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(1); // 默认为正常状态
        }
        if (user.getLanguage() == null) {
            user.setLanguage("zh_CN"); // 默认为中文简体
        }
        if (user.getRegisterTime() == null) {
            user.setRegisterTime(new Date());
        }
        return userRepository.save(user);
    }

    @Override
    public User createUserByNickname(String nickname) {
        User user = new User();
        user.setNickname(nickname);
        user.setInviterId(UserContext.getUserId());
        user.setOpenid("");
        return userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        return optionalUser.orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    public User getUserByOpenid(String openid) {
        Optional<User> optionalUser = userRepository.findByOpenid(openid);
        return optionalUser.orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    public User getUserByPhone(String phone) {
        Optional<User> optionalUser = userRepository.findByPhone(phone);
        return optionalUser.orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> getUsersByStatus(Integer status) {
        return userRepository.findByStatus(status);
    }

    @Override
    public List<User> getUsersByRegisterTimeBetween(Date startDate, Date endDate) {
        return userRepository.findByRegisterTimeBetween(startDate, endDate);
    }

    @Override
    @Transactional
    public UserInfoDTO updateUserBaseInfo(UserInfoDTO user) {
        // 检查用户是否存在
        User existingUser = getUserById(user.getId());
        // 更新用户信息
        existingUser.setNickname(user.getNickname());
        existingUser.setAvatarUrl(user.getAvatarUrl());
        existingUser =   userRepository.save(existingUser);
        return userMapper.toSimpleDTO(existingUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        // 逻辑删除用户
        User user = getUserById(id);
        user.setDeleted(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUsers(List<Long> ids) {
        // 批量逻辑删除用户
        List<User> users = userRepository.findByIdIn(ids);
        for (User user : users) {
            user.setDeleted(true);
        }
        userRepository.saveAll(users);
    }

    @Override
    public boolean existsByOpenid(String openid) {
        return userRepository.existsByOpenid(openid);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Override
    public List<User> getUsersByIds(List<Long> ids) {
        return userRepository.findByIdIn(ids);
    }

    @Override
    @Transactional
    public void bindOpenidToPlaceholder(Long placeholderUserId) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new RuntimeException("当前未登录");
        }
        if (currentUserId.equals(placeholderUserId)) {
            throw new RuntimeException("不能将自己绑定到自己");
        }
        User recipient = getUserById(currentUserId);
        User placeholder = getUserById(placeholderUserId);
        if (recipient.getOpenid() == null || recipient.getOpenid().isBlank()) {
            throw new RuntimeException("当前账号未绑定微信，无法用于绑定");
        }
        if (placeholder.getOpenid() != null && !placeholder.getOpenid().isBlank()) {
            throw new RuntimeException("目标成员已绑定微信，无法重复绑定");
        }
        String openid = recipient.getOpenid();
        String unionid = recipient.getUnionid();
        // 先清掉 recipient 的 openid，避免索引/唯一约束冲突；占位用户对外用 "" 表示未绑定。
        recipient.setOpenid("");
        recipient.setUnionid(null);
        userRepository.saveAndFlush(recipient);
        placeholder.setOpenid(openid);
        if (unionid != null && !unionid.isBlank()) {
            placeholder.setUnionid(unionid);
        }
        placeholder.setLastLoginTime(new Date());
        userRepository.save(placeholder);
        log.info("绑定 openid 完成：原 wx userId={}，占位 userId={}", currentUserId, placeholderUserId);
    }

    @Override
    @Transactional
    public UserInfoDTO updateUnboundUserNickname(Long targetUserId, String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new RuntimeException("昵称不能为空");
        }
        User target = getUserById(targetUserId);
        if (target.getOpenid() != null && !target.getOpenid().isBlank()) {
            throw new RuntimeException("该成员已绑定微信，请其本人修改昵称");
        }
        target.setNickname(nickname.trim());
        User saved = userRepository.save(target);
        return userMapper.toSimpleDTO(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long getOrCreateUserIdByOpenId(String openId,Long invitorId) {
        // 1. 根据 openId 查询已有用户
        return userRepository.findByOpenid(openId)
                // 2. 存在：直接返回本地 userId
                .map(User::getId)
                // 3. 不存在：自动注册新用户，返回新生成的 userId
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setOpenid(openId);
                    newUser.setInviterId(invitorId);
                    // 可选：后续可通过微信接口获取昵称、头像，补充到这里
                    // newUser.setNickname("微信用户");
                    // newUser.setAvatarUrl("默认头像地址");

                    User savedUser = userRepository.save(newUser);
                    return savedUser.getId();
                });
    }
}