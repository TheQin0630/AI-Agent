<template>
  <AppShell :navs="navs" search-placeholder="搜索任务、用户或日志...">
    <!-- Hero -->
    <div class="hero">
      <div style="min-width:0">
        <h2 style="text-wrap:balance">风险规则</h2>
        <p>配置合同字段比对规则与风险等级阈值</p>
      </div>
      <div class="hero-actions">
        <button class="action-btn primary" @click="openCreate">
          <Plus :size="16" /> 新增规则
        </button>
      </div>
    </div>

    <!-- Rules table card -->
    <article class="content-card">
      <div class="card-head">
        <div>
          <span class="eyebrow">规则配置</span>
          <strong class="block text-sm" style="margin-top:.2rem">全部规则</strong>
        </div>
        <span class="pill-secondary">{{ rules.length }} 条</span>
      </div>
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th style="min-width:60px">ID</th>
              <th style="min-width:160px">字段</th>
              <th style="min-width:90px">运算符</th>
              <th style="min-width:90px">阈值</th>
              <th style="min-width:90px">风险等级</th>
              <th style="min-width:80px">启用</th>
              <th style="min-width:160px">备注</th>
              <th style="min-width:120px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in rules" :key="r.id">
              <td class="cell-mono">{{ r.id }}</td>
              <td class="cell-task-name">{{ r.fieldKey }}</td>
              <td class="cell-mono">{{ r.operator }}</td>
              <td class="cell-mono">{{ r.thresholdValue === '' || r.thresholdValue == null ? '—' : r.thresholdValue }}</td>
              <td><RiskBadge :level="r.riskLevel" /></td>
              <td>
                <button type="button" class="switch" :class="{ on: !!r.enabled }" @click="toggleEnabled(r)">
                  <span class="switch-thumb"></span>
                </button>
              </td>
              <td class="truncate" style="max-width:200px">{{ r.remark || '—' }}</td>
              <td>
                <div class="op-row">
                  <button class="op-btn" type="button" @click="openEdit(r)">
                    <PenLine :size="13" style="margin-right:4px" /> 编辑
                  </button>
                  <button class="op-btn op-danger" type="button" @click="openDelete(r)">
                    <Trash2 :size="13" style="margin-right:4px" /> 删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!rules.length">
              <td colspan="8" class="text-center py-8" style="color:var(--muted-foreground)">暂无规则</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <!-- Create / Edit Modal -->
    <div v-if="modalOpen" class="modal-mask" @click.self="modalOpen = false">
      <div class="modal-card">
        <div class="modal-title">{{ editForm.id ? '编辑规则' : '新增规则' }}</div>
        <div class="modal-body">
          <div class="field">
            <label>字段 fieldKey</label>
            <input type="text" v-model="editForm.fieldKey" class="ctl-input" placeholder="如 purchaseTotalAmount" />
          </div>
          <div class="field">
            <label>运算符 operator</label>
            <select v-model="editForm.operator" class="ctl-select">
              <option v-for="o in operators" :key="o" :value="o">{{ o }}</option>
            </select>
          </div>
          <div class="field">
            <label>阈值 thresholdValue</label>
            <input type="text" v-model="editForm.thresholdValue" class="ctl-input" placeholder="如 0.05" />
          </div>
          <div class="field">
            <label>风险等级 riskLevel</label>
            <select v-model="editForm.riskLevel" class="ctl-select">
              <option v-for="r in riskLevels" :key="r" :value="r">{{ r }}</option>
            </select>
          </div>
          <div class="field">
            <label>启用</label>
            <button type="button" class="switch" :class="{ on: editForm.enabled }" @click="editForm.enabled = !editForm.enabled">
              <span class="switch-thumb"></span>
              <span class="switch-label">{{ editForm.enabled ? '已启用' : '已禁用' }}</span>
            </button>
          </div>
          <div class="field">
            <label>备注 remark</label>
            <input type="text" v-model="editForm.remark" class="ctl-input" placeholder="规则说明" />
          </div>
        </div>
        <div class="modal-foot">
          <button class="action-btn ghost" type="button" @click="modalOpen = false">取消</button>
          <button class="action-btn primary" type="button" :disabled="saving" @click="save">保存</button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm Modal -->
    <div v-if="deleteOpen" class="modal-mask" @click.self="deleteOpen = false">
      <div class="modal-card" style="max-width:360px">
        <div class="modal-title">删除规则 #{{ deleteTarget?.id }}</div>
        <div style="font-size:.88rem;color:var(--muted-foreground)">确定删除规则「{{ deleteTarget?.fieldKey }}」吗？此操作不可撤销。</div>
        <div class="modal-foot">
          <button class="action-btn ghost" type="button" @click="deleteOpen = false">取消</button>
          <button class="action-btn primary" type="button" :disabled="deleting"
            :style="`background:var(--destructive);border-color:var(--destructive);color:var(--destructive-foreground)`"
            @click="confirmDelete">删除</button>
        </div>
      </div>
    </div>
  </AppShell>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ListTodo, LayoutDashboard, Users, ShieldAlert, FileText, Plus, PenLine, Trash2, ClipboardList } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import RiskBadge from '@/components/RiskBadge.vue'
