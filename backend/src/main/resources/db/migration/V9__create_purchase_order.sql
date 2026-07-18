CREATE TABLE product_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    item_model VARCHAR(255),
    item_model_key VARCHAR(255) NOT NULL DEFAULT '',
    unit VARCHAR(32) NOT NULL,
    available_quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_identity (item_name, item_model_key, unit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE purchase_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    task_id BIGINT NOT NULL,
    application_no VARCHAR(64),
    product_id BIGINT NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    item_model VARCHAR(255),
    unit VARCHAR(32) NOT NULL,
    requested_quantity DECIMAL(18,4) NOT NULL,
    inventory_quantity DECIMAL(18,4) NOT NULL,
    order_quantity DECIMAL(18,4) NOT NULL,
    unit_price DECIMAL(18,4) NOT NULL,
    total_amount DECIMAL(18,4) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    expected_delivery_date DATE,
    delivery_location VARCHAR(255),
    payment_terms TEXT,
    inventory_check_status VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at DATETIME,
    UNIQUE KEY uk_purchase_order_no (order_no),
    UNIQUE KEY uk_purchase_order_task (task_id),
    INDEX idx_purchase_order_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_notification_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    order_id BIGINT,
    recipient_open_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    payload_json JSON NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME,
    last_error TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_notification_event (task_id, event_type),
    INDEX idx_notification_retry (status, next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
