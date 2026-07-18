<template>
  <button
    type="button"
    class="metric-card"
    :class="{ active }"
    :aria-pressed="active"
    :aria-label="`${label}：${value}${suffix}，点击筛选`"
    @click="$emit('click')"
  >
    <div class="metric-icon" :class="tone">
      <component :is="icon" :size="21" />
    </div>
    <div class="metric-copy">
      <span>{{ label }}</span>
      <strong
        >{{ value }}<small>{{ suffix }}</small></strong
      >
      <p>{{ hint }}</p>
    </div>
    <span class="metric-action">{{ active ? "查看中" : "查看" }}</span>
  </button>
</template>
<script setup lang="ts">
import type { Component } from "vue";
withDefaults(
  defineProps<{
    label: string;
    value: number | string;
    suffix?: string;
    hint: string;
    icon: Component;
    tone?: "blue" | "amber" | "green";
    active?: boolean;
  }>(),
  { suffix: "", tone: "blue", active: false },
);
defineEmits<{ click: [] }>();
</script>
<style scoped>
.metric-card {
  position: relative;
  display: grid;
  grid-template-columns: 50px minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: 22px;
  background: var(--card);
  box-shadow: var(--shadow-sm);
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.28s cubic-bezier(0.2, 0.8, 0.2, 1),
    box-shadow 0.28s ease,
    border-color 0.28s ease,
    background 0.28s ease;
}
.metric-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: #b8cce8;
}
.metric-card:focus-visible {
  outline: 3px solid rgba(23, 105, 224, 0.2);
  outline-offset: 3px;
}
.metric-card.active {
  border-color: #c9ddfb;
  background: #f3f7ff;
  box-shadow: 0 7px 20px rgba(23, 105, 224, 0.09);
}
.metric-card.active .metric-copy > span {
  color: #155fc9;
  font-size: 13px;
  font-weight: 750;
}
.metric-icon {
  width: 50px;
  height: 50px;
  border-radius: 17px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
}
.metric-icon.blue {
  color: #1769e0;
  background: #e8f1ff;
}
.metric-icon.amber {
  color: #d97706;
  background: #fff5df;
}
.metric-icon.green {
  color: #059669;
  background: #e5f8f0;
}
.metric-copy {
  display: flex;
  min-height: 64px;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
}
.metric-copy > span {
  font-size: 12px;
  color: var(--muted-foreground);
  font-weight: 650;
}
.metric-copy strong {
  display: flex;
  align-items: baseline;
  margin-top: 2px;
  font-size: 26px;
  line-height: 1.15;
  letter-spacing: -0.03em;
}
.metric-copy small {
  margin-left: 5px;
  font-size: 12px;
  color: var(--muted-foreground);
  font-weight: 500;
}
.metric-copy p {
  margin: 5px 0 0;
  color: var(--muted-foreground);
  font-size: 12px;
}
.metric-action {
  position: absolute;
  right: 16px;
  top: 14px;
  color: var(--primary);
  padding: 3px 8px;
  border-radius: 999px;
  background: #e7f1ff;
  font-size: 11px;
  font-weight: 700;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.2s;
}
.metric-card:hover .metric-action,
.metric-card:focus-visible .metric-action,
.metric-card.active .metric-action {
  opacity: 1;
  transform: none;
}
.metric-card.active .metric-action {
  font-size: 12px;
  color: #0f5fc9;
}
</style>