import { adminApi } from '@/api/admin'
import { useToastStore } from '@/stores/toast'

const navs = [
  { key: 'user-tasks', label: '用户任务', icon: ListTodo, to: '/tasks' },
  { key: 'extractions', label: '采购申请单', icon: ClipboardList, to: '/extractions' },
  { key: 'overview', label: '管理概览', icon: LayoutDashboard, to: '/admin' },
  { key: 'users', label: '用户管理', icon: Users, to: '/users' },
  { key: 'risk', label: '风险规则', icon: ShieldAlert, to: '/risk-rules' },
  { key: 'logs', label: 'LLM 日志', icon: FileText, to: '/llm-logs' }
]

const toast = useToastStore()
const operators = ['GT', 'LT', 'EQ', 'NE', 'PCT_GT']
const riskLevels = ['LOW', 'MEDIUM', 'HIGH']
const rules = ref<any[]>([])

const modalOpen = ref(false)
const saving = ref(false)
const editForm = reactive({ id: 0, fieldKey: '', operator: 'GT', thresholdValue: '', riskLevel: 'MEDIUM', enabled: true, remark: '' })

const deleteOpen = ref(false)
const deleting = ref(false)
const deleteTarget = ref<any>(null)

async function load() {
  try {
    const res: any = await adminApi.listRiskRules()
    rules.value = res.data.items || []
  } catch { /* toast 已由拦截器处理 */ }
}

async function toggleEnabled(r: any) {
  try {
    await adminApi.updateRiskRule(r.id, { enabled: !r.enabled })
    r.enabled = !r.enabled
    toast.success(`已${r.enabled ? '启用' : '禁用'}规则 #${r.id}`)
  } catch { /* toast 已由拦截器处理 */ }
}

function openCreate() {
  editForm.id = 0
  editForm.fieldKey = ''
  editForm.operator = 'GT'
  editForm.thresholdValue = ''
  editForm.riskLevel = 'MEDIUM'
  editForm.enabled = true
  editForm.remark = ''
  modalOpen.value = true
}
function openEdit(r: any) {
  editForm.id = r.id
  editForm.fieldKey = r.fieldKey
  editForm.operator = r.operator
  editForm.thresholdValue = r.thresholdValue ?? ''
  editForm.riskLevel = r.riskLevel
  editForm.enabled = !!r.enabled
  editForm.remark = r.remark || ''
  modalOpen.value = true
}

async function save() {
  if (!editForm.fieldKey) return toast.warning('请填写字段名')
  saving.value = true
  const body = {
    fieldKey: editForm.fieldKey,
    operator: editForm.operator,
    thresholdValue: editForm.thresholdValue,
    riskLevel: editForm.riskLevel,
    enabled: editForm.enabled,
    remark: editForm.remark
  }
  try {
    if (editForm.id) {
      await adminApi.updateRiskRule(editForm.id, body)
      toast.success('保存成功')
    } else {
      await adminApi.createRiskRule(body)
      toast.success('新增成功')
    }
    modalOpen.value = false
    load()
  } catch { /* toast 已由拦截器处理 */ } finally { saving.value = false }
}

