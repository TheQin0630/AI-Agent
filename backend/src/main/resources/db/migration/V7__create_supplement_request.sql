CREATE TABLE supplement_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    business_id VARCHAR(128) NOT NULL,
    supplement_type VARCHAR(32) NOT NULL,
    supplement_content TEXT NOT NULL,
    original_request_json JSON NULL,
    request_key VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    submitted_content TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplement_request_key (request_key),
    KEY idx_supplement_business_id (business_id),
    KEY idx_supplement_status (status)
);
