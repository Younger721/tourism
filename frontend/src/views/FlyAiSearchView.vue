<template>
  <div class="page flyai-page">
    <div class="section-title">
      <div>
        <h2>旅行搜索</h2>
      </div>
    </div>

    <section class="search-panel">
      <el-radio-group v-model="form.type" class="type-switch">
        <el-radio-button v-for="item in typeOptions" :key="item.value" :label="item.value">
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>

      <template v-if="form.type === 'AI'">
        <el-input
          v-model="form.query"
          class="wide-input"
          placeholder="例如：上海周末亲子游，预算2000，想住地铁附近"
          clearable
          @keyup.enter="search"
        />
        <el-input v-model="form.city" placeholder="城市，可选" clearable @keyup.enter="search" />
      </template>

      <template v-else-if="form.type === 'FLIGHT' || form.type === 'TRAIN'">
        <el-input v-model="form.origin" placeholder="出发地" clearable @keyup.enter="search" />
        <el-input v-model="form.destination" placeholder="目的地" clearable @keyup.enter="search" />
        <el-date-picker v-model="form.depDate" type="date" value-format="YYYY-MM-DD" placeholder="出发日期" />
        <el-input v-model="form.maxPrice" placeholder="最高价，可选" clearable @keyup.enter="search" />
      </template>

      <template v-else-if="form.type === 'HOTEL'">
        <el-input v-model="form.city" placeholder="目的地" clearable @keyup.enter="search" />
        <el-date-picker v-model="form.checkInDate" type="date" value-format="YYYY-MM-DD" placeholder="入住日期" />
        <el-date-picker v-model="form.checkOutDate" type="date" value-format="YYYY-MM-DD" placeholder="退房日期" />
        <el-input v-model="form.keyword" placeholder="关键词，可选" clearable @keyup.enter="search" />
        <el-input v-model="form.maxPrice" placeholder="最高价，可选" clearable @keyup.enter="search" />
      </template>

      <template v-else>
        <el-input v-model="form.city" placeholder="城市" clearable @keyup.enter="search" />
        <el-input v-model="form.keyword" placeholder="景点关键词，可选" clearable @keyup.enter="search" />
      </template>

      <el-button type="primary" :icon="Search" :loading="loading" :disabled="!canSearch" @click="search">
        {{ loading ? '搜索中' : '搜索' }}
      </el-button>
    </section>
    <p v-if="loading" class="search-hint">FlyAI 正在检索实时旅行信息，复杂问题可能需要几十秒。</p>

    <el-alert
      v-if="error"
      class="state-alert"
      type="error"
      :closable="false"
      :title="error"
      show-icon
    />

    <el-empty v-if="!loading && !result && !error" description="输入旅行需求后开始搜索" />

    <section v-if="result" class="result-section">
      <div class="result-head">
        <div>
          <h3>{{ result.success ? '搜索结果' : '搜索失败' }}</h3>
          <p class="muted">{{ result.query }}</p>
        </div>
        <el-tag :type="result.success ? 'success' : 'danger'">
          {{ result.success ? 'FlyAI 已返回' : '需要处理' }}
        </el-tag>
      </div>

      <el-alert
        v-if="result.message"
        class="state-alert"
        :type="result.success ? 'info' : 'warning'"
        :closable="false"
        :title="result.message"
        show-icon
      />

      <article v-if="answerText" class="answer-card">
        <h4>搜索摘要</h4>
        <div>{{ answerText }}</div>
      </article>

      <div v-if="resultItems.length" class="result-grid">
        <article v-for="(item, index) in resultItems" :key="`${item.title}-${index}`" class="result-item">
          <div class="item-top">
            <div>
              <h4>{{ item.title }}</h4>
              <p v-if="item.subtitle">{{ item.subtitle }}</p>
            </div>
            <strong v-if="item.price" class="item-price">{{ item.price }}</strong>
          </div>
          <div v-if="item.time" class="item-time">{{ item.time }}</div>
          <div v-if="item.meta?.length" class="item-meta">
            <el-tag v-for="meta in item.meta" :key="meta" effect="plain">{{ meta }}</el-tag>
          </div>
          <el-button v-if="item.link" text type="primary" @click="openLink(item.link)">查看详情</el-button>
        </article>
      </div>

      <el-alert
        v-else-if="result.success && !answerText"
        class="state-alert"
        type="warning"
        :closable="false"
        title="未解析出结构化结果，可展开原始数据查看。"
        show-icon
      />

      <details class="raw-result">
        <summary>查看原始数据</summary>
        <pre>{{ formattedData }}</pre>
      </details>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from 'lucide-vue-next'
import http from '../api'

const form = reactive({
  type: 'FLIGHT',
  query: '',
  city: '',
  origin: '',
  destination: '',
  depDate: '',
  checkInDate: '',
  checkOutDate: '',
  keyword: '',
  maxPrice: ''
})
const loading = ref(false)
const result = ref(null)
const error = ref('')
const typeOptions = [
  { label: '机票', value: 'FLIGHT' },
  { label: '火车', value: 'TRAIN' },
  { label: '酒店', value: 'HOTEL' },
  { label: '景点', value: 'POI' }
]

