-- 显式声明连接字符集，避免中文 remark 被双 UTF-8 编码（mojibake）
SET NAMES utf8mb4;

INSERT INTO risk_rule (field_key, operator, threshold_value, risk_level, enabled, remark) VALUES
('purchaseTotalAmount', 'PCT_GT', 0.05,    'HIGH',   1, '金额差 > 5%'),
('quantity',            'NE',     NULL,     'HIGH',   1, '数量不一致'),
('expectedDeliveryDate','GT',     7,        'MEDIUM', 1, '交付日期差 > 7 天'),
('paymentTerms',        'NE',     NULL,     'MEDIUM', 1, '付款条款不一致'),
('deliveryLocation',    'NE',     NULL,     'MEDIUM', 1, '交付地点不一致');
