<template>
  <section>
    <div class="section-title">
      <h3>{{ title }}</h3>
      <el-button type="primary" :icon="Plus" @click="create">新增</el-button>
    </div>

    <el-table :data="rows" border>
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="province" label="省份" width="110" />
      <el-table-column prop="city" label="城市" width="120" />
      <el-table-column prop="level" label="等级" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button class="action-btn is-edit" :icon="Pencil" @click="edit(row)">编辑</el-button>
            <el-button class="action-btn is-delete" :icon="Trash2" @click="remove(row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑景点灵感' : '新增景点灵感'" width="680px">
      <el-form :model="form" label-position="top">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="等级"><el-input v-model="form.level" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="省份"><el-input v-model="form.province" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="图片">
          <ImageUpload v-model="form.imageUrl" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :icon="Save" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Pencil, Plus, Save, Trash2 } from 'lucide-vue-next'
import http from '../api'
import ImageUpload from './ImageUpload.vue'

const props = defineProps({
  title: { type: String, required: true },
  endpoint: { type: String, required: true }
})

const emit = defineEmits(['changed'])
const rows = ref([])
const visible = ref(false)
const form = reactive(defaultForm())

onMounted(load)

async function load() {
  rows.value = await http.get(props.endpoint)
}

function defaultForm() {
  return {
    id: null,
    name: '',
    province: '',
    city: '',
    address: '',
    level: '',
    price: 0,
    imageUrl: '',
    description: '',
    status: 1
  }
}

function create() {
  Object.assign(form, defaultForm())
  visible.value = true
}

function edit(row) {
  Object.assign(form, defaultForm(), row)
  visible.value = true
}

async function save() {
  await http.post(props.endpoint, form)
  visible.value = false
  ElMessage.success('已保存')
  await load()
  emit('changed')
}

async function remove(id) {
  await ElMessageBox.confirm('确认删除这个景点灵感吗？相关收藏和评论也会被清理。', '提示')
  await http.delete(`${props.endpoint}/${id}`)
  ElMessage.success('已删除')
  await load()
  emit('changed')
}
</script>

<style scoped>
.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 176px;
}

.table-actions .el-button + .el-button {
  margin-left: 0;
}

.action-btn {
  width: 78px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  font-weight: 700;
  border: 1px solid transparent;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.06);
}

.action-btn.is-edit {
  color: #0f766e;
  border-color: rgba(20, 184, 166, 0.24);
  background: rgba(20, 184, 166, 0.1);
}

.action-btn.is-edit:hover {
  color: #ffffff;
  border-color: transparent;
  background: linear-gradient(135deg, #0f766e, #2563eb);
}

.action-btn.is-delete {
  color: #e11d48;
  border-color: rgba(225, 29, 72, 0.22);
  background: rgba(225, 29, 72, 0.08);
}

.action-btn.is-delete:hover {
  color: #ffffff;
  border-color: transparent;
  background: linear-gradient(135deg, #e11d48, #f97316);
}
</style>
