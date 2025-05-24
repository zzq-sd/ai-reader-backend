package com.aireader.backend.repository;

import com.aireader.backend.model.jpa.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户信息仓库接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户对象
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     *
     * @param email 邮箱
     * @return 用户对象
     */
    Optional<User> findByEmail(String email);

    /**
     * 通过用户名或邮箱查找用户
     *
     * @param username 用户名
     * @param email 邮箱
     * @return 用户对象的Optional包装
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @return 存在返回true，否则返回false
     */
    boolean existsByEmail(String email);
} 