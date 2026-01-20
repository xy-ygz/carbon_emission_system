-- ============================================
-- 碳排放管理系统数据库建表SQL
-- 用于 Docker Compose 初始化数据库
-- 
-- 本文件包含系统初始化所需的所有配置：
-- 1. 所有数据库表结构
-- 2. 所有权限配置（菜单权限和API权限），维持正确的树形结构
-- 3. 4个默认角色：超级管理员、管理员、普通用户、游客
-- 4. 正确的角色权限分配
-- ============================================

-- 设置字符集
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET CHARACTER SET utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

USE `sys_carbon`;

-- 1. 用户表 (user)
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) DEFAULT NULL COMMENT '姓名',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名/账号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `department` VARCHAR(100) DEFAULT NULL COMMENT '部门',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系方式',
    `salt` VARCHAR(50) DEFAULT NULL COMMENT '盐值',
    `status` INT NOT NULL DEFAULT 0 COMMENT '身份标识(0后勤人员/1管理员)',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 学校信息表 (school)
CREATE TABLE IF NOT EXISTS `school` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `school_name` VARCHAR(200) DEFAULT NULL COMMENT '学校名称',
    `total_number` BIGINT DEFAULT NULL COMMENT '学校总人数',
    `student_number` INT DEFAULT NULL COMMENT '在校学生人数',
    `teacher_number` INT DEFAULT NULL COMMENT '在校教师人数',
    `green_area` BIGINT DEFAULT NULL COMMENT '学校绿化总面积',
    `building_area` BIGINT DEFAULT NULL COMMENT '学校建筑物总占地面积',
    `total_area` BIGINT DEFAULT NULL COMMENT '学校总占地面积',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '学校图片地址',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校信息表';

-- 3. 地点信息表 (place_info)
CREATE TABLE IF NOT EXISTS `place_info` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(200) DEFAULT NULL COMMENT '地点名称',
    `population` INT DEFAULT NULL COMMENT '人数',
    `area` DECIMAL(10, 2) DEFAULT NULL COMMENT '占地面积',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地点信息表';

-- 4. 转化系数设置表 (exchange_setting)
CREATE TABLE IF NOT EXISTS `exchange_setting` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `object_category` VARCHAR(100) NOT NULL COMMENT '物品分类',
    `exchange_coefficient` DECIMAL(10, 6) NOT NULL COMMENT '转化系数',
    `unit` VARCHAR(50) NOT NULL COMMENT '种类单位',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_object_category` (`object_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转化系数设置表';

-- 5. 碳排放表 (carbon_emission)
-- 注意：不包含 exchange_coefficient 字段，转化系数从 exchange_setting 表获取
CREATE TABLE IF NOT EXISTS `carbon_emission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(200) NOT NULL COMMENT '本次排放录入的名称',
    `category` VARCHAR(100) NOT NULL COMMENT '分类(如电力/热力/食物等)',
    `consumption` DECIMAL(10, 2) NOT NULL COMMENT '消耗量',
    `purpose` VARCHAR(200) DEFAULT NULL COMMENT '用途',
    `year` INT NOT NULL COMMENT '年份',
    `month` INT NOT NULL COMMENT '月份',
    `amount` DECIMAL(10, 2) DEFAULT NULL COMMENT '碳排放量（消耗量*转化系数，转化系数从 exchange_setting 表获取）',
    `place` VARCHAR(200) DEFAULT NULL COMMENT '排放地点（楼宇）',
    `emission_type` INT NOT NULL COMMENT '排放类型（0/1/2:直接排放/间接排放/其他排放）',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_year` (`year`),
    KEY `idx_month` (`month`),
    KEY `idx_year_month` (`year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳排放表';

-- ============================================
-- RBAC权限管理系统数据库表
-- ============================================

-- 6. 角色表 (role)
CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码（唯一）',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0禁用/1启用）',
    `order` INT NOT NULL DEFAULT 999 COMMENT '权限等级（越小等级越高，0=超级管理员，1=管理员，2=普通用户，3=游客）',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 7. 权限表 (permission)
CREATE TABLE IF NOT EXISTS `permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码（唯一）',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `permission_type` VARCHAR(20) NOT NULL COMMENT '权限类型（menu菜单/button按钮/api接口）',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父权限ID（0表示顶级）',
    `path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径（菜单类型）',
    `component` VARCHAR(200) DEFAULT NULL COMMENT '组件路径（菜单类型）',
    `icon` VARCHAR(50) DEFAULT NULL COMMENT '图标（菜单类型）',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '权限描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0禁用/1启用）',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 8. 用户角色关联表 (user_role)
CREATE TABLE IF NOT EXISTS `user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 9. 角色权限关联表 (role_permission)
CREATE TABLE IF NOT EXISTS `role_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 10. 初始化默认角色
INSERT INTO `role` (`role_code`, `role_name`, `description`, `status`, `order`) VALUES
('SUPER_ADMIN', '超级管理员', '拥有所有权限，可管理所有用户、角色、权限', 1, 0),
('ADMIN', '管理员', '可管理数据录入、查看报表，可管理普通用户', 1, 1),
('USER', '普通用户', '只能录入数据、查看自己的数据', 1, 2),
('GUEST', '游客', '未登录用户，只能进行查询操作', 1, 3)
ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`), `order`=VALUES(`order`);

