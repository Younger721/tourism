<template>
  <div class="page">
    <section class="hero">
      <div>
        <h1>发现下一段旅程</h1>
        <p>用 AI 找到目的地，在社区里记录旅途，也看看别人走过的风景。</p>
      </div>
      <div class="toolbar">
        <el-button :icon="PenLine" @click="$router.push('/community')">发布记录</el-button>
      </div>
    </section>

    <div class="section-title">
      <h2>景点灵感</h2>
      <div class="toolbar search-toolbar">
        <el-input v-model="keyword" class="search-input" placeholder="搜索城市或景点" clearable />
        <el-button :icon="Search" @click="load">搜索</el-button>
      </div>
    </div>
    <div class="grid">
      <ResourceCard
        v-for="item in spots"
        :key="item.id"
        :favorited="item.favorited"
        :item="item"
        type="SCENIC"
        @favorite="toggleFavorite('SCENIC', $event)"
      />
    </div>

  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { PenLine, Search } from 'lucide-vue-next'
import http from '../api'
import ResourceCard from '../components/ResourceCard.vue'

const keyword = ref('')
const spots = ref([])

onMounted(load)

async function load() {
  const params = keyword.value ? { keyword: keyword.value } : {}
  const [spotList, favorites] = await Promise.all([
    http.get('/scenic-spots', { params }),
    loadFavorites()
  ])
  spots.value = withFavoriteState(spotList, favorites, 'SCENIC')
}

async function loadFavorites() {
  if (!localStorage.getItem('token')) {
    return []
  }
  return http.get('/favorites/my')
}

function withFavoriteState(items, favorites, type) {
  const favoriteIds = new Set(
    favorites
      .filter((favorite) => favorite.targetType === type)
      .map((favorite) => String(favorite.targetId))
  )
  return items.map((item) => ({
    ...item,
    favorited: favoriteIds.has(String(item.id)),
    favoritePending: false
  }))
}

async function toggleFavorite(type, item) {
  if (item.favoritePending) {
    return
  }
  item.favoritePending = true
  try {
    if (item.favorited) {
      await http.delete('/favorites', { params: { targetType: type, targetId: item.id } })
      item.favorited = false
      ElMessage.success('已取消收藏')
    } else {
      await http.post('/favorites', { targetType: type, targetId: item.id, targetName: item.name })
      item.favorited = true
      ElMessage.success('已收藏')
    }
  } finally {
    item.favoritePending = false
  }
}
</script>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 240px;
  padding: 34px;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 26px;
  background: linear-gradient(rgba(2, 6, 23, 0.36), rgba(15, 23, 42, 0.62)),
    url('https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1400&q=80') center/cover;
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.36);
  backdrop-filter: blur(10px) saturate(120%);
}

.hero h1 {
  font-size: 42px;
  margin: 0 0 12px;
}

.hero p {
  max-width: 600px;
  line-height: 1.8;
}

.search-toolbar {
  flex-wrap: nowrap;
  justify-content: flex-end;
  min-width: 360px;
}

.search-input {
  width: 260px;
}

@media (max-width: 760px) {
  .search-toolbar {
    width: 100%;
    min-width: 0;
    justify-content: flex-start;
  }

  .search-input {
    flex: 1;
    min-width: 0;
  }
}
</style>
