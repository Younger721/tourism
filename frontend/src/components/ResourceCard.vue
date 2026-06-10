<template>
  <el-card class="travel-card" shadow="hover">
    <img :src="imageSrc" :alt="item.name" loading="lazy" decoding="async" @error="useFallback" />
    <div class="body">
      <h3>{{ item.name }}</h3>
      <p class="muted">{{ subtitle }}</p>
      <p>{{ item.description || '暂无简介' }}</p>
      <div class="toolbar">
        <el-tag type="success">灵感目的地</el-tag>
        <el-button
          :class="['favorite-button', { 'is-favorited': favorited }]"
          :disabled="item.favoritePending"
          :icon="Heart"
          :type="favorited ? 'danger' : ''"
          @click="$emit('favorite', item)"
        >
          {{ favorited ? '已收藏' : '收藏' }}
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Heart } from 'lucide-vue-next'

const props = defineProps({
  item: { type: Object, required: true },
  type: { type: String, required: true },
  favorited: { type: Boolean, default: false }
})
defineEmits(['favorite'])

const fallback = '/images/scenic-fallback.svg'
const failed = ref(false)
const imageSrc = computed(() => failed.value ? fallback : (props.item.imageUrl || fallback))
const subtitle = computed(() => props.item.city || props.item.destination || props.item.province || props.type)

watch(() => props.item.imageUrl, () => {
  failed.value = false
})

function useFallback() {
  failed.value = true
}
</script>

<style scoped>
.favorite-button.is-favorited :deep(svg) {
  fill: currentColor;
  stroke: currentColor;
}
</style>
