<template>
  <div class="image-upload">
    <el-upload
      :show-file-list="false"
      :http-request="upload"
      :before-upload="beforeUpload"
      accept="image/jpeg,image/png,image/webp,image/gif"
    >
      <el-button type="primary" :icon="Upload" :loading="loading">上传图片</el-button>
    </el-upload>
    <el-input v-model="model" placeholder="也可以粘贴图片URL" clearable />
    <img v-if="model" class="preview" :src="model" alt="图片预览" decoding="async" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from 'lucide-vue-next'
import http from '../api'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])
const loading = ref(false)

const model = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

function beforeUpload(file) {
  const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!allowed.includes(file.type)) {
    ElMessage.warning('仅支持 jpg、png、webp、gif 图片')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 10MB')
    return false
  }
  return true
}

async function upload({ file }) {
  loading.value = true
  try {
    const data = new FormData()
    data.append('file', file)
    const result = await http.post('/files/upload', data, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    model.value = result.url
    ElMessage.success('图片上传成功')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.image-upload {
  display: grid;
  gap: 10px;
}

.preview {
  width: 180px;
  height: 110px;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 42px rgba(0, 0, 0, 0.22);
}
</style>
