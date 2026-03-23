-- 为 user 表增加角色字段（最小权限方案）
ALTER TABLE `user`
ADD COLUMN `role` varchar(20) NOT NULL DEFAULT 'USER' COMMENT '角色(USER/ADMIN)' AFTER `password`;

-- 将已有数据中 role 为空的记录兜底为 USER（幂等执行可按需保留）
UPDATE `user`
SET `role` = 'USER'
WHERE `role` IS NULL OR TRIM(`role`) = '';

-- 将指定管理员账号设为 ADMIN（示例：按用户名）
UPDATE `user`
SET `role` = 'ADMIN'
WHERE `username` = 'admin';

-- 也可按主键设置管理员（示例）
-- UPDATE `user` SET `role` = 'ADMIN' WHERE `id` = 1;
