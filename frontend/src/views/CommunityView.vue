<template>
  <div class="page">
    <div class="section-title">
      <div>
        <h2>旅游社区</h2>
        <p class="muted">发布旅途照片和文字，发现其他人的旅行灵感。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">发布记录</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索目的地、省份或内容" clearable />
      <el-button :icon="Search" @click="load">搜索</el-button>
    </div>

    <div class="grid post-grid">
      <el-card v-for="post in posts" :key="post.id" class="travel-card" shadow="hover">
        <img :src="post.imageUrl || fallback" :alt="post.title" loading="lazy" decoding="async" />
        <div class="body">
          <h3>{{ post.title }}</h3>
          <p class="muted">{{ post.destination || post.provinceName }} · {{ post.travelDate || '未填写日期' }}</p>
          <p>{{ post.content }}</p>
          <div class="toolbar">
            <el-button text type="primary" @click="$router.push(`/users/${post.userId}`)">作者主页</el-button>
            <el-button :icon="Heart" @click="favorite(post)">收藏</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="visible" title="发布旅游记录" width="620px">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="目的地"><el-input v-model="form.destination" /></el-form-item>
        <el-form-item label="省份名称"><el-input v-model="form.provinceName" placeholder="例如：浙江" /></el-form-item>
        <el-form-item label="省份编码"><el-input v-model="form.provinceCode" placeholder="例如：330000" /></el-form-item>
        <el-form-item label="旅行日期"><el-date-picker v-model="form.travelDate" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="图片"><ImageUpload v-model="form.imageUrl" /></el-form-item>
        <el-form-item label="正文"><el-input v-model="form.content" type="textarea" :rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :icon="Save" @click="save">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Heart, Plus, Save, Search } from 'lucide-vue-next'
import http from '../api'
import ImageUpload from '../components/ImageUpload.vue'

const posts = ref([])
const keyword = ref('')
const visible = ref(false)
const fallback = 'https://images.unsplash.com/photo-1512100356356-de1b84283e18?auto=format&fit=crop&w=900&q=80'
const form = reactive({
  title: '',
  content: '',
  destination: '',
  provinceCode: '',
  provinceName: '',
  imageUrl: '',
  travelDate: '',
  visibility: 'PUBLIC'
})

onMounted(() => {
  const draft = JSON.parse(localStorage.getItem('postDraft') || 'null')
  if (draft) {
    Object.assign(form, draft)
    localStorage.removeItem('postDraft')
    visible.value = true
  }
  load()
})

async function load() {
  posts.value = await http.get('/posts', { params: keyword.value ? { keyword: keyword.value } : {} })
}

function openCreate() {
  Object.assign(form, { title: '', content: '', destination: '', provinceCode: '', provinceName: '', imageUrl: '', travelDate: '', visibility: 'PUBLIC' })
  visible.value = true
}

async function save() {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和正文')
    return
  }
  await http.post('/posts', form)
  visible.value = false
  ElMessage.success('旅游记录已发布')
  load()
}

async function favorite(post) {
  await http.post('/favorites', { targetType: 'POST', targetId: post.id, targetName: post.title })
  ElMessage.success('已收藏')
}
</script>

<style scoped>
.post-grid {
  margin-top: 16px;
}
</style>
