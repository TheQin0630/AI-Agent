-- 用户
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 对比任务
CREATE TABLE IF NOT EXISTS comparison_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    summary TEXT,
    risk_level VARCHAR(16),
    confirmed_at DATETIME,
    confirmed_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 合同
CREATE TABLE IF NOT EXISTS contract (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    side VARCHAR(8) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_path VARCHAR(512) NOT NULL,
    mime_type VARCHAR(64),
    extracted_text LONGTEXT,
    file_size BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_side (task_id, side)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 抽取结果
CREATE TABLE IF NOT EXISTS extraction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL UNIQUE,
    supplier_name VARCHAR(255),
    item_name VARCHAR(255),
    item_model VARCHAR(255),
    unit VARCHAR(32),
    quantity DECIMAL(18,4),
    purchase_unit_price DECIMAL(18,4),
    purchase_total_amount DECIMAL(18,4),
    expected_delivery_date DATE,
    payment_terms TEXT,
    delivery_location VARCHAR(255),
    application_no VARCHAR(64),
    application_title VARCHAR(255),
    apply_date DATE,
    application_type VARCHAR(32),
    currency VARCHAR(8),
    application_status VARCHAR(16),
    create_time DATETIME,
    message VARCHAR(255),
    confidence DECIMAL(3,2),
    raw_quote TEXT,
    extracted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 对话消息
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    tool_calls JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_created (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 风险规则
CREATE TABLE IF NOT EXISTS risk_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_key VARCHAR(64) NOT NULL,
    operator VARCHAR(8) NOT NULL,
    threshold_value DECIMAL(18,4),
    risk_level VARCHAR(16) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(255),
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- LLM 调用日志
CREATE TABLE IF NOT EXISTS llm_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT,
    user_id BIGINT,
    model VARCHAR(64) NOT NULL,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    duration_ms INT,
    status VARCHAR(16) NOT NULL,
    error_msg TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 注意：初始管理员账号由 DataInitializer 在应用启动时创建（admin / admin123），
-- 使用 BCryptPasswordEncoder 生成 hash，避免 SQL 中硬编码 hash 不匹配的问题。
