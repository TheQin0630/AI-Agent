<template>
  <AppShell :navs="navs" search-placeholder="搜索用户...">
    <!-- Hero -->
    <div class="hero">
      <div style="min-width:0">
        <h2 style="text-wrap:balance">用户管理</h2>
        <p>管理系统用户、角色分配和账号状态</p>
      </div>
      <div class="hero-actions">
        <button class="action-btn primary" @click="onSearch">
          <Search :size="15" /> 搜索用户
        </button>
      </div>
    </div>

    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="filter-search">
        <Search :size="15" :style="{ color: 'var(--muted-foreground)', flexShrink: 0 }" />
        <input type="text" v-model="keyword" placeholder="输入用户名搜索..."
          class="border-0 outline-none bg-transparent flex-1 text-sm"
          style="color:var(--foreground);font-family:var(--font-sans)"
          @keydown.enter="onSearch" />
      </div>
      <div class="filter-chips no-scrollbar">
        <button v-for="opt in filters" :key="opt" type="button"
          class="chip" :class="{ active: activeFilter === opt }"
          @click="activeFilter = opt; page = 1; load()">{{ opt }}</button>
      </div>
    </div>

    <!-- Users table card -->
    <article class="content-card">
      <div class="card-head">
        <div>
          <span class="eyebrow">用户列表</span>
          <strong class="block text-sm" style="margin-top:.2rem">全部用户</strong>
        </div>
        <span class="pill-secondary">{{ total }} 用户</span>
      </div>
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th style="min-width:60px">用户ID</th>
              <th style="min-width:120px">用户名</th>
              <th style="min-width:80px">角色</th>
              <th style="min-width:80px">状态</th>
              <th style="min-width:60px">任务数</th>
              <th style="min-width:100px">注册时间</th>
              <th style="min-width:120px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id" :style="u.enabled ? '' : 'opacity:.64'">
              <td class="cell-mono">{{ u.id }}</td>
              <td class="cell-task-name truncate" style="max-width:180px">{{ u.username }}</td>
              <td>
                <span class="badge" :class="u.role === 'ADMIN' ? 'role-admin' : 'role-user'">{{ u.role }}</span>
              </td>
              <td>
                <span class="badge" :class="u.enabled ? 'st-on' : 'st-off'">{{ u.enabled ? '已启用' : '已禁用' }}</span>
              </td>
              <td class="cell-mono">{{ u.taskCount ?? '—' }}</td>
              <td class="cell-mono whitespace-nowrap">{{ fmt(u.createdAt) }}</td>
              <td>
                <div class="op-row">
                  <button class="op-btn" type="button" @click="openEdit(u)">
                    <PenLine :size="13" style="margin-right:4px" /> 编辑
                  </button>
                  <button class="op-btn" type="button"
                    :style="u.enabled ? '' : 'color:var(--chart-5)'"
                    @click="toggleEnabled(u)">
                    {{ u.enabled ? '禁用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!users.length">
              <td colspan="7" class="text-center py-8" style="color:var(--muted-foreground)">暂无用户</td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination :total="total" :page="page" :size="20" @update:page="page=$event; load()" />
    </article>

    <!-- Edit Modal -->
    <div v-if="modalOpen" class="modal-mask" @click.self="modalOpen = false">
      <div class="modal-card">
        <div class="modal-title">编辑用户 · {{ editForm.username }}</div>
        <div class="modal-body">
          <div class="field">
            <label>角色</label>
            <select v-model="editForm.role" class="field-select">
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
          <div class="field">
            <label>账号状态</label>
            <button type="button" class="switch" :class="{ on: editForm.enabled }" @click="editForm.enabled = !editForm.enabled">
              <span class="switch-thumb"></span>
              <span class="switch-label">{{ editForm.enabled ? '已启用' : '已禁用' }}</span>
            </button>
          </div>
        </div>
        <div class="modal-foot">
          <button class="action-btn ghost" type="button" @click="modalOpen = false">取消</button>
          <button class="action-btn primary" type="button" :disabled="saving" @click="saveEdit">保存</button>
        </div>
      </div>
    </div>
  </AppShell>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ListTodo, LayoutDashboard, Users, ShieldAlert, FileText, Search, PenLine, ClipboardList } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import Pagination from '@/components/Pagination.vue'
import { adminApi } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import dayjs from 'dayjs'

const navs = [
  { key: 'user-tasks', label: '用户任务', icon: ListTodo, to: '/tasks' },
  { key: 'extractions', label: '采购申请单', icon: ClipboardList, to: '/extractions' },
  { key: 'overview', label: '管理概览', icon: LayoutDashboard, to: '/admin' },
  { key: 'users', label: '用户管理', icon: Users, to: '/users' },
  { key: 'risk', label: '风险规则', icon: ShieldAlert, to: '/risk-rules' },
  { key: 'logs', label: 'LLM 日志', icon: FileText, to: '/llm-logs' }
]

const toast = useToastStore()
const filters = ['全部', '已启用', '已禁用']
const activeFilter = ref('全部')
const keyword = ref('')
const users = ref<any[]>([])
const total = ref(0)
const page = ref(1)

const enabledMap: Record<string, boolean | undefined> = {
  '全部': undefined, '已启用': true, '已禁用': false
}

const modalOpen = ref(false)
const saving = ref(false)
const editForm = reactive({ id: 0, username: '', role: 'USER' as 'USER' | 'ADMIN', enabled: true })

function fmt(s: string) { return s ? dayjs(s).format('YYYY-MM-DD') : '—' }

async function load() {
  try {
    const res: any = await adminApi.listUsers({ page: page.value, size: 20, keyword: keyword.value || undefined, enabled: enabledMap[activeFilter.value] })
    users.value = res.data.items || []
    total.value = res.data.total || 0
  } catch { /* toast 已由拦截器处理 */ }
}
function onSearch() { page.value = 1; load() }

async function toggleEnabled(u: any) {
  try {
    await adminApi.updateUser(u.id, { enabled: !u.enabled })
    toast.success(`已${u.enabled ? '禁用' : '启用'} ${u.username}`)
    load()
  } catch { /* toast 已由拦截器处理 */ }
}

function openEdit(u: any) {
  editForm.id = u.id
  editForm.username = u.username
  editForm.role = u.role
  editForm.enabled = !!u.enabled
  modalOpen.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await adminApi.updateUser(editForm.id, { role: editForm.role, enabled: editForm.enabled })
    toast.success('保存成功')
    modalOpen.value = false
    load()
  } catch { /* toast 已由拦截器处理 */ } finally { saving.value = false }
}

