package com.aireader.backend.service.impl;

import com.aireader.backend.dto.auth.UserRegistrationRequestDto;
import com.aireader.backend.dto.auth.UserResponseDto;
import com.aireader.backend.model.jpa.Role;
import com.aireader.backend.model.jpa.User;
import com.aireader.backend.repository.RoleRepository;
import com.aireader.backend.repository.UserRepository;
import com.aireader.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 注册新用户
     * 
     * @param registrationRequest 用户注册请求
     * @return 注册成功的用户信息
     */
    @Override
    @Transactional
    public UserResponseDto registerUser(UserRegistrationRequestDto registrationRequest) {
        // 检查用户名和邮箱是否已存在
        if (userRepository.existsByUsername(registrationRequest.getUsername())) {
            throw new RuntimeException("用户名已被使用");
        }
        
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setEmail(registrationRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setFullName(registrationRequest.getFullName());
        user.setEnabled(true);
        user.setLocked(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // 分配默认用户角色
        Role userRole = roleRepository.findByName(com.aireader.backend.model.constant.UserRole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("默认角色不存在"));
        user.setRoles(Collections.singleton(userRole));
        
        // 保存用户
        User savedUser = userRepository.save(user);
        
        // 返回用户响应DTO
        return convertToDto(savedUser);
    }
    
    /**
     * 根据用户名或邮箱查找用户
     * 
     * @param usernameOrEmail 用户名或邮箱
     * @return 可选的用户对象
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
    }
    
    /**
     * 根据ID查找用户
     * 
     * @param id 用户ID
     * @return 可选的用户对象
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
    
    /**
     * 获取所有用户列表
     * 
     * @return 用户列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 将用户转换为响应DTO
     * 
     * @param user 用户实体
     * @return 用户响应DTO
     */
    @Override
    public UserResponseDto convertToDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().toString())
                        .collect(Collectors.toList()))
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    /**
     * 更新用户信息
     * 
     * @param id 用户ID
     * @param userDetails 用户详细信息
     * @return 更新后的用户信息
     */
    @Override
    @Transactional
    public UserResponseDto updateUser(String id, UserRegistrationRequestDto userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 如果要更改用户名，检查是否已存在
        if (!user.getUsername().equals(userDetails.getUsername()) && 
                userRepository.existsByUsername(userDetails.getUsername())) {
            throw new RuntimeException("用户名已被使用");
        }
        
        // 如果要更改邮箱，检查是否已存在
        if (!user.getEmail().equals(userDetails.getEmail()) && 
                userRepository.existsByEmail(userDetails.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 更新用户信息
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setFullName(userDetails.getFullName());
        
        // 如果提供了新密码，则更新密码
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(userDetails.getPassword()));
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        
        // 保存更新后的用户
        User updatedUser = userRepository.save(user);
        
        // 返回更新后的用户响应DTO
        return convertToDto(updatedUser);
    }
    
    /**
     * 删除用户
     * 
     * @param id 用户ID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteUser(String id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    /**
     * 检查用户名是否已存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * 检查邮箱是否已存在
     * 
     * @param email 邮箱
     * @return 是否存在
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
} 