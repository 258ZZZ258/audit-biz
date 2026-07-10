-- 用户信息表增量变更：补充头像、部门和角色字段。
ALTER TABLE `idap_user_info`
    ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL' AFTER `note`;

ALTER TABLE `idap_user_info`
    ADD COLUMN `department_id` VARCHAR(32) DEFAULT NULL COMMENT '部门 ID' AFTER `mobile`;

ALTER TABLE `idap_user_info`
    ADD COLUMN `role_id` VARCHAR(32) DEFAULT NULL COMMENT '角色 ID' AFTER `department_id`;

ALTER TABLE `idap_user_info`
    ADD INDEX `idx_department_id` (`department_id`);

ALTER TABLE `idap_user_info`
    ADD INDEX `idx_role_id` (`role_id`);

-- 截图中的 DELETE 和演示用户 INSERT 属于本地初始化数据，不进入版本迁移，
-- 避免在已存在用户的环境中造成数据丢失或污染。
