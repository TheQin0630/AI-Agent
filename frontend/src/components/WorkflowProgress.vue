<template>
  <section class="workflow-progress surface-panel" aria-label="采购流程进度">
    <div class="progress-head">
      <div>
        <p class="section-kicker">Workflow</p>
        <h3>整体流程进度</h3>
      </div>
      <span>{{ caption }}</span>
    </div>
    <ol>
      <li
        v-for="(step, index) in steps"
        :key="step.label"
        :class="{ done: index < active, active: index === active }"
      >
        <div class="step-marker">
          <Check v-if="index < active" :size="15" /><span v-else>{{
            index + 1
          }}</span>
        </div>
        <div class="step-copy">
          <strong>{{ step.label }}</strong
          ><small>{{ step.detail }}</small>
        </div>
        <div v-if="index < steps.length - 1" class="connector"><i /></div>
      </li>
    </ol>
  </section>
</template>
<script setup lang="ts">
import { Check } from "lucide-vue-next";
defineProps<{
  steps: Array<{ label: string; detail: string }>;
  active: number;
  caption: string;
}>();
</script>
<style scoped>
.workflow-progress {
  padding: 20px 22px;
  border-radius: 22px;
}
.progress-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.progress-head h3 {
  margin: 0;
  font-size: 15px;
}
.progress-head > span {
  font-size: 12px;
  color: var(--muted-foreground);
}
ol {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}
li {
  position: relative;
  display: flex;
  align-items: center;
  flex-direction: column;
  text-align: center;
  min-width: 0;
}
.step-marker {
  position: relative;
  z-index: 2;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #eef2f7;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
  transition: all 0.35s ease;
}
.done .step-marker,
.active .step-marker {
  background: var(--primary);
  color: white;
  box-shadow: 0 7px 16px rgba(23, 105, 224, 0.22);
}
.active .step-marker {
  animation: pulse 2s infinite;
}
.step-copy {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-top: 9px;
  padding: 0 8px;
}
.step-copy strong {
  font-size: 13px;
  line-height: 16px;
  white-space: nowrap;
}
.step-copy small {
  margin-top: 2px;
  font-size: 11px;
  line-height: 13px;
  color: var(--muted-foreground);
  white-space: nowrap;
}
.connector {
  position: absolute;
  left: 50%;
  right: -50%;
  top: 15px;
  height: 3px;
  overflow: hidden;
  border-radius: 999px;
  background: #dfe7f1;
  z-index: 1;
}
.connector i {
  display: block;
  height: 100%;
  width: 0;
  background: var(--primary);
  transition: width 0.7s ease;
}
.done .connector i {
  width: 100%;
}
@keyframes pulse {
  50% {
    box-shadow: 0 0 0 7px rgba(23, 105, 224, 0.08);
  }
}
@media (max-width: 760px) {
  ol {
    grid-template-columns: 1fr;
    gap: 14px;
  }
  li {
    display: grid;
    grid-template-columns: 40px minmax(0, 1fr);
    align-items: start;
    text-align: left;
  }
  .step-copy {
    margin-top: 0;
    padding: 1px 8px 1px 4px;
  }
  .connector {
    left: 15px;
    right: auto;
    top: 30px;
    bottom: -14px;
    width: 2px;
    height: auto;
  }
  .connector i {
    width: 100%;
    height: 0;
  }
  .done .connector i {
    height: 100%;
  }
  .progress-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
