-- 为商品增加校内模糊交易地点字段
ALTER TABLE `product`
  ADD COLUMN `trade_location` VARCHAR(64) NULL COMMENT '校内模糊交易地点（如东区/图书馆附近）' AFTER `description`;
