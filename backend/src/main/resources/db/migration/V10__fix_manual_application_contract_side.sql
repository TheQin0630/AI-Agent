-- 销售合同只参与合同对比，不是采购申请单。清理历史误生成的申请元数据。
UPDATE extraction e
JOIN contract c ON c.id = e.contract_id
SET e.application_no = NULL,
    e.application_status = NULL,
    e.application_title = NULL,
    e.application_type = NULL,
    e.apply_date = NULL,
    e.message = '销售合同抽取完成，仅用于合同对比'
WHERE c.side = 'SELL';

-- 手动对比中的采购合同在抽取完成后即进入审批，不保留“待提交”中间态。
UPDATE extraction e
JOIN contract c ON c.id = e.contract_id
SET e.application_status = '已提交审批',
    e.message = '合同对比任务已创建，采购申请已提交审批'
WHERE c.side = 'BUY'
  AND e.application_status = '待提交';
