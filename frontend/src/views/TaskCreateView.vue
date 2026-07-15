<template>
  <AppShell :navs="navs" search-placeholder="搜索任务...">
    <div class="hero">
      <div>
        <h2>新建对比任务</h2>
        <p>上传采购与销售合同，系统自动抽取要素并对比</p>
      </div>
    </div>
    <article class="content-card" style="max-width:720px;padding:calc(var(--spacing)*6)">
      <form @submit.prevent="onSubmit" class="flex flex-col gap-5">
        <FieldInput v-model="title" label="任务名称" :icon="Tag" placeholder="如：7月螺丝采购" />
        <FileDrop label="采购合同（BUY）" v-model="buy" />
        <FileDrop label="销售合同（SELL）" v-model="sell" />
        <div class="flex gap-3">
          <button type="button" class="action-btn ghost" @click="$router.back()">取消</button>
          <button type="submit" :disabled="loading" class="action-btn primary">提交对比</button>
        </div>
      </form>
    </article>
  </AppShell>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Tag, ListTodo } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import FieldInput from '@/components/FieldInput.vue'
import FileDrop from '@/components/FileDrop.vue'
import { taskApi } from '@/api/task'
import { useToastStore } from '@/stores/toast'

const navs = [{ key: 'tasks', label: '任务列表', icon: ListTodo, to: '/tasks' }]
const title = ref('')
const buy = ref<File | null>(null)
const sell = ref<File | null>(null)
const loading = ref(false)
const router = useRouter()
const toast = useToastStore()

async function onSubmit() {
  if (!title.value) return toast.warning('请填任务名')
  if (!buy.value || !sell.value) return toast.warning('请上传两份合同')
  if (buy.value.size > 20 * 1024 * 1024 || sell.value.size > 20 * 1024 * 1024) return toast.warning('文件 ≤ 20MB')
  loading.value = true
  try {
    const fd = new FormData()
    fd.append('title', title.value)
    fd.append('buyFile', buy.value)
    fd.append('sellFile', sell.value)
    const res: any = await taskApi.create(fd)
    toast.success('任务已创建，正在后台处理')
    router.push(`/tasks/${res.data.id}`)
  } finally { loading.value = false }
}
</script>
