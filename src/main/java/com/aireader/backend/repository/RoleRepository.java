package com.aireader.backend.repository;

import com.aireader.backend.model.jpa.Role;
import com.aireader.backend.model.constant.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色信息仓库接口
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * 根据角色名称查找角色
     * 
     * @param name 角色名称
     * @return 角色对象
     */
    Optional<Role> findByName(UserRole name);
} 