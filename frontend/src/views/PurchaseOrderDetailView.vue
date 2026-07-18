<template>
  <AppShell :navs="navs" search-placeholder="搜索采购订单">
    <main v-if="order" class="detail-page">
      <header class="detail-header"><div><p class="section-kicker">Purchase Order</p><div class="title-line"><h2>{{ order.orderNo }}</h2><span class="status-pill"><i />已提交</span></div><p>由采购申请审批通过并完成库存对比后生成的正式订单</p></div><router-link class="action-btn ghost" to="/purchase-orders"><ChevronLeft :size="18" />返回订单列表</router-link></header>
      <section class="layout">
        <article class="card wide"><CardTitle :icon="FileCheck2" title="订单信息" /><dl class="fields three"><Info label="采购订单编号" :value="order.orderNo" mono /><Info label="对应采购申请单编号" :value="order.applicationNo || '—'" mono /><Info label="创建时间" :value="formatTime(order.createdAt)" /></dl></article>
        <article class="card"><CardTitle :icon="Building2" title="供应商信息" /><dl class="fields"><Info label="供应商名称" :value="clean(order.supplierName)" /><Info label="发货方式" :value="clean(order.shippingMethod)" /><Info label="预计交付时间" :value="order.expectedDeliveryDate || '待补充'" /><Info label="付款条款" :value="clean(order.paymentTerms)" /></dl></article>
        <article class="card"><CardTitle :icon="Package" title="商品信息" /><dl class="fields"><Info label="商品名称" :value="clean(order.itemName)" /><Info label="规格型号" :value="clean(order.itemModel)" /><Info label="单位" :value="clean(order.unit)" /><Info label="采购单价" :value="money(order.unitPrice)" /><Info label="数量" :value="order.orderQuantity || '—'" /><Info label="币种名称" :value="clean(order.currency)" /><Info label="含税采购金额" :value="money(order.taxInclusiveAmount)" highlight /><Info label="税率名称" :value="clean(order.taxRateName)" /></dl></article>
      </section>
    </main><div v-else class="skeleton loading" />
  </AppShell>
</template>
<script setup lang="ts">
import { defineComponent, h, onMounted, ref } from "vue"; import type { Component } from "vue"; import { useRoute } from "vue-router";
import { Building2, ChevronLeft, FileCheck2, ListTodo, Package } from "lucide-vue-next"; import AppShell from "@/components/AppShell.vue"; import { purchaseOrderApi, type PurchaseOrderDetail } from "@/api/purchaseOrder";
const route=useRoute(),order=ref<PurchaseOrderDetail|null>(null),navs=[{key:"tasks",label:"合同对比",icon:ListTodo,to:"/tasks"}];
const Info=defineComponent({props:{label:String,value:[String,Number],mono:Boolean,highlight:Boolean},setup:p=>()=>h("div",{class:["field",{mono:p.mono,highlight:p.highlight}]},[h("dt",p.label),h("dd",p.value??"—")])});
const CardTitle=defineComponent({props:{icon:Object as ()=>Component,title:String},setup:p=>()=>h("div",{class:"card-title"},[h(p.icon!,{size:20}),h("h3",p.title)])});
function clean(v:string|null|undefined){return v?.trim()||"待补充"} function formatTime(v:string){return v?.replace("T"," ").slice(0,16)||"—"} function money(v:string){return v?`${Number(v).toLocaleString("zh-CN",{minimumFractionDigits:2})} ${order.value?.currency||""}`:"—"}
onMounted(async()=>{const r:any=await purchaseOrderApi.get(Number(route.params.id));order.value=r.data});
</script>
<style scoped>
.detail-page{display:grid;gap:26px}.detail-header{display:flex;align-items:center;justify-content:space-between;gap:20px}.detail-header p:last-child{margin:8px 0 0;color:var(--muted-foreground)}.detail-header a{text-decoration:none}.title-line{display:flex;align-items:center;gap:12px}.title-line h2{margin:0;font-size:30px}.status-pill{display:inline-flex;align-items:center;gap:7px;padding:7px 11px;border-radius:999px;background:#e9f8f1;color:#16805b;font-size:12px}.status-pill i{width:6px;height:6px;border-radius:50%;background:#19a974}.layout{display:grid;grid-template-columns:1fr 1.4fr;gap:22px}.card{padding:28px;border:1px solid #e0e7f0;border-radius:20px;background:#fff;box-shadow:0 10px 30px rgba(35,61,94,.05)}.card.wide{grid-column:1/-1}:deep(.card-title){display:flex;align-items:center;gap:10px;margin-bottom:24px;color:#2878e5}:deep(.card-title h3){margin:0;color:#50637e;font-size:17px}.fields{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:24px 28px;margin:0}.fields.three{grid-template-columns:repeat(3,minmax(0,1fr))}:deep(.field dt){color:#8492a6;font-size:13px;font-weight:550}:deep(.field dd){margin:7px 0 0;color:#162033;font-size:16px;line-height:1.55;overflow-wrap:anywhere}:deep(.field.mono dd){font-family:var(--font-mono)}:deep(.field.highlight dd){color:#1769e0;font-size:19px;font-weight:650}.loading{height:540px}@media(max-width:900px){.layout{grid-template-columns:1fr}.card.wide{grid-column:auto}}@media(max-width:650px){.detail-header{align-items:flex-start;flex-direction:column}.fields,.fields.three{grid-template-columns:1fr}}
</style>
