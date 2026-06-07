<template>
  <div class="page">
    <div class="section-title">
      <h2>AI 智能行程推荐</h2>
      <el-segmented v-model="form.mode" :options="modeOptions" />
    </div>

    <el-card class="form-panel">
      <el-form :model="form" label-position="top">
        <el-form-item v-if="form.mode === 'DESTINATION_PLAN'" label="目的地">
          <el-input v-model="form.destination" placeholder="例如：杭州" />
        </el-form-item>
        <el-form-item label="出行人数">
          <el-input-number v-model="form.peopleCount" :min="1" />
        </el-form-item>
        <el-form-item label="出游天数">
          <el-input-number v-model="form.days" :min="1" :max="15" />
        </el-form-item>
        <el-form-item label="预算">
          <el-input-number v-model="form.budget" :min="0" :step="500" />
        </el-form-item>
        <el-form-item label="出发日期">
          <el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="旅游要求">
          <el-input v-model="form.requirements" type="textarea" :rows="4" placeholder="例如：想看海、节奏轻松、适合拍照、预算不要太高" />
        </el-form-item>
        <el-button type="primary" :icon="Sparkles" :loading="loading" @click="generate">生成计划</el-button>
      </el-form>
    </el-card>

    <el-card v-if="result" class="result-panel">
      <div class="section-title">
        <h2>{{ result.title }}</h2>
        <el-button type="primary" :icon="PenLine" @click="createDraft">转成旅游记录草稿</el-button>
      </div>
      <p><strong>推荐目的地：</strong>{{ result.recommendedDestination }}</p>
      <p><strong>推荐理由：</strong>{{ result.reason }}</p>
      <p>{{ result.summary }}</p>
      <el-timeline>
        <el-timeline-item v-for="day in result.dailyPlans" :key="day.day" :timestamp="`第 ${day.day} 天：${day.theme}`">
          <p v-for="activity in day.activities" :key="activity">{{ activity }}</p>
          <p class="muted">美食：{{ day.food }}；住宿：{{ day.accommodation }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-alert :title="result.estimatedCost" type="success" :closable="false" />
      <p><strong>交通建议：</strong>{{ result.transportAdvice }}</p>
      <p><strong>注意事项：</strong>{{ result.tips }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { PenLine, Sparkles } from 'lucide-vue-next'
import http from '../api'

const router = useRouter()
const modeOptions = [
  { label: '指定目的地', value: 'DESTINATION_PLAN' },
  { label: 'AI推荐目的地', value: 'AI_DESTINATION_PLAN' }
]
const form = reactive({
  mode: 'AI_DESTINATION_PLAN',
  destination: '',
  peopleCount: 2,
  days: 3,
  budget: 3000,
  startDate: '',
  requirements: ''
})
const loading = ref(false)
const result = ref(null)

async function generate() {
  if (form.mode === 'DESTINATION_PLAN' && !form.destination) {
    ElMessage.warning('请填写目的地')
    return
  }
  loading.value = true
  try {
    result.value = await http.post('/ai/trip-plan', form)
    ElMessage.success('行程已生成并保存')
  } finally {
    loading.value = false
  }
}

function createDraft() {
  const content = [
    result.value.summary,
    ...(result.value.dailyPlans || []).map((day) => `第 ${day.day} 天：${day.theme}\n${(day.activities || []).join('\n')}`)
  ].join('\n\n')
  localStorage.setItem('postDraft', JSON.stringify({
    title: result.value.title,
    content,
    destination: result.value.recommendedDestination,
    travelDate: form.startDate,
    visibility: 'PUBLIC'
  }))
  router.push('/community')
}
</script>