function openDelete(r: any) {
  deleteTarget.value = r
  deleteOpen.value = true
}
async function confirmDelete() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await adminApi.deleteRiskRule(deleteTarget.value.id)
    toast.success('删除成功')
    deleteOpen.value = false
    load()
  } catch { /* toast 已由拦截器处理 */ } finally { deleting.value = false }
}

onMounted(load)
</script>
<style scoped>
.card-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: calc(var(--spacing) * 4) calc(var(--spacing) * 5);
  border-bottom: 1px solid var(--border);
}
.eyebrow { font-size: .78rem; text-transform: uppercase; letter-spacing: .08em; font-weight: 600; color: var(--muted-foreground); }
.pill-secondary {
  display: inline-flex; align-items: center; gap: .35rem;
  padding: .34rem .65rem; border-radius: 999px;
  background: var(--secondary); color: var(--secondary-foreground); font-size: .8rem; font-weight: 500;
}
.op-row { display: flex; align-items: center; justify-content: flex-end; gap: calc(var(--spacing) * 2); }
.op-btn {
  display: inline-flex; align-items: center; justify-content: center;
  padding: calc(var(--spacing) * 2) calc(var(--spacing) * 3);
  border-radius: var(--radius); border: 1px solid var(--border);
  background: transparent; color: var(--foreground);
  font: 500 0.75rem/1 var(--font-sans); cursor: pointer;
  transition: background 0.15s ease;
}
.op-btn:hover { background: var(--accent); color: var(--accent-foreground); }
.op-danger { color: var(--chart-2); }
.op-danger:hover { background: color-mix(in srgb, var(--chart-2) 12%, var(--background)); }

/* Switch（沿用设计稿 toggle，蓝底圆角） */
.switch {
  display: inline-flex; align-items: center; gap: calc(var(--spacing) * 3);
  border: none; background: transparent; cursor: pointer; padding: 0; font: inherit;
  --w: 38px; --h: 22px;
}
.switch .switch-thumb {
  width: var(--w); height: var(--h); border-radius: 999px;
  background: var(--muted); position: relative; transition: background 0.2s ease;
  flex-shrink: 0;
}
.switch .switch-thumb::after {
  content: ''; position: absolute; top: 2px; left: 2px;
  width: calc(var(--h) - 4px); height: calc(var(--h) - 4px); border-radius: 50%;
  background: var(--card); transition: transform 0.2s ease;
}
.switch.on .switch-thumb { background: var(--primary); }
.switch.on .switch-thumb::after { transform: translateX(calc(var(--w) - var(--h))); }
.switch-label { font-size: .82rem; color: var(--foreground); }

/* Modal（沿用设计系统卡片样式） */
.modal-mask {
  position: fixed; inset: 0; z-index: 60;
  background: rgba(14, 17, 21, 0.42);
  display: grid; place-items: center; padding: calc(var(--spacing) * 4);
}
.modal-card {
  width: 100%; max-width: 460px;
  background: var(--card); color: var(--card-foreground);
  border: 1px solid var(--border); border-radius: var(--radius);
  box-shadow: var(--shadow-lg); padding: calc(var(--spacing) * 6);
  display: flex; flex-direction: column; gap: calc(var(--spacing) * 5);
  max-height: 90vh; overflow-y: auto;
}
.modal-title { font-size: 1.05rem; font-weight: 600; }
.modal-body { display: flex; flex-direction: column; gap: calc(var(--spacing) * 4); }
.field { display: flex; flex-direction: column; gap: calc(var(--spacing) * 2); }
.field label { font-size: .78rem; font-weight: 500; color: var(--muted-foreground); }
.ctl-select, .ctl-input {
  height: 32px; padding: 0 calc(var(--spacing) * 3);
  border: 1px solid var(--input); border-radius: var(--radius);
  background: var(--popover); color: var(--foreground); font: inherit; font-size: .85rem;
  outline: none; width: 100%;
}
.modal-foot { display: flex; justify-content: flex-end; gap: calc(var(--spacing) * 3); }
</style>
