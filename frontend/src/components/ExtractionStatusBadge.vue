<template><span class="status-badge" :class="cur.cls"><i/>{{ cur.label }}</span></template>
<script setup lang="ts">
import { computed } from 'vue';const props=defineProps<{status:string}>();const map:Record<string,{cls:string;label:string}>={'待确认':{cls:'running',label:'待确认'},'已确认':{cls:'done',label:'已确认'},'待审批':{cls:'pending',label:'待审批'},'已提交审批':{cls:'pending',label:'待审批'},'待提交':{cls:'pending',label:'待提交'},'审批驳回':{cls:'failed',label:'已驳回'},'创建失败':{cls:'failed',label:'创建失败'}};const cur=computed(()=>map[props.status]||{cls:'pending',label:props.status||'未知'})
</script>
<style scoped>.status-badge{display:inline-flex;align-items:center;gap:6px;padding:6px 10px;border-radius:999px;font-size:11px;font-weight:650;white-space:nowrap}.status-badge i{width:6px;height:6px;border-radius:50%;background:currentColor}.done{background:#e7f8ef;color:#078654}.running{background:#e7f0ff;color:#1769e0}.pending{background:#f0f2f5;color:#64748b}.failed{background:#fff0f0;color:#dc3f3f}</style>
