<template>
  <div class="diff-table-wrap">
    <table class="diff-table">
      <colgroup>
        <col class="col-field" />
        <col class="col-value" />
        <col class="col-value" />
        <col class="col-status" />
        <col class="col-risk" />
      </colgroup>
      <thead>
        <tr>
          <th>字段</th>
          <th>采购值</th>
          <th>销售值</th>
          <th>状态</th>
          <th>风险</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(d, i) in diffs" :key="i">
          <td data-label="字段" class="field-name">{{ fieldLabel(d.field) }}</td>
          <td data-label="采购值" class="field-value">{{ fmt(d.buy) }}</td>
          <td data-label="销售值" class="field-value" :class="{ 'cell-missing': d.status === 'MISSING' }">{{ fmt(d.sell) }}</td>
          <td data-label="状态"><span class="diff-status" :class="statusCls(d.status)">{{ d.status }}</span></td>
          <td data-label="风险">
            <span v-if="d.risk" class="diff-status" :class="riskCls(d.risk)">{{ d.risk }}</span>
            <span v-else class="diff-status risk-none">&mdash;</span>
          </td>
        </tr>
        <tr v-if="!diffs || !diffs.length">
          <td colspan="5" class="text-center py-6" style="color:var(--muted-foreground)">暂无差异</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
<script setup lang="ts">
defineProps<{ diffs: any[] }>()

const fieldMap: Record<string, string> = {
  supplierName: '供应商',
  itemName: '品名',
  itemModel: '型号',
  quantity: '数量',
  purchaseUnitPrice: '单价',
  purchaseTotalAmount: '总额',
  expectedDeliveryDate: '预交日期',
  paymentTerms: '付款条款',
  deliveryLocation: '交货地点'
}
function fieldLabel(f: string) { return fieldMap[f] || f }
function fmt(v: any) {
  if (v === null || v === undefined || v === '') return '— (缺失)'
  return String(v)
}
function statusCls(s: string) {
  return {
    MATCH: 'st-match',
    DIFFER: 'st-differ',
    MISSING: 'st-missing'
  }[s] || 'st-match'
}
function riskCls(r: string) {
  return {
    HIGH: 'risk-high',
    MEDIUM: 'risk-medium',
    LOW: 'risk-low'
  }[r] || 'risk-none'
}
</script>
<style scoped>
.diff-table-wrap { width: 100%; overflow: hidden; }
.diff-table { width: 100%; table-layout: fixed; border-collapse: collapse; }
.col-field { width: 16%; }
.col-value { width: 28%; }
.col-status { width: 15%; }
.col-risk { width: 13%; }
.diff-table th {
  padding: 12px 14px; text-align: left; white-space: normal;
  font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.08em;
  color: var(--muted-foreground); font-weight: 600;
  background: var(--muted);
}
.diff-table td {
  padding: 13px 14px; border-bottom: 1px solid #edf1f6;
  font-size: 0.875rem; color: var(--foreground);
  vertical-align: top;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}
.field-name { font-weight: 650; color: #25324a; }
.field-value { line-height: 1.55; color: #3f4d63; }
.diff-table tbody tr:last-child td { border-bottom: none; }
.cell-missing { color: var(--chart-3); }
.diff-status {
  display: inline-flex; align-items: center;
  padding: 4px 10px; border-radius: 999px;
  font-size: 0.75rem; font-weight: 500; white-space: nowrap;
}
.st-match { background: color-mix(in srgb, var(--chart-5) 18%, var(--background)); color: var(--chart-5); }
.st-differ { background: color-mix(in srgb, var(--chart-2) 18%, var(--background)); color: var(--chart-2); }
.st-missing { background: color-mix(in srgb, var(--chart-3) 18%, var(--background)); color: var(--chart-3); }
.risk-high { background: color-mix(in srgb, var(--chart-2) 18%, var(--background)); color: var(--chart-2); }
.risk-medium { background: color-mix(in srgb, var(--chart-3) 18%, var(--background)); color: var(--chart-3); }
.risk-low { background: color-mix(in srgb, var(--chart-5) 18%, var(--background)); color: var(--chart-5); }
.risk-none { background: var(--muted); color: var(--muted-foreground); }
@media (max-width: 720px) {
  .diff-table colgroup,
  .diff-table thead { display: none; }
  .diff-table,
  .diff-table tbody,
  .diff-table tr,
  .diff-table td { display: block; width: 100%; }
  .diff-table tbody { padding: 10px 12px; }
  .diff-table tr {
    margin-bottom: 10px;
    padding: 8px 12px;
    border-radius: 12px;
    background: #f8fafc;
  }
  .diff-table td {
    display: grid;
    grid-template-columns: 72px minmax(0, 1fr);
    gap: 10px;
    padding: 7px 0;
    border: 0;
  }
  .diff-table td::before {
    content: attr(data-label);
    color: var(--muted-foreground);
    font-size: 12px;
    font-weight: 650;
  }
}
</style>
