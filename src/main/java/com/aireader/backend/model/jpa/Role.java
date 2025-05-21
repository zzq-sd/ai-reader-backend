package com.aireader.backend.model.jpa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * 角色实体类
 */
@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", length = 20, unique = true, nullable = false)
    private RoleName name;

    public enum RoleName {
        ROLE_USER,
        ROLE_ADMIN
    }
} 