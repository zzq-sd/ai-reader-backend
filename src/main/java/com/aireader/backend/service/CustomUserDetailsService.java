package com.aireader.backend.service;

import com.aireader.backend.model.jpa.User;
import com.aireader.backend.model.security.CustomUserDetails;
import com.aireader.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义用户详情服务
 * 实现Spring Security的UserDetailsService接口
 * 用于根据用户名加载用户详细信息
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 根据用户名或邮箱加载用户
     * 
     * @param usernameOrEmail 用户名或邮箱
     * @return UserDetails对象
     * @throws UsernameNotFoundException 如果用户不存在
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // 尝试通过用户名或邮箱查找用户
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("找不到用户: " + usernameOrEmail));
        
        // 将用户角色转换为Spring Security的GrantedAuthority
        // 注意：UserRole枚举值已经包含ROLE_前缀（如ROLE_USER），不需要再次添加
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().toString()))
                .collect(Collectors.toList());
        
        // 返回自定义的UserDetails对象
        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                !user.isLocked(), // accountNonLocked
                authorities);
    }
} 