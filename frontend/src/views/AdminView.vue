<template>
  <div class="page admin-page">
    <div class="section-title">
      <h2>后台管理</h2>
      <el-button :icon="RefreshCcw" @click="loadAll">刷新</el-button>
    </div>

    <div class="stats">
      <el-card v-for="(value, key) in stats" :key="key" shadow="never">
        <p class="muted">{{ labels[key] || key }}</p>
        <h2>{{ value }}</h2>
      </el-card>
    </div>

    <el-tabs>
      <el-tab-pane label="景点灵感">
        <AdminCrud title="景点灵感" endpoint="/admin/scenic-spots" @changed="loadAll" />
      </el-tab-pane>

      <el-tab-pane label="旅游记录">
        <el-table :data="posts" border>
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="destination" label="目的地" width="120" />
          <el-table-column prop="provinceName" label="省份" width="110" />
          <el-table-column label="用户" width="130">
            <template #default="{ row }">
              {{ userName(row.userId) }}
            </template>
          </el-table-column>
          <el-table-column prop="travelDate" label="旅行日期" width="120" />
          <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right" align="center">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button class="action-btn is-delete" :icon="Trash2" @click="deletePost(row.id)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="用户">
        <el-table :data="users" border>
          <el-table-column prop="username" label="用户名" min-width="130" />
          <el-table-column prop="nickname" label="昵称" min-width="130" />
          <el-table-column prop="city" label="城市" width="110" />
          <el-table-column prop="role" label="角色" width="100" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="270" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 1"
                type="warning"
                :icon="Ban"
                :disabled="isCurrentUser(row)"
                @click="disableUser(row)"
              >
                禁用
              </el-button>
              <el-button
                v-else
                type="success"
                :icon="UserCheck"
                :disabled="isCurrentUser(row)"
                @click="updateUserStatus(row, 1)"
              >
                启用
              </el-button>
              <el-button :icon="LogOut" :disabled="isCurrentUser(row)" @click="kickoutUser(row)">
                踢下线
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="足迹">
        <el-table :data="footprints" border>
          <el-table-column prop="provinceName" label="省份" width="110" />
          <el-table-column prop="title" label="标题" min-width="160" />
          <el-table-column label="用户" width="130">
            <template #default="{ row }">
              {{ userName(row.userId) }}
            </template>
          </el-table-column>
          <el-table-column prop="travelDate" label="旅行日期" width="120" />
          <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button type="danger" :icon="Trash2" @click="deleteFootprint(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Ban, LogOut, RefreshCcw, Trash2, UserCheck } from 'lucide-vue-next'
import http from '../api'
import AdminCrud from '../components/AdminCrud.vue'

const stats = ref({})
const posts = ref([])
const users = ref([])
const footprints = ref([])
const currentUser = JSON.parse(localStorage.getItem('user') || 'null')
const statKeys = ['users', 'spots', 'posts', 'footprints']
const labels = {
  users: '用户',
  spots: '景点',
  posts: '旅游记录',
  footprints: '足迹'
}

onMounted(loadAll)

async function loadAll() {
  const [statsData, postsData, usersData, footprintsData] = await Promise.all([
    http.get('/admin/stats'),
    http.get('/admin/posts'),
    http.get('/admin/users'),
    http.get('/admin/footprints')
  ])
  stats.value = pickStats(statsData)
  posts.value = postsData
  users.value = usersData
  footprints.value = footprintsData
}

function pickStats(data) {
  return statKeys.reduce((result, key) => {
    result[key] = data?.[key] ?? 0
    return result
  }, {})
}

function isCurrentUser(row) {
  return currentUser?.id === row.id
}

function userName(userId) {
  const user = users.value.find((item) => item.id === userId)
  return user?.nickname || user?.username || `用户${userId}`
}

async function deletePost(id) {
  await ElMessageBox.confirm('确认删除这条旅游记录吗？相关收藏和评论也会被清理。', '提示')
  await http.delete(`/admin/posts/${id}`)
  ElMessage.success('已删除')
  loadAll()
}

async function disableUser(row) {
  await ElMessageBox.confirm(`确认禁用用户 ${row.username} 吗？该用户会被强制下线。`, '提示')
  await http.delete(`/admin/users/${row.id}`)
  ElMessage.success('已禁用并强制下线')
  loadAll()
}

async function updateUserStatus(row, status) {
  const action = status === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确认${action}用户 ${row.username} 吗？`, '提示')
  await http.patch(`/admin/users/${row.id}/status`, { status })
  ElMessage.success(`已${action}`)
  loadAll()
}

async function kickoutUser(row) {
  await ElMessageBox.confirm(`确认强制用户 ${row.username} 下线吗？`, '提示')
  await http.post(`/admin/users/${row.id}/kickout`)
  ElMessage.success('已强制下线')
}

async function deleteFootprint(id) {
  await ElMessageBox.confirm('确认删除这条足迹吗？', '提示')
  await http.delete(`/admin/footprints/${id}`)
  ElMessage.success('已删除')
  loadAll()
}
</script>

<style scoped>
.admin-page {
  max-width: 1280px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.stats :deep(.el-card__body) {
  padding: 14px 16px;
}

.stats h2 {
  margin: 0;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
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