-- 11. 初始化默认权限（菜单权限）
-- 注意：由于MySQL的限制，parent_id使用临时值，后续通过UPDATE语句修复
-- 第一步：插入所有权限（parent_id使用临时值）
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES
-- 数据输入模块
('DATA_INPUT', '数据输入', 'menu', 0, '/Tan', NULL, 'el-icon-s-data', 1, '数据输入模块', 1),
('SCHOOL_MANAGE', '学校信息', 'menu', 999, '/Tan/ManageSchool', 'ManageSchool', 'el-icon-school', 0, '学校信息管理', 1),
('PLACE_MANAGE', '排放地点', 'menu', 999, '/Tan/ManagePlace', 'ManagePlace', 'el-icon-location', 1, '排放地点管理', 1),
('PLACE_MANAGE_ADD', '新增排放地点', 'api', 999, NULL, NULL, NULL, 1, '新增排放地点权限', 1),
('PLACE_MANAGE_UPDATE', '更新排放地点', 'api', 999, NULL, NULL, NULL, 2, '更新排放地点权限', 1),
('PLACE_MANAGE_DELETE', '删除排放地点', 'api', 999, NULL, NULL, NULL, 3, '删除排放地点权限', 1),
('PLACE_MANAGE_QUERY', '查询排放地点', 'api', 999, NULL, NULL, NULL, 4, '查询排放地点权限', 1),
('SCHOOL_MANAGE_QUERY', '查询学校信息', 'api', 999, NULL, NULL, NULL, 13, '查询学校信息权限', 1),
('SCHOOL_MANAGE_UPDATE', '更新学校信息', 'api', 999, NULL, NULL, NULL, 14, '更新学校信息权限', 1),
('EXCHANGE_SETTING', '碳排放转化系数', 'menu', 999, '/Tan/ExchangeSetting', 'ExchangeSetting', 'el-icon-location', 2, '碳排放转化系数管理', 1),
('EXCHANGE_SETTING_ADD', '新增转化系数', 'api', 999, NULL, NULL, NULL, 5, '新增碳排放转化系数权限', 1),
('EXCHANGE_SETTING_UPDATE', '更新转化系数', 'api', 999, NULL, NULL, NULL, 6, '更新碳排放转化系数权限', 1),
('EXCHANGE_SETTING_DELETE', '删除转化系数', 'api', 999, NULL, NULL, NULL, 7, '删除碳排放转化系数权限', 1),
('EXCHANGE_SETTING_QUERY', '查询转化系数', 'api', 999, NULL, NULL, NULL, 8, '查询碳排放转化系数权限', 1),
('CARBON_RECORD', '碳排放记录', 'menu', 999, '/Tan/ManageCarbon', 'ManageCarbon', 'el-icon-location', 3, '碳排放记录管理', 1),
('CARBON_RECORD_ADD', '新增碳排放记录', 'api', 999, NULL, NULL, NULL, 9, '新增碳排放记录权限', 1),
('CARBON_RECORD_UPDATE', '更新碳排放记录', 'api', 999, NULL, NULL, NULL, 10, '更新碳排放记录权限', 1),
('CARBON_RECORD_DELETE', '删除碳排放记录', 'api', 999, NULL, NULL, NULL, 11, '删除碳排放记录权限', 1),
('CARBON_RECORD_QUERY', '查询碳排放记录', 'api', 999, NULL, NULL, NULL, 12, '查询碳排放记录权限', 1),

-- 能耗监测模块
('ENERGY_MONITOR', '能耗监测', 'menu', 0, '/Tan', NULL, 'el-icon-s-flag', 2, '能耗监测模块', 1),
('ENERGY_MONITOR_DETAIL', '能耗碳排放监测', 'menu', 999, '/Tan/TanMonitor', 'TanMonitor', 'el-icon-location', 1, '能耗碳排放监测', 1),
('ENERGY_AUDIT', '能耗碳排放审计', 'menu', 999, '/Tan/TanAudit', 'TanAudit', 'el-icon-location', 2, '能耗碳排放审计', 1),
('ENERGY_CONTRAST', '能耗碳排放对比', 'menu', 999, '/Tan/TanContrast', 'TanContrast', 'el-icon-location', 3, '能耗碳排放对比', 1),

-- 碳排放计算与减碳分析模块
('CARBON_ANALYSIS', '碳排放计算与减碳分析', 'menu', 0, '/Tan', NULL, 'el-icon-s-data', 3, '碳排放计算与减碳分析模块', 1),
('CARBON_RESULT', '核算结果', 'menu', 999, '/Tan/TanResult', 'TanResult', 'el-icon-location', 1, '核算结果', 1),
('CARBON_ANALYSE', '减碳分析', 'menu', 999, '/Tan/TanAnalyse', 'TanAnalyse', 'el-icon-location', 2, '减碳分析', 1),

-- 报告生成模块
('REPORT_GENERATE', '报告生成', 'menu', 0, '/Tan/TanExport', 'TanExport', 'el-icon-s-order', 4, '报告生成', 1),

-- 系统管理模块
('SYSTEM_MANAGE', '系统管理', 'menu', 0, '/Tan', NULL, 'el-icon-setting', 5, '系统管理模块', 1),
('USER_MANAGE', '用户管理', 'menu', 999, '/Tan/ManageUser', 'ManageUser', 'el-icon-user', 1, '用户管理', 1),
('ROLE_MANAGE', '角色管理', 'menu', 999, '/Tan/ManageRole', 'ManageRole', 'el-icon-s-custom', 2, '角色管理', 1),
('PERMISSION_MANAGE', '权限管理', 'menu', 999, '/Tan/ManagePermission', 'ManagePermission', 'el-icon-key', 3, '权限管理', 1),

-- 用户管理相关的细粒度权限
('USER_MANAGE_ADD', '新增用户', 'api', 999, NULL, NULL, NULL, 1, '新增用户权限', 1),
('USER_MANAGE_UPDATE', '更新用户', 'api', 999, NULL, NULL, NULL, 2, '更新用户权限', 1),
('USER_MANAGE_DELETE', '删除用户', 'api', 999, NULL, NULL, NULL, 3, '删除用户权限', 1),
('USER_MANAGE_QUERY', '查询用户', 'api', 999, NULL, NULL, NULL, 4, '查询用户权限', 1),
('USER_MANAGE_RESET_PASSWORD', '重置密码', 'api', 999, NULL, NULL, NULL, 5, '重置用户密码权限', 1),
('USER_MANAGE_ASSIGN_ROLE', '分配角色', 'api', 999, NULL, NULL, NULL, 6, '为用户分配角色权限', 1),

-- 角色管理相关的细粒度权限
('ROLE_MANAGE_ADD', '新增角色', 'api', 999, NULL, NULL, NULL, 7, '新增角色权限', 1),
('ROLE_MANAGE_UPDATE', '更新角色', 'api', 999, NULL, NULL, NULL, 8, '更新角色权限', 1),
('ROLE_MANAGE_DELETE', '删除角色', 'api', 999, NULL, NULL, NULL, 9, '删除角色权限', 1),
('ROLE_MANAGE_QUERY', '查询角色', 'api', 999, NULL, NULL, NULL, 10, '查询角色权限', 1),
('ROLE_MANAGE_ASSIGN_PERMISSION', '分配权限', 'api', 999, NULL, NULL, NULL, 11, '为角色分配权限', 1),

