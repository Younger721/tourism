<template>
  <div class="page">
    <div class="section-title">
      <div>
        <h2>{{ provinceName }}足迹</h2>
        <p class="muted">照片地址可以先填写网络图片链接，后续可扩展真实上传。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增足迹</el-button>
    </div>

    <div class="grid">
      <el-card v-for="item in list" :key="item.id" class="travel-card">
        <img :src="item.imageUrl || fallback" :alt="item.title" />
        <div class="body">
          <h3>{{ item.title }}</h3>
          <p class="muted">{{ item.travelDate }}</p>
          <p>{{ item.content }}</p>
          <div class="toolbar">
            <el-button :icon="Pencil" @click="edit(item)">编辑</el-button>
            <el-button type="danger" :icon="Trash2" @click="remove(item.id)">删除</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑足迹' : '新增足迹'" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="照片">
          <ImageUpload v-model="form.imageUrl" />
        </el-form-item>
        <el-form-item label="旅行日期"><el-date-picker v-model="form.travelDate" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="文字记录"><el-input v-model="form.content" type="textarea" :rows="5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :icon="Save" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Pencil, Plus, Save, Trash2 } from 'lucide-vue-next'
import http from '../api'
import ImageUpload from '../components/ImageUpload.vue'
import { provinceName as findProvinceName } from '../data/provinces'

const route = useRoute()
const code = computed(() => route.params.provinceCode)
const provinceName = computed(() => route.query.name || findProvinceName(code.value))
const list = ref([])
const visible = ref(false)
const fallback = 'https://images.unsplash.com/photo-1512100356356-de1b84283e18?auto=format&fit=crop&w=900&q=80'
const form = reactive({ id: null, title: '', imageUrl: '', travelDate: '', content: '' })

onMounted(load)

async function load() {
  list.value = await http.get('/footprints', { params: { provinceCode: code.value } })
}

function openCreate() {
  Object.assign(form, { id: null, title: '', imageUrl: '', travelDate: '', content: '' })
  visible.value = true
}

function edit(item) {
  Object.assign(form, item)
  visible.value = true
}

async function save() {
  const payload = { ...form, provinceCode: code.value, provinceName: provinceName.value }
  if (form.id) await http.put(`/footprints/${form.id}`, payload)
  else await http.post('/footprints', payload)
  visible.value = false
  ElMessage.success('已保存')
  load()
}

async function remove(id) {
  await ElMessageBox.confirm('确认删除这条足迹吗？', '提示')
  await http.delete(`/footprints/${id}`)
  ElMessage.success('已删除')
  load()
}
</script>
