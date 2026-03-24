CREATE TABLE IF NOT EXISTS consult_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  buyer_last_read_time DATETIME NULL,
  seller_last_read_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_consult_session_product_buyer (product_id, buyer_id),
  KEY idx_consult_session_buyer (buyer_id),
  KEY idx_consult_session_seller (seller_id),
  CONSTRAINT fk_consult_session_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consult_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_consult_message_session_time (session_id, create_time),
  CONSTRAINT fk_consult_message_session FOREIGN KEY (session_id) REFERENCES consult_session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
