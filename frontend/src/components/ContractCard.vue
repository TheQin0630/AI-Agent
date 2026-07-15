<template>
  <article class="contract-card">
    <div class="contract-head" :style="headStyle">
      <component :is="side === 'BUY' ? FileText : FileText" :size="20" :style="{ color: headColor }" />
      <div class="min-w-0 flex-1">
        <div class="text-sm font-semibold truncate" style="color:var(--foreground)">{{ side === 'BUY' ? '采购合同' : '销售合同' }}</div>
        <div class="text-xs truncate" style="color:var(--muted-foreground)">{{ filename || '—' }}</div>
      </div>
    </div>
    <div class="contract-grid">
      <div class="flex flex-col" v-for="f in fields" :key="f.key">
        <span class="text-xs mb-0.5" style="color:var(--muted-foreground)">{{ f.label }}</span>
        <span class="font-medium truncate" :style="f.bold ? `color:${headColor};font-weight:700` : 'color:var(--foreground)'">{{ f.value }}</span>
      </div>
      <div class="flex flex-col col-span-2">
        <span class="text-xs mb-0.5" style="color:var(--muted-foreground)">置信度</span>
        <div class="flex items-center gap-2">
          <div class="flex-1 h-2 rounded-full" style="background:var(--muted);max-width:200px">
            <div class="h-2 rounded-full" :style="`width:${confidencePct}%;background:${headColor}`"></div>
          </div>
          <span class="text-xs font-medium" :style="`color:${headColor}`">{{ confidencePct }}%</span>
        </div>
      </div>
    </div>
  </article>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { FileText } from 'lucide-vue-next'

const props = defineProps<{ side: 'BUY' | 'SELL'; ex?: any; filename?: string }>()

const headColor = computed(() => props.side === 'BUY' ? 'var(--chart-1)' : 'var(--chart-5)')
const headStyle = computed(() => ({
  background: `color-mix(in srgb, ${props.side === 'BUY' ? 'var(--chart-1)' : 'var(--chart-5)'} 8%, var(--card))`,
  borderBottom: '1px solid var(--border)'
}))
const confidencePct = computed(() => {
  const c = Number(props.ex?.confidence)
  return c && !isNaN(c) ? Math.round(c * 100) : 0
})
const fields = computed(() => {
  const e = props.ex || {}
  return [
    { key: 'supplierName', label: '供应商', value: fmt(e.supplierName) },
    { key: 'itemName', label: '品名', value: fmt(e.itemName) },
    { key: 'itemModel', label: '型号', value: fmt(e.itemModel) },
    { key: 'unit', label: '单位', value: fmt(e.unit) },
    { key: 'quantity', label: '数量', value: fmt(e.quantity) },
    {
      key: 'unitPrice',
      label: props.side === 'BUY' ? '采购单价' : '销售单价',
      value: fmt(props.side === 'BUY' ? e.purchaseUnitPrice : e.purchaseUnitPrice)
    },
    {
      key: 'total',
      label: props.side === 'BUY' ? '采购总额' : '销售总额',
      value: fmt(e.purchaseTotalAmount),
      bold: true
    },
    { key: 'expectedDeliveryDate', label: '预交日期', value: fmt(e.expectedDeliveryDate) },
    { key: 'paymentTerms', label: '付款条款', value: fmt(e.paymentTerms) },
    { key: 'deliveryLocation', label: '交货地点', value: fmt(e.deliveryLocation) }
  ]
})
function fmt(v: any) {
  if (v === null || v === undefined || v === '') return '—'
  if (typeof v === 'number') return v.toLocaleString()
  return String(v)
}
</script>
<style scoped>
/* 拷贝自 design/pages/任务详情.html 的双列对比卡片样式 */
.contract-card {
  background: var(--card); border: 1px solid var(--border);
  border-radius: calc(var(--radius) + 6px); box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.contract-head {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 20px;
}
.contract-grid {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 12px 16px; padding: 16px 20px; font-size: 0.875rem;
}
</style>
