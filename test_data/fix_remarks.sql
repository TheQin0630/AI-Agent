-- 修复 risk_rule 表中双重编码的中文 remark
SET NAMES utf8mb4;
UPDATE risk_rule SET remark = '金额差 > 5%' WHERE id = 1;
UPDATE risk_rule SET remark = '数量不一致' WHERE id = 2;
UPDATE risk_rule SET remark = '交付日期差 > 7 天' WHERE id = 3;
UPDATE risk_rule SET remark = '付款条款不一致' WHERE id = 4;
UPDATE risk_rule SET remark = '交付地点不一致' WHERE id = 5;
