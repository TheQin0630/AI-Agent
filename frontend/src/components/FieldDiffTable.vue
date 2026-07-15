<template>
  <div class="overflow-x-auto">
    <table class="diff-table">
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
          <td class="font-medium">{{ fieldLabel(d.field) }}</td>
          <td class="truncate">{{ fmt(d.buy) }}</td>
          <td class="truncate" :class="{ 'cell-missing': d.status === 'MISSING' }">{{ fmt(d.sell) }}</td>
          <td><span class="diff-status" :class="statusCls(d.status)">{{ d.status }}</span></td>
          <td>
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
/* 拷贝自 design/pages/任务详情.html 的字段差异表样式 */
.diff-table { width: 100%; border-collapse: collapse; }
.diff-table th {
  padding: 12px 16px; text-align: left; white-space: nowrap;
  font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.08em;
  color: var(--muted-foreground); font-weight: 600;
  background: var(--muted);
}
.diff-table td {
  padding: 12px 16px; border-bottom: 1px solid var(--border);
  font-size: 0.875rem; color: var(--foreground); min-width: 100px;
}
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
</style>
