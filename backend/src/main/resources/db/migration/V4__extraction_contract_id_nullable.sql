-- 放宽 extraction.contract_id 约束：允许 Dify Agent 独立创建申请单（无合同上下文）
-- 原始约束：NOT NULL UNIQUE（每条抽取必须绑定一个合同）
-- 新约束：可为 NULL（Dify 直创建场景）；UNIQUE 保留，避免重复绑定
ALTER TABLE extraction MODIFY COLUMN contract_id BIGINT NULL;
