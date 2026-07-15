-- 扩容 mime_type 列：浏览器上传的 Content-Type 可能超过 64 字符
-- 例如 application/vnd.openxmlformats-officedocument.wordprocessingml.document (71 字符)
ALTER TABLE contract MODIFY COLUMN mime_type VARCHAR(255) NOT NULL DEFAULT '';
