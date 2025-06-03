-- 角色表初始化
INSERT INTO roles (id, name) VALUES (1, 'ROLE_USER') ON DUPLICATE KEY UPDATE name = 'ROLE_USER';
INSERT INTO roles (id, name) VALUES (2, 'ROLE_ADMIN') ON DUPLICATE KEY UPDATE name = 'ROLE_ADMIN';

-- 默认管理员用户 (密码: admin123)，仅供开发测试使用，生产环境中请移除
-- 注意：passwordHash值为BCrypt加密后的结果
INSERT INTO users (id, username, email, password_hash, enabled, locked, created_at, updated_at)
SELECT 
  UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')), 
  'admin', 
  'admin@example.com', 
  '$2a$10$EqYTrQ8FTnGG0GxR/mq.PeTi5UKRWFw3HxGBl/9K1BScpPphUdPru', 
  1, 
  0,
  NOW(), 
  NOW() 
FROM dual 
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- 为管理员用户添加角色
INSERT INTO user_roles (user_id, role_id)
SELECT 
  UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')), 
  2
FROM dual
WHERE NOT EXISTS (
  SELECT 1 FROM user_roles 
  WHERE user_id = UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440000', '-', '')) AND role_id = 2
);

-- 为article_metadata表添加读取时间和分类字段
ALTER TABLE article_metadata
ADD COLUMN reading_time_minutes INT DEFAULT NULL COMMENT '文章阅读时间（分钟）',
ADD COLUMN category VARCHAR(100) DEFAULT NULL COMMENT '文章分类';

-- 为zzq用户添加管理员角色（如果zzq用户存在的话）
INSERT INTO user_roles (user_id, role_id)
SELECT 
  (SELECT id FROM users WHERE username = 'zzq' LIMIT 1),
  2
FROM dual
WHERE EXISTS (SELECT 1 FROM users WHERE username = 'zzq')
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    JOIN users u ON ur.user_id = u.id
    WHERE u.username = 'zzq' AND ur.role_id = 2
  );

-- 在这里可以添加其他表结构更新 