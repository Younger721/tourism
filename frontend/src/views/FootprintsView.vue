<template>
  <div class="footprints-page">
    <div class="footprints-toolbar">
      <div>
        <h2>灵感足迹</h2>
        <p class="muted">点击省份，记录照片和旅途文字。</p>
      </div>
      <el-button :icon="RefreshCcw" :loading="loadingCounts" @click="loadCounts">刷新</el-button>
    </div>

    <div class="map-panel">
      <ChinaFootprintMap :counts="counts" :selected-code="selectedProvince?.code || ''" @select="selectProvince" />
    </div>

    <aside v-if="selectedProvince" class="province-drawer">
      <header class="drawer-header">
        <div>
          <span class="drawer-kicker">当前省份</span>
          <h3>{{ selectedProvince.name }}</h3>
          <p>{{ footprints.length }} 条足迹</p>
        </div>
        <el-button circle text :icon="X" @click="closeDrawer" />
      </header>

      <div v-loading="loadingFootprints" class="drawer-body">
        <el-empty v-if="!loadingFootprints && footprints.length === 0" description="这里还没有足迹" />

        <div v-else class="footprint-list">
          <article v-for="item in footprints" :key="item.id" class="footprint-item">
            <img :src="item.imageUrl || fallback" :alt="item.title" />
            <div>
              <h4>{{ item.title }}</h4>
              <span>{{ item.travelDate || '未填写日期' }}</span>
              <p>{{ item.content }}</p>
            </div>
          </article>
        </div>

        <section class="create-box">
          <div class="create-title">
            <Plus :size="18" />
            <strong>继续上传足迹</strong>
          </div>
          <el-form :model="form" label-position="top">
            <el-form-item label="标题">
              <el-input v-model="form.title" placeholder="例如：西湖傍晚散步" />
            </el-form-item>
            <el-form-item label="照片">
              <ImageUpload v-model="form.imageUrl" />
            </el-form-item>
            <el-form-item label="旅行日期">
              <el-date-picker v-model="form.travelDate" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="文字记录">
              <el-input v-model="form.content" type="textarea" :rows="4" placeholder="写一点旅途感受、路线或小提示" />
            </el-form-item>
            <el-button type="primary" :icon="Upload" :loading="saving" :disabled="!canSave" @click="saveFootprint">
              保存足迹
            </el-button>
          </el-form>
        </section>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, RefreshCcw, Upload, X } from 'lucide-vue-next'
import http from '../api'
import ChinaFootprintMap from '../components/ChinaFootprintMap.vue'
import ImageUpload from '../components/ImageUpload.vue'

const counts = ref({})
const footprints = ref([])
const selectedProvince = ref(null)
const loadingCounts = ref(false)
const loadingFootprints = ref(false)
const saving = ref(false)
const fallback = 'https://images.unsplash.com/photo-1512100356356-de1b84283e18?auto=format&fit=crop&w=900&q=80'
const form = reactive({ title: '', imageUrl: '', travelDate: '', content: '' })

const canSave = computed(() => Boolean(selectedProvince.value && form.title.trim() && form.content.trim()))

onMounted(loadCounts)

async function loadCounts() {
  loadingCounts.value = true
  try {
    counts.value = await http.get('/footprints/provinces')
  } finally {
    loadingCounts.value = false
  }
}

async function selectProvince(province) {
  selectedProvince.value = province
  resetForm()
  await loadFootprints()
}

async function loadFootprints() {
  if (!selectedProvince.value) return
  loadingFootprints.value = true
  try {
    footprints.value = await http.get('/footprints', {
      params: { provinceCode: selectedProvince.value.code }
    })
  } finally {
    loadingFootprints.value = false
  }
}

async function saveFootprint() {
  if (!canSave.value || !selectedProvince.value) return
  saving.value = true
  try {
    await http.post('/footprints', {
      ...form,
      provinceCode: selectedProvince.value.code,
      provinceName: selectedProvince.value.name
    })
    ElMessage.success('足迹已保存')
    resetForm()
    await Promise.all([loadFootprints(), loadCounts()])
  } finally {
    saving.value = false
  }
}

function closeDrawer() {
  selectedProvince.value = null
  footprints.value = []
  resetForm()
}

function resetForm() {
  Object.assign(form, { title: '', imageUrl: '', travelDate: '', content: '' })
}
</script>

<style scoped>
.footprints-page {
  position: relative;
  width: calc(100% + 40px);
  min-height: calc(100vh - 60px);
  height: calc(100vh - 60px);
  margin: -20px;
  overflow: hidden;
  background: var(--footprints-bg);
}

.footprints-toolbar {
  position: absolute;
  z-index: 5;
  top: 24px;
  left: 32px;
  right: 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  pointer-events: none;
}

.footprints-toolbar > * {
  pointer-events: auto;
}

.footprints-toolbar h2 {
  margin: 0 0 10px;
  font-size: 28px;
  color: var(--heading-color);
}

.footprints-toolbar p {
  margin: 0;
}

.map-panel {
  position: absolute;
  inset: 0;
  min-width: 0;
}

.province-drawer {
  position: absolute;
  z-index: 6;
  top: 96px;
  right: 32px;
  width: 380px;
  max-height: calc(100% - 128px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 22px;
  background: var(--glass-bg-strong);
  backdrop-filter: blur(24px) saturate(155%);
  box-shadow: 0 28px 82px rgba(0, 0, 0, 0.38);
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
  background: var(--glass-bg-soft);
}

.drawer-kicker {
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
}

.drawer-header h3 {
  margin: 4px 0;
  color: var(--heading-color);
  font-size: 22px;
}

.drawer-header p {
  margin: 0;
  color: var(--text-muted);
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
}

.footprint-list {
  display: grid;
  gap: 12px;
}

.footprint-item {
  display: grid;
  grid-template-columns: 106px 1fr;
  gap: 12px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 16px;
  background: var(--glass-bg-soft);
  backdrop-filter: blur(16px);
}

.footprint-item img {
  width: 106px;
  height: 86px;
  object-fit: cover;
  border-radius: 14px;
  background: var(--image-bg);
}

.footprint-item h4 {
  margin: 0 0 4px;
  color: var(--heading-color);
  font-size: 15px;
}

.footprint-item span {
  color: var(--text-muted);
  font-size: 12px;
}

.footprint-item p {
  margin: 6px 0 0;
  color: var(--text-main);
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.create-box {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}

.create-title {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 12px;
  color: var(--accent);
}

:deep(.el-date-editor.el-input) {
  width: 100%;
}

@media (max-width: 1080px) {
  .footprints-page {
    min-height: calc(100vh - 60px);
    height: calc(100vh - 60px);
  }

  .province-drawer {
    left: 14px;
    right: 14px;
    bottom: 14px;
    top: auto;
    width: auto;
    max-height: 48%;
  }
}

@media (max-width: 640px) {
  .footprints-toolbar {
    top: 14px;
    left: 14px;
    right: 14px;
    align-items: flex-start;
  }

  .footprints-toolbar h2 {
    font-size: 22px;
  }

  .footprints-toolbar p {
    max-width: 220px;
    font-size: 13px;
  }
}
</style>
