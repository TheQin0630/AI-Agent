-- 新增 source_json / result_json 列：供 Dify Agent 采购申请创建节点
-- 写入原始输入 JSON（source_json）与组装后的结果 JSON（result_json）
-- 原 extraction 表无对应列，Dify 脚本 INSERT 时会报 Unknown column 错误
ALTER TABLE extraction
    ADD COLUMN source_json LONGTEXT NULL,
    ADD COLUMN result_json LONGTEXT NULL;
