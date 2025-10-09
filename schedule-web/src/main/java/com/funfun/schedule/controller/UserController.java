package com.funfun.schedule.controller;

import com.funfun.schedule.entity.User;
import com.funfun.schedule.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * UserController类，提供用户相关的RESTful API接口
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * 根据openid查询用户
     */
    @GetMapping("/openid/{openid}")
    public ResponseEntity<User> getUserByOpenid(@PathVariable String openid) {
        User user = userService.getUserByOpenid(openid);
        return ResponseEntity.ok(user);
    }

    /**
     * 根据手机号查询用户
     */
    @GetMapping("/phone/{phone}")
    public ResponseEntity<User> getUserByPhone(@PathVariable String phone) {
        User user = userService.getUserByPhone(phone);
        return ResponseEntity.ok(user);
    }

    /**
     * 查询所有用户
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * 根据状态查询用户
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<User>> getUsersByStatus(@PathVariable Integer status) {
        List<User> users = userService.getUsersByStatus(status);
        return ResponseEntity.ok(users);
    }

    /**
     * 根据注册时间范围查询用户
     */
    @GetMapping("/register-time")
    public ResponseEntity<List<User>> getUsersByRegisterTimeBetween(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<User> users = userService.getUsersByRegisterTimeBetween(startDate, endDate);
        return ResponseEntity.ok(users);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        User updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteUsers(@RequestBody List<Long> ids) {
        userService.deleteUsers(ids);
        return ResponseEntity.noContent().build();
    }

    /**
     * 检查用户是否存在
     */
    @GetMapping("/exists/openid/{openid}")
    public ResponseEntity<Boolean> existsByOpenid(@PathVariable String openid) {
        boolean exists = userService.existsByOpenid(openid);
        return ResponseEntity.ok(exists);
    }

    /**
     * 检查手机号是否已被使用
     */
    @GetMapping("/exists/phone/{phone}")
    public ResponseEntity<Boolean> existsByPhone(@PathVariable String phone) {
        boolean exists = userService.existsByPhone(phone);
        return ResponseEntity.ok(exists);
    }

    /**
     * 批量查询用户
     */
    @PostMapping("/batch")
    public ResponseEntity<List<User>> getUsersByIds(@RequestBody List<Long> ids) {
        List<User> users = userService.getUsersByIds(ids);
        return ResponseEntity.ok(users);
    }
}