const formattedData = computed(() => JSON.stringify(result.value?.data ?? {}, null, 2))
const answerText = computed(() => findAnswerText(result.value?.data))
const resultItems = computed(() => result.value?.items || [])
const canSearch = computed(() => {
  if (form.type === 'AI') return Boolean(form.query.trim())
  if (form.type === 'FLIGHT' || form.type === 'TRAIN') {
    return Boolean(form.origin.trim() && form.destination.trim() && form.depDate)
  }
  if (form.type === 'HOTEL') {
    return Boolean(form.city.trim() && form.checkInDate && form.checkOutDate)
  }
  return Boolean(form.city.trim())
})

async function search() {
  const warning = validateForm()
  if (warning) {
    ElMessage.warning(warning)
    return
  }
  loading.value = true
  error.value = ''
  result.value = null
  try {
    const response = await http.post('/flyai/search', buildPayload(), {
      timeout: 90000,
      silent: true
    })
    result.value = response
    if (!response.success && response.message) {
      error.value = response.message
    }
  } catch (err) {
    error.value = formatSearchError(err)
  } finally {
    loading.value = false
  }
}

function validateForm() {
  if (form.type === 'AI' && !form.query.trim()) return '请输入要搜索的旅行需求'
  if ((form.type === 'FLIGHT' || form.type === 'TRAIN') && !canSearch.value) return '请填写出发地、目的地和出发日期'
  if (form.type === 'HOTEL' && !canSearch.value) return '请填写目的地、入住日期和退房日期'
  if (form.type === 'POI' && !form.city.trim()) return '请填写景点城市'
  return ''
}

function buildPayload() {
  return {
    type: form.type,
    query: form.query.trim(),
    city: form.city.trim(),
    origin: form.origin.trim(),
    destination: form.destination.trim(),
    depDate: form.depDate,
    checkInDate: form.checkInDate,
    checkOutDate: form.checkOutDate,
    keyword: form.keyword.trim(),
    maxPrice: form.maxPrice.trim()
  }
}

function formatSearchError(err) {
  const message = err?.message || ''
  if (err?.code === 'ECONNABORTED' || message.includes('timeout')) {
    return 'FlyAI 搜索耗时较长，已超过 90 秒。请换一个更具体的关键词再试。'
  }
  return message || 'FlyAI 搜索失败'
}

function openLink(link) {
  window.open(link, '_blank', 'noopener,noreferrer')
}

function findAnswerText(value) {
  if (typeof value === 'string') {
    return value
  }
  if (!isPlainObject(value)) {
    return ''
  }
  const direct = value.data || value.result || value.content || value.answer || value.message
  if (typeof direct === 'string' && direct.trim().length > 20) {
    return direct
  }
  for (const item of Object.values(value)) {
    const nested = findAnswerText(item)
    if (nested) return nested
  }
  return ''
}

function isPlainObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value)
}
</script>

<style scoped>
.flyai-page {
  max-width: 1180px;
}

.section-title {
  align-items: flex-end;
}

.section-title p {
  margin: 8px 0 0;
}

.search-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr)) auto;
  gap: 12px;
  align-items: center;
  padding: 16px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  backdrop-filter: blur(12px) saturate(125%);
  box-shadow: var(--shadow-glass);
}

.type-switch {
  grid-column: 1 / -1;
}

.wide-input {
  grid-column: span 2;
}

.state-alert {
  margin-top: 16px;
}

.search-hint {
  margin: 10px 0 0;
  color: var(--text-muted);
}

.result-section {
  margin-top: 18px;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.result-head h3 {
  margin: 0 0 6px;
  color: var(--heading-color);
}

.result-head p {
  margin: 0;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.answer-card {
  padding: 18px;
  margin-bottom: 16px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  backdrop-filter: blur(10px) saturate(120%);
}

.answer-card h4 {
  margin: 0 0 12px;
  color: var(--heading-color);
}

.answer-card div {
  white-space: pre-wrap;
  line-height: 1.75;
  overflow-wrap: anywhere;
}

.result-item {
  padding: 16px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  backdrop-filter: blur(10px) saturate(120%);
}

.item-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.result-item h4 {
  margin: 0 0 10px;
  color: var(--heading-color);
}

.result-item p {
  margin: 0;
  line-height: 1.6;
  color: var(--text-muted);
}

.item-price {
  color: #ef4444;
  font-size: 18px;
  white-space: nowrap;
}

.item-time {
  margin-top: 12px;
  color: var(--heading-color);
  font-size: 18px;
  font-weight: 800;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.raw-result {
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  overflow: hidden;
}

.raw-result summary {
  cursor: pointer;
  padding: 12px 14px;
  color: var(--heading-color);
  font-weight: 700;
}

.raw-result pre {
  margin: 0;
  max-height: 480px;
  overflow: auto;
  padding: 14px;
  color: var(--text-main);
  background: rgba(2, 6, 23, 0.28);
}

@media (max-width: 760px) {
  .search-panel {
    grid-template-columns: 1fr;
  }

  .wide-input,
  .type-switch {
    grid-column: auto;
  }

  .result-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
