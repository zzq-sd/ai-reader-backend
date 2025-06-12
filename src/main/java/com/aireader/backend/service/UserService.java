package com.aireader.backend.service;

import com.aireader.backend.dto.auth.UserRegistrationRequestDto;
import com.aireader.backend.dto.auth.UserResponseDto;
import com.aireader.backend.model.jpa.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 * 提供用户相关的业务逻辑
 */
public interface UserService {
    
    /**
     * 注册新用户
     * 
     * @param registrationRequest 用户注册请求
     * @return 注册成功的用户信息
     */
    UserResponseDto registerUser(UserRegistrationRequestDto registrationRequest);
    
    /**
     * 根据用户名或邮箱查找用户
     * 
     * @param usernameOrEmail 用户名或邮箱
     * @return 可选的用户对象
     */
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);
    
    /**
     * 根据ID查找用户
     * 
     * @param id 用户ID
     * @return 可选的用户对象
     */
    Optional<User> findById(String id);
    
    /**
     * 获取所有用户列表
     * 
     * @return 用户列表
     */
    List<UserResponseDto> getAllUsers();
    
    /**
     * 将用户转换为响应DTO
     * 
     * @param user 用户实体
     * @return 用户响应DTO
     */
    UserResponseDto convertToDto(User user);
    
    /**
     * 更新用户信息
     * 
     * @param id 用户ID
     * @param userDetails 用户详细信息
     * @return 更新后的用户信息
     */
    UserResponseDto updateUser(String id, UserRegistrationRequestDto userDetails);
    
    /**
     * 删除用户
     * 
     * @param id 用户ID
     * @return 是否删除成功
     */
    boolean deleteUser(String id);
    
    /**
     * 检查用户名是否已存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否已存在
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);
} 