-- 权限管理相关的细粒度权限
('PERMISSION_MANAGE_ADD', '新增权限', 'api', 999, NULL, NULL, NULL, 12, '新增权限', 1),
('PERMISSION_MANAGE_UPDATE', '更新权限', 'api', 999, NULL, NULL, NULL, 13, '更新权限', 1),
('PERMISSION_MANAGE_DELETE', '删除权限', 'api', 999, NULL, NULL, NULL, 14, '删除权限', 1),
('PERMISSION_MANAGE_QUERY', '查询权限', 'api', 999, NULL, NULL, NULL, 15, '查询权限', 1)
ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`);

-- 第二步：修复parent_id关系
-- 修复数据输入模块的子权限
UPDATE `permission` SET `parent_id` = (SELECT id FROM (SELECT id FROM permission WHERE permission_code = 'DATA_INPUT') AS tmp) WHERE permission_code IN ('SCHOOL_MANAGE', 'PLACE_MANAGE', 'EXCHANGE_SETTING', 'CARBON_RECORD') AND parent_id = 999;

-- 修复学校信息相关的细粒度权限
UPDATE `permission` SET `parent_id` = (SELECT id FROM (SELECT id FROM permission WHERE permission_code = 'SCHOOL_MANAGE') AS tmp) WHERE permission_code IN ('SCHOOL_MANAGE_QUERY', 'SCHOOL_MANAGE_UPDATE') AND parent_id = 999;

-- 修复排放地点相关的细粒度权限
UPDATE `permission` SET `parent_id` = (SELECT id FROM (SELECT id FROM permission WHERE permission_code = 'PLACE_MANAGE') AS tmp) WHERE permission_code IN ('PLACE_MANAGE_ADD', 'PLACE_MANAGE_UPDATE', 'PLACE_MANAGE_DELETE', 'PLACE_MANAGE_QUERY') AND parent_id = 999;

-- 修复碳排放转化系数相关的细粒度权限
UPDATE `permission` SET `parent_id` = (SELECT id FROM (SELECT id FROM permission WHERE permission_code = 'EXCHANGE_SETTING') AS tmp) WHERE permission_code IN ('EXCHANGE_SETTING_ADD', 'EXCHANGE_SETTING_UPDATE', 'EXCHANGE_SETTING_DELETE', 'EXCHANGE_SETTING_QUERY') AND parent_id = 999;

-- 修复碳排放记录相关的细粒度权限
UPDATE `permission` SET `parent_id` = (SELECT id FROM (SELECT id FROM permission WHERE permission_code = 'CARBON_RECORD') AS tmp) WHERE permission_code IN ('CARBON_RECORD_ADD', 'CARBON_RECORD_UPDATE', 'CARBON_RECORD_DELETE', 'CARBON_RECORD_QUERY') AND parent_id = 999;

-- 修复能耗监测模块的子权限
UPDATE `permission` SET `parent_id` = (SELECT id FROM (SELECT id FROM permission WHERE permission_code = 'ENERGY_MONITOR') AS tmp) WHERE permission_code IN ('ENERGY_MONITOR_DETAIL', 'ENERGY_AUDIT', 'ENERGY_CONTRAST') AND parent_id = 999;

-- 修复碳排放计算与减碳分析模块的子权限
UPDATE `permission` SET `parent_id` = (SELECT id FROM (SELECT id FROM permission WHERE permission_code = 'CARBON_ANALYSIS') AS tmp) WHERE permission_code IN ('CARBON_RESULT', 'CARBON_ANALYSE') AND parent_id = 999;

-- 修复系统管理模块的子权限
UPDATE `permission` SET `parent_id` = (SELECT id FROM (SELECT id FROM permission WHERE permission_code = 'SYSTEM_MANAGE') AS tmp) WHERE permission_code IN ('USER_MANAGE', 'ROLE_MANAGE', 'PERMISSION_MANAGE') AND parent_id = 999;

-- 修复用户管理相关的细粒度权限
UPDATE `permission` SET `parent_id` = (SELECT id FROM (SELECT id FROM permission WHERE permission_code = 'USER_MANAGE') AS tmp) WHERE permission_code IN ('USER_MANAGE_ADD', 'USER_MANAGE_UPDATE', 'USER_MANAGE_DELETE', 'USER_MANAGE_QUERY', 'USER_MANAGE_RESET_PASSWORD', 'USER_MANAGE_ASSIGN_ROLE') AND parent_id = 999;

-- 修复角色管理相关的细粒度权限
UPDATE `permission` SET `parent_id` = (SELECT id FROM (SELECT id FROM permission WHERE permission_code = 'ROLE_MANAGE') AS tmp) WHERE permission_code IN ('ROLE_MANAGE_ADD', 'ROLE_MANAGE_UPDATE', 'ROLE_MANAGE_DELETE', 'ROLE_MANAGE_QUERY', 'ROLE_MANAGE_ASSIGN_PERMISSION') AND parent_id = 999;

-- 修复权限管理相关的细粒度权限
UPDATE `permission` SET `parent_id` = (SELECT id FROM (SELECT id FROM permission WHERE permission_code = 'PERMISSION_MANAGE') AS tmp) WHERE permission_code IN ('PERMISSION_MANAGE_ADD', 'PERMISSION_MANAGE_UPDATE', 'PERMISSION_MANAGE_DELETE', 'PERMISSION_MANAGE_QUERY') AND parent_id = 999;

-- 12. 为超级管理员分配所有权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r
CROSS JOIN `permission` p
WHERE r.role_code = 'SUPER_ADMIN'
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

-- 13. 为管理员分配数据输入和查看权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r
CROSS JOIN `permission` p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN (
    -- 数据输入模块（所有权限）
    'DATA_INPUT', 'SCHOOL_MANAGE', 'SCHOOL_MANAGE_QUERY', 'SCHOOL_MANAGE_UPDATE',
    'PLACE_MANAGE', 'PLACE_MANAGE_ADD', 'PLACE_MANAGE_UPDATE', 'PLACE_MANAGE_DELETE', 'PLACE_MANAGE_QUERY',
    'EXCHANGE_SETTING', 'EXCHANGE_SETTING_ADD', 'EXCHANGE_SETTING_UPDATE', 'EXCHANGE_SETTING_DELETE', 'EXCHANGE_SETTING_QUERY',
    'CARBON_RECORD', 'CARBON_RECORD_ADD', 'CARBON_RECORD_UPDATE', 'CARBON_RECORD_DELETE', 'CARBON_RECORD_QUERY',
    -- 能耗监测模块
    'ENERGY_MONITOR', 'ENERGY_MONITOR_DETAIL', 'ENERGY_AUDIT', 'ENERGY_CONTRAST',
    -- 碳排放计算与减碳分析模块
    'CARBON_ANALYSIS', 'CARBON_RESULT', 'CARBON_ANALYSE',
    -- 报告生成
    'REPORT_GENERATE',
    -- 用户管理（查询权限）
    'USER_MANAGE', 'USER_MANAGE_QUERY'
  )
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

-- 14. 为普通用户分配基本权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r
CROSS JOIN `permission` p
WHERE r.role_code = 'USER'
  AND p.permission_code IN (
    -- 数据输入模块（只能查看和添加碳排放记录）
    'DATA_INPUT', 'CARBON_RECORD', 'CARBON_RECORD_QUERY', 'CARBON_RECORD_ADD',
    -- 能耗监测模块（只能查看）
    'ENERGY_MONITOR', 'ENERGY_MONITOR_DETAIL',
    -- 碳排放计算与减碳分析模块（只能查看）
    'CARBON_ANALYSIS', 'CARBON_RESULT', 'CARBON_ANALYSE'
  )
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

-- 15. 为游客角色分配所有查询权限（所有以QUERY结尾的权限和菜单权限）
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r
CROSS JOIN `permission` p
WHERE r.role_code = 'GUEST'
  AND p.status = 1
  AND (
    -- 所有查询权限（以QUERY结尾）
    p.permission_code LIKE '%_QUERY'
    -- 或者所有菜单权限（用于访问页面）
    OR p.permission_type = 'menu'
  )
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

-- ============================================
-- 初始化完成说明
-- ============================================
-- 1. 权限配置：
--    - 所有菜单权限和API权限已正确配置
--    - 权限树形结构已正确建立（通过parent_id关联）
--    - 数据输入模块：学校信息、排放地点、碳排放转化系数、碳排放记录（各包含增删改查权限）
--    - 能耗监测模块：能耗碳排放监测、能耗碳排放审计、能耗碳排放对比
--    - 碳排放计算与减碳分析模块：核算结果、减碳分析
--    - 报告生成模块：报告生成
--    - 系统管理模块：用户管理、角色管理、权限管理（各包含细粒度权限）
--
-- 2. 角色配置：
--    - SUPER_ADMIN（超级管理员，order=0）：拥有所有权限
--    - ADMIN（管理员，order=1）：拥有数据输入、查看、用户管理查询权限
--    - USER（普通用户，order=2）：拥有数据查看和添加碳排放记录权限
--    - GUEST（游客，order=3）：拥有所有查询权限和菜单访问权限
--
-- 3. 角色权限分配：
--    - 超级管理员：所有权限
--    - 管理员：数据输入（全部）、能耗监测（查看）、碳排放分析（查看）、报告生成、用户管理（查询）
--    - 普通用户：数据输入（查看和添加碳排放记录）、能耗监测（查看）、碳排放分析（查看）
--    - 游客：所有查询权限和菜单权限
-- ============================================

-- ============================================
-- 用户表数据（从当前数据库导出）
-- ============================================
INSERT INTO `user` (`name`, `username`, `password`, `department`, `phone`, `salt`, `status`) VALUES ("超级管理员","super_admin","2a884a75ca3d2d5745601426290f4e00","","","qT85",1) ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `password`=VALUES(`password`), `department`=VALUES(`department`), `phone`=VALUES(`phone`), `salt`=VALUES(`salt`), `status`=VALUES(`status`);
INSERT INTO `user` (`name`, `username`, `password`, `department`, `phone`, `salt`, `status`) VALUES ("管理员","admin","5a73d468fb5ae05627185dd7f607cc8a","","","%8yT",1) ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `password`=VALUES(`password`), `department`=VALUES(`department`), `phone`=VALUES(`phone`), `salt`=VALUES(`salt`), `status`=VALUES(`status`);
INSERT INTO `user` (`name`, `username`, `password`, `department`, `phone`, `salt`, `status`) VALUES ("普通用户","user","a6d985395468ab08c4b87c550e6b7d94","","","c9^T",1) ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `password`=VALUES(`password`), `department`=VALUES(`department`), `phone`=VALUES(`phone`), `salt`=VALUES(`salt`), `status`=VALUES(`status`);

-- ============================================
-- 学校信息表数据（从当前数据库导出）
-- ============================================
INSERT INTO `school` (`school_name`, `total_number`, `student_number`, `teacher_number`, `green_area`, `building_area`, `total_area`, `image_url`) VALUES ("北京林业大学",26119,25693,1412,94000,24857,464000,"https://pic1.zhimg.com/v2-9bddb0e0db1c9a22d22a392c23c27b65_1440w.jpg?source=172ae18b") ON DUPLICATE KEY UPDATE `school_name`=VALUES(`school_name`), `total_number`=VALUES(`total_number`), `student_number`=VALUES(`student_number`), `teacher_number`=VALUES(`teacher_number`), `green_area`=VALUES(`green_area`), `building_area`=VALUES(`building_area`), `total_area`=VALUES(`total_area`), `image_url`=VALUES(`image_url`);

-- ============================================
-- 地点信息表数据（从当前数据库导出）
-- ============================================
INSERT INTO `place_info` (`name`, `population`, `area`) VALUES ("校园道路",15000,6000.00) ON DUPLICATE KEY UPDATE `population`=VALUES(`population`), `area`=VALUES(`area`);
INSERT INTO `place_info` (`name`, `population`, `area`) VALUES ("东区食堂",10000,2000.00) ON DUPLICATE KEY UPDATE `population`=VALUES(`population`), `area`=VALUES(`area`);
INSERT INTO `place_info` (`name`, `population`, `area`) VALUES ("西区食堂",6000,1500.00) ON DUPLICATE KEY UPDATE `population`=VALUES(`population`), `area`=VALUES(`area`);
INSERT INTO `place_info` (`name`, `population`, `area`) VALUES ("学研教学楼",2000,900.00) ON DUPLICATE KEY UPDATE `population`=VALUES(`population`), `area`=VALUES(`area`);
INSERT INTO `place_info` (`name`, `population`, `area`) VALUES ("5号宿舍楼",500,1200.00) ON DUPLICATE KEY UPDATE `population`=VALUES(`population`), `area`=VALUES(`area`);

-- ============================================
-- 碳排放转化系数表数据（从当前数据库导出）
-- ============================================
INSERT INTO `exchange_setting` (`object_category`, `exchange_coefficient`, `unit`) VALUES ("电力",0.685000,"kgCO₂/kWh") ON DUPLICATE KEY UPDATE `exchange_coefficient`=VALUES(`exchange_coefficient`), `unit`=VALUES(`unit`);
INSERT INTO `exchange_setting` (`object_category`, `exchange_coefficient`, `unit`) VALUES ("热力（天然气）",2.160000,"kgCO₂/Nm³") ON DUPLICATE KEY UPDATE `exchange_coefficient`=VALUES(`exchange_coefficient`), `unit`=VALUES(`unit`);
INSERT INTO `exchange_setting` (`object_category`, `exchange_coefficient`, `unit`) VALUES ("食品",2.500000,"kgCO₂e/kg") ON DUPLICATE KEY UPDATE `exchange_coefficient`=VALUES(`exchange_coefficient`), `unit`=VALUES(`unit`);
INSERT INTO `exchange_setting` (`object_category`, `exchange_coefficient`, `unit`) VALUES ("生活垃圾",0.300000,"kgCO₂e/kg") ON DUPLICATE KEY UPDATE `exchange_coefficient`=VALUES(`exchange_coefficient`), `unit`=VALUES(`unit`);
INSERT INTO `exchange_setting` (`object_category`, `exchange_coefficient`, `unit`) VALUES ("生物材料（PHA）",1.200000,"kgCO₂e/kg") ON DUPLICATE KEY UPDATE `exchange_coefficient`=VALUES(`exchange_coefficient`), `unit`=VALUES(`unit`);

-- ============================================
-- 碳排放表数据（从当前数据库导出）
-- ============================================
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-学研教学楼用电","电力",45000.00,"日常用电",2025,1,30825.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-5号宿舍楼用电","电力",32000.00,"宿舍用电",2025,1,21920.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-东区食堂用电","电力",28000.00,"食堂运营",2025,1,19180.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-西区食堂用电","电力",18000.00,"食堂运营",2025,1,12330.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-校园道路照明用电","电力",15000.00,"道路照明",2025,1,10275.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-学研教学楼供暖","热力（天然气）",8564.00,"冬季供暖",2025,1,18498.24,"学研教学楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-5号宿舍楼供暖","热力（天然气）",12000.00,"冬季供暖",2025,1,25920.00,"5号宿舍楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-东区食堂供暖","热力（天然气）",15000.00,"冬季供暖",2025,1,32400.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-西区食堂供暖","热力（天然气）",9000.00,"冬季供暖",2025,1,19440.00,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-东区食堂食品消耗","食品",85000.00,"师生用餐",2025,1,212500.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-西区食堂食品消耗","食品",51000.00,"师生用餐",2025,1,127500.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-5号宿舍楼生活垃圾","生活垃圾",15000.00,"日常生活垃圾",2025,1,4500.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-学研教学楼生活垃圾","生活垃圾",8000.00,"日常办公垃圾",2025,1,2400.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-东区食堂生活垃圾","生活垃圾",12000.00,"食堂垃圾",2025,1,3600.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-西区食堂生活垃圾","生活垃圾",7200.00,"食堂垃圾",2025,1,2160.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.01-校园道路生活垃圾","生活垃圾",5000.00,"公共区域垃圾",2025,1,1500.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-学研教学楼用电","电力",38000.00,"日常用电",2025,2,26030.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-5号宿舍楼用电","电力",28000.00,"宿舍用电",2025,2,19180.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-东区食堂用电","电力",25000.00,"食堂运营",2025,2,17125.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-西区食堂用电","电力",16000.00,"食堂运营",2025,2,10960.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-校园道路照明用电","电力",14000.00,"道路照明",2025,2,9590.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-学研教学楼供暖","热力（天然气）",8000.00,"冬季供暖",2025,2,17280.00,"学研教学楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-5号宿舍楼供暖","热力（天然气）",11500.00,"冬季供暖",2025,2,24840.00,"5号宿舍楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-东区食堂供暖","热力（天然气）",14500.00,"冬季供暖",2025,2,31320.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-西区食堂供暖","热力（天然气）",8700.00,"冬季供暖",2025,2,18792.00,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-东区食堂食品消耗","食品",70000.00,"师生用餐",2025,2,175000.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-西区食堂食品消耗","食品",42000.00,"师生用餐",2025,2,105000.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-5号宿舍楼生活垃圾","生活垃圾",12000.00,"日常生活垃圾",2025,2,3600.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-学研教学楼生活垃圾","生活垃圾",6500.00,"日常办公垃圾",2025,2,1950.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-东区食堂生活垃圾","生活垃圾",10000.00,"食堂垃圾",2025,2,3000.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-西区食堂生活垃圾","生活垃圾",6000.00,"食堂垃圾",2025,2,1800.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.02-校园道路生活垃圾","生活垃圾",4000.00,"公共区域垃圾",2025,2,1200.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-学研教学楼用电","电力",42000.00,"日常用电",2025,3,28770.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-5号宿舍楼用电","电力",30000.00,"宿舍用电",2025,3,20550.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-东区食堂用电","电力",27000.00,"食堂运营",2025,3,18495.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-西区食堂用电","电力",17000.00,"食堂运营",2025,3,11645.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-校园道路照明用电","电力",14500.00,"道路照明",2025,3,9932.50,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-学研教学楼供暖","热力（天然气）",5000.00,"春季供暖",2025,3,10800.00,"学研教学楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-5号宿舍楼供暖","热力（天然气）",7000.00,"春季供暖",2025,3,15120.00,"5号宿舍楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-东区食堂供暖","热力（天然气）",8000.00,"春季供暖",2025,3,17280.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-西区食堂供暖","热力（天然气）",4800.00,"春季供暖",2025,3,10368.00,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-东区食堂食品消耗","食品",88000.00,"师生用餐",2025,3,220000.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-西区食堂食品消耗","食品",52800.00,"师生用餐",2025,3,132000.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-5号宿舍楼生活垃圾","生活垃圾",15500.00,"日常生活垃圾",2025,3,4650.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-学研教学楼生活垃圾","生活垃圾",8200.00,"日常办公垃圾",2025,3,2460.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-东区食堂生活垃圾","生活垃圾",12500.00,"食堂垃圾",2025,3,3750.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-西区食堂生活垃圾","生活垃圾",7500.00,"食堂垃圾",2025,3,2250.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.03-校园道路生活垃圾","生活垃圾",5200.00,"公共区域垃圾",2025,3,1560.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-学研教学楼用电","电力",41000.00,"日常用电",2025,4,28085.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-5号宿舍楼用电","电力",29000.00,"宿舍用电",2025,4,19865.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-东区食堂用电","电力",26500.00,"食堂运营",2025,4,18152.50,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-西区食堂用电","电力",16500.00,"食堂运营",2025,4,11302.50,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-校园道路照明用电","电力",14000.00,"道路照明",2025,4,9590.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-学研教学楼供暖","热力（天然气）",2000.00,"春季供暖",2025,4,4320.00,"学研教学楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-5号宿舍楼供暖","热力（天然气）",3000.00,"春季供暖",2025,4,6480.00,"5号宿舍楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-东区食堂供暖","热力（天然气）",3500.00,"春季供暖",2025,4,7560.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-西区食堂供暖","热力（天然气）",2100.00,"春季供暖",2025,4,4536.00,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-东区食堂食品消耗","食品",90000.00,"师生用餐",2025,4,225000.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-西区食堂食品消耗","食品",54000.00,"师生用餐",2025,4,135000.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-5号宿舍楼生活垃圾","生活垃圾",16000.00,"日常生活垃圾",2025,4,4800.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-学研教学楼生活垃圾","生活垃圾",8500.00,"日常办公垃圾",2025,4,2550.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-东区食堂生活垃圾","生活垃圾",13000.00,"食堂垃圾",2025,4,3900.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-西区食堂生活垃圾","生活垃圾",7800.00,"食堂垃圾",2025,4,2340.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.04-校园道路生活垃圾","生活垃圾",5400.00,"公共区域垃圾",2025,4,1620.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-学研教学楼用电","电力",40000.00,"日常用电",2025,5,27400.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-5号宿舍楼用电","电力",28500.00,"宿舍用电",2025,5,19522.50,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-东区食堂用电","电力",26000.00,"食堂运营",2025,5,17810.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-西区食堂用电","电力",16000.00,"食堂运营",2025,5,10960.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-校园道路照明用电","电力",13500.00,"道路照明",2025,5,9247.50,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-东区食堂热水","热力（天然气）",1500.00,"热水供应",2025,5,3240.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-西区食堂热水","热力（天然气）",900.00,"热水供应",2025,5,1944.00,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-东区食堂食品消耗","食品",92000.00,"师生用餐",2025,5,230000.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-西区食堂食品消耗","食品",55200.00,"师生用餐",2025,5,138000.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-5号宿舍楼生活垃圾","生活垃圾",16500.00,"日常生活垃圾",2025,5,4950.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-学研教学楼生活垃圾","生活垃圾",8800.00,"日常办公垃圾",2025,5,2640.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-东区食堂生活垃圾","生活垃圾",13500.00,"食堂垃圾",2025,5,4050.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-西区食堂生活垃圾","生活垃圾",8100.00,"食堂垃圾",2025,5,2430.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.05-校园道路生活垃圾","生活垃圾",5600.00,"公共区域垃圾",2025,5,1680.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-学研教学楼用电","电力",48000.00,"日常用电及空调",2025,6,32880.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-5号宿舍楼用电","电力",35000.00,"宿舍用电及空调",2025,6,23975.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-东区食堂用电","电力",30000.00,"食堂运营及空调",2025,6,20550.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-西区食堂用电","电力",19000.00,"食堂运营及空调",2025,6,13015.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-校园道路照明用电","电力",14000.00,"道路照明",2025,6,9590.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-东区食堂热水","热力（天然气）",1200.00,"热水供应",2025,6,2592.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-西区食堂热水","热力（天然气）",720.00,"热水供应",2025,6,1555.20,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-东区食堂食品消耗","食品",90000.00,"师生用餐",2025,6,225000.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-西区食堂食品消耗","食品",54000.00,"师生用餐",2025,6,135000.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-5号宿舍楼生活垃圾","生活垃圾",17000.00,"日常生活垃圾",2025,6,5100.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-学研教学楼生活垃圾","生活垃圾",9000.00,"日常办公垃圾",2025,6,2700.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-东区食堂生活垃圾","生活垃圾",14000.00,"食堂垃圾",2025,6,4200.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-西区食堂生活垃圾","生活垃圾",8400.00,"食堂垃圾",2025,6,2520.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.06-校园道路生活垃圾","生活垃圾",5800.00,"公共区域垃圾",2025,6,1740.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-学研教学楼用电","电力",52000.00,"日常用电及空调",2025,7,35620.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-5号宿舍楼用电","电力",38000.00,"宿舍用电及空调",2025,7,26030.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-东区食堂用电","电力",32000.00,"食堂运营及空调",2025,7,21920.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-西区食堂用电","电力",20000.00,"食堂运营及空调",2025,7,13700.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-校园道路照明用电","电力",14500.00,"道路照明",2025,7,9932.50,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-东区食堂热水","热力（天然气）",1000.00,"热水供应",2025,7,2160.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-西区食堂热水","热力（天然气）",600.00,"热水供应",2025,7,1296.00,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-东区食堂食品消耗","食品",70000.00,"师生用餐",2025,7,175000.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-西区食堂食品消耗","食品",42000.00,"师生用餐",2025,7,105000.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-5号宿舍楼生活垃圾","生活垃圾",12000.00,"日常生活垃圾",2025,7,3600.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-学研教学楼生活垃圾","生活垃圾",6000.00,"日常办公垃圾",2025,7,1800.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-东区食堂生活垃圾","生活垃圾",10000.00,"食堂垃圾",2025,7,3000.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-西区食堂生活垃圾","生活垃圾",6000.00,"食堂垃圾",2025,7,1800.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.07-校园道路生活垃圾","生活垃圾",4500.00,"公共区域垃圾",2025,7,1350.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-学研教学楼用电","电力",50000.00,"日常用电及空调",2025,8,34250.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-5号宿舍楼用电","电力",36000.00,"宿舍用电及空调",2025,8,24660.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-东区食堂用电","电力",31000.00,"食堂运营及空调",2025,8,21235.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-西区食堂用电","电力",19000.00,"食堂运营及空调",2025,8,13015.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-校园道路照明用电","电力",14500.00,"道路照明",2025,8,9932.50,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-东区食堂热水","热力（天然气）",1100.00,"热水供应",2025,8,2376.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-西区食堂热水","热力（天然气）",660.00,"热水供应",2025,8,1425.60,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-东区食堂食品消耗","食品",72000.00,"师生用餐",2025,8,180000.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-西区食堂食品消耗","食品",43200.00,"师生用餐",2025,8,108000.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-5号宿舍楼生活垃圾","生活垃圾",12500.00,"日常生活垃圾",2025,8,3750.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-学研教学楼生活垃圾","生活垃圾",6500.00,"日常办公垃圾",2025,8,1950.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-东区食堂生活垃圾","生活垃圾",10500.00,"食堂垃圾",2025,8,3150.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-西区食堂生活垃圾","生活垃圾",6300.00,"食堂垃圾",2025,8,1890.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.08-校园道路生活垃圾","生活垃圾",4800.00,"公共区域垃圾",2025,8,1440.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-学研教学楼用电","电力",43000.00,"日常用电",2025,9,29455.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-5号宿舍楼用电","电力",31000.00,"宿舍用电",2025,9,21235.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-东区食堂用电","电力",27500.00,"食堂运营",2025,9,18837.50,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-西区食堂用电","电力",17500.00,"食堂运营",2025,9,11987.50,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-校园道路照明用电","电力",14200.00,"道路照明",2025,9,9727.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-东区食堂热水","热力（天然气）",1300.00,"热水供应",2025,9,2808.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-西区食堂热水","热力（天然气）",780.00,"热水供应",2025,9,1684.80,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-东区食堂食品消耗","食品",95000.00,"师生用餐",2025,9,237500.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-西区食堂食品消耗","食品",57000.00,"师生用餐",2025,9,142500.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-5号宿舍楼生活垃圾","生活垃圾",18000.00,"日常生活垃圾",2025,9,5400.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-学研教学楼生活垃圾","生活垃圾",9500.00,"日常办公垃圾",2025,9,2850.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-东区食堂生活垃圾","生活垃圾",15000.00,"食堂垃圾",2025,9,4500.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-西区食堂生活垃圾","生活垃圾",9000.00,"食堂垃圾",2025,9,2700.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.09-校园道路生活垃圾","生活垃圾",6000.00,"公共区域垃圾",2025,9,1800.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-学研教学楼用电","电力",42000.00,"日常用电",2025,10,28770.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-5号宿舍楼用电","电力",30000.00,"宿舍用电",2025,10,20550.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-东区食堂用电","电力",27000.00,"食堂运营",2025,10,18495.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-西区食堂用电","电力",17000.00,"食堂运营",2025,10,11645.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-校园道路照明用电","电力",14000.00,"道路照明",2025,10,9590.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-学研教学楼供暖","热力（天然气）",3000.00,"秋季供暖",2025,10,6480.00,"学研教学楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-5号宿舍楼供暖","热力（天然气）",4500.00,"秋季供暖",2025,10,9720.00,"5号宿舍楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-东区食堂供暖","热力（天然气）",5000.00,"秋季供暖",2025,10,10800.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-西区食堂供暖","热力（天然气）",3000.00,"秋季供暖",2025,10,6480.00,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-东区食堂食品消耗","食品",92000.00,"师生用餐",2025,10,230000.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-西区食堂食品消耗","食品",55200.00,"师生用餐",2025,10,138000.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-5号宿舍楼生活垃圾","生活垃圾",17500.00,"日常生活垃圾",2025,10,5250.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-学研教学楼生活垃圾","生活垃圾",9000.00,"日常办公垃圾",2025,10,2700.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-东区食堂生活垃圾","生活垃圾",14000.00,"食堂垃圾",2025,10,4200.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-西区食堂生活垃圾","生活垃圾",8400.00,"食堂垃圾",2025,10,2520.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.10-校园道路生活垃圾","生活垃圾",5800.00,"公共区域垃圾",2025,10,1740.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-学研教学楼用电","电力",44000.00,"日常用电",2025,11,30140.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-5号宿舍楼用电","电力",31500.00,"宿舍用电",2025,11,21577.50,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-东区食堂用电","电力",28000.00,"食堂运营",2025,11,19180.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-西区食堂用电","电力",18000.00,"食堂运营",2025,11,12330.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-校园道路照明用电","电力",14500.00,"道路照明",2025,11,9932.50,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-学研教学楼供暖","热力（天然气）",6000.00,"秋季供暖",2025,11,12960.00,"学研教学楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-5号宿舍楼供暖","热力（天然气）",9000.00,"秋季供暖",2025,11,19440.00,"5号宿舍楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-东区食堂供暖","热力（天然气）",11000.00,"秋季供暖",2025,11,23760.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-西区食堂供暖","热力（天然气）",6600.00,"秋季供暖",2025,11,14256.00,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-东区食堂食品消耗","食品",90000.00,"师生用餐",2025,11,225000.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-西区食堂食品消耗","食品",54000.00,"师生用餐",2025,11,135000.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-5号宿舍楼生活垃圾","生活垃圾",17000.00,"日常生活垃圾",2025,11,5100.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-学研教学楼生活垃圾","生活垃圾",9200.00,"日常办公垃圾",2025,11,2760.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-东区食堂生活垃圾","生活垃圾",13500.00,"食堂垃圾",2025,11,4050.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-西区食堂生活垃圾","生活垃圾",8100.00,"食堂垃圾",2025,11,2430.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.11-校园道路生活垃圾","生活垃圾",6000.00,"公共区域垃圾",2025,11,1800.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-学研教学楼用电","电力",46000.00,"日常用电",2025,12,31510.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-5号宿舍楼用电","电力",33000.00,"宿舍用电",2025,12,22605.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-东区食堂用电","电力",29000.00,"食堂运营",2025,12,19865.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-西区食堂用电","电力",18500.00,"食堂运营",2025,12,12672.50,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-校园道路照明用电","电力",15000.00,"道路照明",2025,12,10275.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-学研教学楼供暖","热力（天然气）",9000.00,"冬季供暖",2025,12,19440.00,"学研教学楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-5号宿舍楼供暖","热力（天然气）",12500.00,"冬季供暖",2025,12,27000.00,"5号宿舍楼",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-东区食堂供暖","热力（天然气）",15500.00,"冬季供暖",2025,12,33480.00,"东区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-西区食堂供暖","热力（天然气）",9300.00,"冬季供暖",2025,12,20088.00,"西区食堂",0) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-东区食堂食品消耗","食品",88000.00,"师生用餐",2025,12,220000.00,"东区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-西区食堂食品消耗","食品",52800.00,"师生用餐",2025,12,132000.00,"西区食堂",2) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-5号宿舍楼生活垃圾","生活垃圾",16500.00,"日常生活垃圾",2025,12,4950.00,"5号宿舍楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-学研教学楼生活垃圾","生活垃圾",9000.00,"日常办公垃圾",2025,12,2700.00,"学研教学楼",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-东区食堂生活垃圾","生活垃圾",14000.00,"食堂垃圾",2025,12,4200.00,"东区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-西区食堂生活垃圾","生活垃圾",8400.00,"食堂垃圾",2025,12,2520.00,"西区食堂",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);
INSERT INTO `carbon_emission` (`name`, `category`, `consumption`, `purpose`, `year`, `month`, `amount`, `place`, `emission_type`) VALUES ("2025.12-校园道路生活垃圾","生活垃圾",5800.00,"公共区域垃圾",2025,12,1740.00,"校园道路",1) ON DUPLICATE KEY UPDATE `consumption`=VALUES(`consumption`), `purpose`=VALUES(`purpose`), `amount`=VALUES(`amount`), `place`=VALUES(`place`), `emission_type`=VALUES(`emission_type`);

-- ============================================
-- 角色表数据（从当前数据库导出）
-- ============================================
INSERT INTO `role` (`role_code`, `role_name`, `description`, `status`, `order`) VALUES ("SUPER_ADMIN","超级管理员","拥有所有权限，可管理所有用户、角色、权限",1,0) ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`), `description`=VALUES(`description`), `status`=VALUES(`status`), `order`=VALUES(`order`);
INSERT INTO `role` (`role_code`, `role_name`, `description`, `status`, `order`) VALUES ("ADMIN","管理员","可管理数据录入、查看报表，可管理普通用户",1,1) ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`), `description`=VALUES(`description`), `status`=VALUES(`status`), `order`=VALUES(`order`);
INSERT INTO `role` (`role_code`, `role_name`, `description`, `status`, `order`) VALUES ("USER","普通用户","只能录入数据、查看自己的数据",1,2) ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`), `description`=VALUES(`description`), `status`=VALUES(`status`), `order`=VALUES(`order`);
INSERT INTO `role` (`role_code`, `role_name`, `description`, `status`, `order`) VALUES ("GUEST","游客","未登录用户，只能进行查询操作",1,3) ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`), `description`=VALUES(`description`), `status`=VALUES(`status`), `order`=VALUES(`order`);

-- ============================================
-- 权限表数据（从当前数据库导出）
-- ============================================
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("DATA_INPUT","数据输入","menu",0,"/Tan",NULL,"el-icon-s-data",1,"数据输入模块",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("SCHOOL_MANAGE","学校信息","menu",1,"/Tan/ManageSchool","ManageSchool","el-icon-school",0,"学校信息管理",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("PLACE_MANAGE","排放地点","menu",1,"/Tan/ManagePlace","ManagePlace","el-icon-location",1,"排放地点管理",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("PLACE_MANAGE_ADD","新增排放地点","api",3,NULL,NULL,NULL,1,"新增排放地点权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("PLACE_MANAGE_UPDATE","更新排放地点","api",3,NULL,NULL,NULL,2,"更新排放地点权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("PLACE_MANAGE_DELETE","删除排放地点","api",3,NULL,NULL,NULL,3,"删除排放地点权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("PLACE_MANAGE_QUERY","查询排放地点","api",3,NULL,NULL,NULL,4,"查询排放地点权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("SCHOOL_MANAGE_QUERY","查询学校信息","api",2,NULL,NULL,NULL,13,"查询学校信息权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("SCHOOL_MANAGE_UPDATE","更新学校信息","api",2,NULL,NULL,NULL,14,"更新学校信息权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("EXCHANGE_SETTING","碳排放转化系数","menu",1,"/Tan/ExchangeSetting","ExchangeSetting","el-icon-location",2,"碳排放转化系数管理",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("EXCHANGE_SETTING_ADD","新增转化系数","api",10,NULL,NULL,NULL,5,"新增碳排放转化系数权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("EXCHANGE_SETTING_UPDATE","更新转化系数","api",10,NULL,NULL,NULL,6,"更新碳排放转化系数权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("EXCHANGE_SETTING_DELETE","删除转化系数","api",10,NULL,NULL,NULL,7,"删除碳排放转化系数权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("EXCHANGE_SETTING_QUERY","查询转化系数","api",10,NULL,NULL,NULL,8,"查询碳排放转化系数权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("CARBON_RECORD","碳排放记录","menu",1,"/Tan/ManageCarbon","ManageCarbon","el-icon-location",3,"碳排放记录管理",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("CARBON_RECORD_ADD","新增碳排放记录","api",15,NULL,NULL,NULL,9,"新增碳排放记录权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("CARBON_RECORD_UPDATE","更新碳排放记录","api",15,NULL,NULL,NULL,10,"更新碳排放记录权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("CARBON_RECORD_DELETE","删除碳排放记录","api",15,NULL,NULL,NULL,11,"删除碳排放记录权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("CARBON_RECORD_QUERY","查询碳排放记录","api",15,NULL,NULL,NULL,12,"查询碳排放记录权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("ENERGY_MONITOR","能耗监测","menu",0,"/Tan",NULL,"el-icon-s-flag",2,"能耗监测模块",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("ENERGY_MONITOR_DETAIL","能耗碳排放监测","menu",20,"/Tan/TanMonitor","TanMonitor","el-icon-location",1,"能耗碳排放监测",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("ENERGY_AUDIT","能耗碳排放审计","menu",20,"/Tan/TanAudit","TanAudit","el-icon-location",2,"能耗碳排放审计",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("ENERGY_CONTRAST","能耗碳排放对比","menu",20,"/Tan/TanContrast","TanContrast","el-icon-location",3,"能耗碳排放对比",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("CARBON_ANALYSIS","碳排放计算与减碳分析","menu",0,"/Tan",NULL,"el-icon-s-data",3,"碳排放计算与减碳分析模块",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("CARBON_RESULT","核算结果","menu",24,"/Tan/TanResult","TanResult","el-icon-location",1,"核算结果",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("CARBON_ANALYSE","减碳分析","menu",24,"/Tan/TanAnalyse","TanAnalyse","el-icon-location",2,"减碳分析",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("REPORT_GENERATE","报告生成","menu",0,"/Tan/TanExport","TanExport","el-icon-s-order",4,"报告生成",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("SYSTEM_MANAGE","系统管理","menu",0,"/Tan",NULL,"el-icon-setting",5,"系统管理模块",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("USER_MANAGE","用户管理","menu",28,"/Tan/ManageUser","ManageUser","el-icon-user",1,"用户管理",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("ROLE_MANAGE","角色管理","menu",28,"/Tan/ManageRole","ManageRole","el-icon-s-custom",2,"角色管理",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("PERMISSION_MANAGE","权限管理","menu",28,"/Tan/ManagePermission","ManagePermission","el-icon-key",3,"权限管理",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("USER_MANAGE_ADD","新增用户","api",29,NULL,NULL,NULL,1,"新增用户权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("USER_MANAGE_UPDATE","更新用户","api",29,NULL,NULL,NULL,2,"更新用户权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("USER_MANAGE_DELETE","删除用户","api",29,NULL,NULL,NULL,3,"删除用户权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("USER_MANAGE_QUERY","查询用户","api",29,NULL,NULL,NULL,4,"查询用户权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("USER_MANAGE_RESET_PASSWORD","重置密码","api",29,NULL,NULL,NULL,5,"重置用户密码权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("USER_MANAGE_ASSIGN_ROLE","分配角色","api",29,NULL,NULL,NULL,6,"为用户分配角色权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("ROLE_MANAGE_ADD","新增角色","api",30,NULL,NULL,NULL,7,"新增角色权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("ROLE_MANAGE_UPDATE","更新角色","api",30,NULL,NULL,NULL,8,"更新角色权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("ROLE_MANAGE_DELETE","删除角色","api",30,NULL,NULL,NULL,9,"删除角色权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("ROLE_MANAGE_QUERY","查询角色","api",30,NULL,NULL,NULL,10,"查询角色权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("ROLE_MANAGE_ASSIGN_PERMISSION","分配权限","api",30,NULL,NULL,NULL,11,"为角色分配权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("PERMISSION_MANAGE_ADD","新增权限","api",31,NULL,NULL,NULL,12,"新增权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("PERMISSION_MANAGE_UPDATE","更新权限","api",31,NULL,NULL,NULL,13,"更新权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("PERMISSION_MANAGE_DELETE","删除权限","api",31,NULL,NULL,NULL,14,"删除权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);
INSERT INTO `permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `description`, `status`) VALUES ("PERMISSION_MANAGE_QUERY","查询权限","api",31,NULL,NULL,NULL,15,"查询权限",1) ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`), `permission_type`=VALUES(`permission_type`), `parent_id`=VALUES(`parent_id`), `path`=VALUES(`path`), `component`=VALUES(`component`), `icon`=VALUES(`icon`), `sort_order`=VALUES(`sort_order`), `description`=VALUES(`description`), `status`=VALUES(`status`);

-- ============================================
-- 用户角色关联表数据（从当前数据库导出）
-- ============================================
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (1,1) ON DUPLICATE KEY UPDATE `user_id`=VALUES(`user_id`);
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (2,2) ON DUPLICATE KEY UPDATE `user_id`=VALUES(`user_id`);
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (3,3) ON DUPLICATE KEY UPDATE `user_id`=VALUES(`user_id`);

-- ============================================
-- 角色权限关联表数据（从当前数据库导出）
-- ============================================
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,1) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,2) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,3) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,4) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,5) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,6) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,7) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,8) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,9) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,10) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,11) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,12) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,13) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,14) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,15) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,16) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,17) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,18) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,19) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,20) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,21) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,22) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,23) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,24) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,25) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,26) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,27) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,28) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,29) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,30) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,31) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,32) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,33) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,34) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,35) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,36) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,37) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,38) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,39) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,40) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,41) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,42) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,43) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,44) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,45) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (1,46) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,1) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,2) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,3) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,4) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,5) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,6) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,7) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,8) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,9) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,10) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,11) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,12) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,13) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,14) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,15) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,16) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,17) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,18) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,19) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,20) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,21) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,22) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,23) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,24) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,25) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,26) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,27) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,29) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,32) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,33) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,34) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,35) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,36) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,37) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,41) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (2,46) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,1) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,7) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,8) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,14) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,15) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,19) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,20) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,21) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,22) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,23) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,24) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,25) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,26) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,27) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,41) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (3,46) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,1) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,2) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,3) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,7) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,8) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,10) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,14) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,15) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,19) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,20) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,21) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,22) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,23) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,24) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,25) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,26) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES (4,27) ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);
