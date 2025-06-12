package com.aireader.backend.repository;

import com.aireader.backend.model.jpa.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 系统配置仓库
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    
    /**
     * 根据配置键查找配置
     * 
     * @param key 配置键
     * @return 可选的系统配置
     */
    Optional<SystemConfig> findByKey(String key);
} 