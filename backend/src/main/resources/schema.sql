CREATE TABLE IF NOT EXISTS app_users (
 id VARCHAR(36) PRIMARY KEY,
 username VARCHAR(32) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(16) NOT NULL,
 created_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS user_favorites (
 user_id VARCHAR(36) NOT NULL,
 attraction_id BIGINT NOT NULL,
 created_at BIGINT NOT NULL,
 PRIMARY KEY (user_id, attraction_id),
 FOREIGN KEY (user_id) REFERENCES app_users(id)
);
CREATE TABLE IF NOT EXISTS user_history (
 user_id VARCHAR(36) NOT NULL,
 attraction_id BIGINT NOT NULL,
 viewed_at BIGINT NOT NULL,
 PRIMARY KEY (user_id, attraction_id),
 FOREIGN KEY (user_id) REFERENCES app_users(id)
);
CREATE TABLE IF NOT EXISTS feedback_exposures (
 id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL,
 created_at BIGINT NOT NULL,
 FOREIGN KEY (user_id) REFERENCES app_users(id)
);
CREATE TABLE IF NOT EXISTS feedback_exposure_items (
 exposure_id VARCHAR(36) NOT NULL,
 attraction_id BIGINT NOT NULL,
 position_no INT NOT NULL,
 PRIMARY KEY (exposure_id, attraction_id),
 FOREIGN KEY (exposure_id) REFERENCES feedback_exposures(id)
);
CREATE TABLE IF NOT EXISTS user_feedback (
 id VARCHAR(36) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL,
 attraction_id BIGINT NOT NULL,
 exposure_id VARCHAR(36),
 rating INT NOT NULL,
 comment_text VARCHAR(500) NOT NULL,
 created_at BIGINT NOT NULL,
 FOREIGN KEY (user_id) REFERENCES app_users(id),
 FOREIGN KEY (exposure_id) REFERENCES feedback_exposures(id)
);
