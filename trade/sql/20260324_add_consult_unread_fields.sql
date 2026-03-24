ALTER TABLE consult_session
  ADD COLUMN buyer_last_read_time DATETIME NULL AFTER seller_id,
  ADD COLUMN seller_last_read_time DATETIME NULL AFTER buyer_last_read_time;
