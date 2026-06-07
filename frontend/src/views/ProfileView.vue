<template>
  <div class="page">
    <el-card class="profile-card">
      <div class="profile-head">
        <el-avatar :size="72" :src="profile?.user?.avatarUrl">{{ profile?.user?.nickname?.[0] || '旅' }}</el-avatar>
        <div>
          <h2>{{ profile?.user?.nickname || profile?.user?.username }}</h2>
          <p class="muted">{{ profile?.user?.city || '未填写城市' }}</p>
          <p>{{ profile?.user?.bio || '这个人还没有写简介。' }}</p>
        </div>
        <div class="profile-actions">
          <el-button v-if="profile?.friendStatus === 'NONE'" type="primary" :icon="UserPlus" @click="addFriend">加好友</el-button>
          <el-button v-if="profile?.friendStatus === 'FRIEND'" type="primary" :icon="MessageCircle" @click="$router.push(`/messages?friendId=${userId}`)">发消息</el-button>
          <el-button v-if="profile?.friendStatus === 'SELF'" :icon="Edit3" @click="editing = true">编辑资料</el-button>
        </div>
      </div>
    </el-card>

    <div class="section-title">
      <h2>旅游记录</h2>
    </div>
    <div class="grid">
      <el-card v-for="post in posts" :key="post.id" class="travel-card">
        <img :src="post.imageUrl || fallback" :alt="post.title" />
        <div class="body">
          <h3>{{ post.title }}</h3>
          <p class="muted">{{ post.destination || post.provinceName }} · {{ post.travelDate }}</p>
          <p>{{ post.content }}</p>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="editing" title="编辑资料" width="520px">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="昵称"><el-input v-model="editForm.nickname" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="editForm.city" /></el-form-item>
        <el-form-item label="头像"><ImageUpload v-model="editForm.avatarUrl" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="editForm.bio" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editing = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Edit3, MessageCircle, UserPlus } from 'lucide-vue-next'
import http from '../api'
import ImageUpload from '../components/ImageUpload.vue'

const route = useRoute()
const userId = computed(() => route.params.id)
const profile = ref(null)
const posts = ref([])
const editing = ref(false)
const fallback = 'https://images.unsplash.com/photo-1512100356356-de1b84283e18?auto=format&fit=crop&w=900&q=80'
const editForm = reactive({ nickname: '', city: '', avatarUrl: '', bio: '' })

onMounted(load)
watch(userId, load)

async function load() {
  ;[profile.value, posts.value] = await Promise.all([
    http.get(`/users/${userId.value}/profile`),
    http.get(`/users/${userId.value}/posts`)
  ])
  Object.assign(editForm, profile.value.user)
}

async function addFriend() {
  await http.post('/friends/requests', { toUserId: Number(userId.value) })
  ElMessage.success('好友申请已发送')
}

async function saveProfile() {
  const user = await http.put('/users/me', editForm)
  localStorage.setItem('user', JSON.stringify(user))
  editing.value = false
  ElMessage.success('资料已更新')
  load()
}
</script>

<style scoped>
.profile-card {
  margin-top: 18px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 18px;
}

.profile-actions {
  margin-left: auto;
}
</style>