onMounted(load)
</script>
<style scoped>
/* 拷贝自 design/pages/用户管理.html 的 filter bar / 用户表格 / 角色与状态徽标 样式 */
.filter-bar { display: flex; flex-direction: column; gap: calc(var(--spacing) * 3); }
@media (min-width: 640px) { .filter-bar { flex-direction: row; align-items: center; } }
.filter-search {
  display: flex; align-items: center; gap: calc(var(--spacing) * 3);
  min-width: 280px; max-width: 400px; width: 100%;
  padding: calc(var(--spacing) * 3) calc(var(--spacing) * 4);
  border: 1px solid var(--input); border-radius: var(--radius); background: var(--popover);
}
.filter-chips { display: flex; flex-wrap: nowrap; overflow-x: auto; gap: calc(var(--spacing) * 2); }
.chip {
  display: inline-flex; align-items: center; justify-content: center;
  padding: calc(var(--spacing) * 2.5) calc(var(--spacing) * 4);
  border-radius: var(--radius); border: 1px solid var(--border);
  background: var(--card); color: var(--muted-foreground);
  font: 500 0.84rem/1 var(--font-sans); white-space: nowrap; cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}
.chip:hover { background: var(--accent); color: var(--accent-foreground); }
.chip.active { background: var(--primary); border-color: var(--primary); color: var(--primary-foreground); }

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

.badge {
  display: inline-flex; align-items: center; gap: .35rem;
  padding: .34rem .65rem; border-radius: 999px; font-size: .8rem; font-weight: 500; white-space: nowrap;
}
.role-admin { background: color-mix(in srgb, var(--primary) 18%, var(--background)); color: var(--primary); }
.role-user { background: var(--muted); color: var(--muted-foreground); }
.st-on { background: color-mix(in srgb, var(--chart-5) 18%, var(--background)); color: var(--chart-5); }
.st-off { background: color-mix(in srgb, var(--chart-2) 18%, var(--background)); color: var(--chart-2); }

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

/* Modal（沿用设计系统卡片样式） */
.modal-mask {
  position: fixed; inset: 0; z-index: 60;
  background: rgba(14, 17, 21, 0.42);
  display: grid; place-items: center; padding: calc(var(--spacing) * 4);
}
.modal-card {
  width: 100%; max-width: 420px;
  background: var(--card); color: var(--card-foreground);
  border: 1px solid var(--border); border-radius: var(--radius);
  box-shadow: var(--shadow-lg); padding: calc(var(--spacing) * 6);
  display: flex; flex-direction: column; gap: calc(var(--spacing) * 5);
}
.modal-title { font-size: 1.05rem; font-weight: 600; }
.modal-body { display: flex; flex-direction: column; gap: calc(var(--spacing) * 4); }
.field { display: flex; flex-direction: column; gap: calc(var(--spacing) * 2); }
.field label { font-size: .78rem; font-weight: 500; color: var(--muted-foreground); }
.field-select {
  height: 32px; padding: 0 calc(var(--spacing) * 3);
  border: 1px solid var(--input); border-radius: var(--radius);
  background: var(--popover); color: var(--foreground); font: inherit; font-size: .875rem;
  outline: none;
}
.switch {
  display: inline-flex; align-items: center; gap: calc(var(--spacing) * 3);
  border: none; background: transparent; cursor: pointer; padding: 0; font: inherit;
}
.switch {
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
.modal-foot { display: flex; justify-content: flex-end; gap: calc(var(--spacing) * 3); }
</style>
