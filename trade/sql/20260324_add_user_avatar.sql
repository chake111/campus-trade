-- 为 user 表增加头像字段（最小闭环：头像 URL）
ALTER TABLE `user`
ADD COLUMN `avatar` VARCHAR(255) NULL COMMENT '头像 URL' AFTER `role`;
