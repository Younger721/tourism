<template>
  <div class="page">
    <div class="section-title">
      <h2>我的订单</h2>
      <el-button :icon="RefreshCcw" @click="load">刷新</el-button>
    </div>
    <el-table :data="orders" border>
      <el-table-column prop="itemName" label="项目" />
      <el-table-column prop="itemType" label="类型" width="120" />
      <el-table-column prop="quantity" label="数量" width="90" />
      <el-table-column prop="totalAmount" label="金额" width="120" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="createTime" label="下单时间" width="180" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RefreshCcw } from 'lucide-vue-next'
import http from '../api'

const orders = ref([])
onMounted(load)

async function load() {
  orders.value = await http.get('/orders/my')
}
</script>
