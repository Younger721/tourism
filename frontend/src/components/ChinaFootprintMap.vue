<template>
  <div class="map-shell">
    <div v-if="!webglSupported" class="webgl-warning">
      当前浏览器或设备不支持 WebGL，无法显示 3D 地图。
    </div>
    <div ref="chartRef" class="map-box" />
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import 'echarts-gl'
import chinaGeoJson from '../data/china-geo.json'
import { provinces } from '../data/provinces'

const props = defineProps({
  counts: { type: Object, default: () => ({}) },
  selectedCode: { type: String, default: '' }
})
const emit = defineEmits(['select'])
const chartRef = ref(null)
const webglSupported = ref(true)
let chart

const provinceByCode = new Map(provinces.map((item) => [item.code, item]))
const provinceByName = new Map(provinces.map((item) => [item.name, item]))
const mapName = 'china-footprint-3d'
const provincePalette = [
  '#b8e6d7',
  '#b7d7f2',
  '#f4d7a1',
  '#c9daf8',
  '#d8c4f2',
  '#f7c6c7',
  '#bfe3c0',
  '#f5d6a6',
  '#b7e3ea',
  '#d5e5a3',
  '#c8d7ff',
  '#f3c7a7'
]
const activePalette = [
  '#22c55e',
  '#0ea5e9',
  '#f59e0b',
  '#3b82f6',
  '#8b5cf6',
  '#ef4444',
  '#16a34a',
  '#ea580c',
  '#0891b2',
  '#65a30d',
  '#2563eb',
  '#db2777'
]

function normalizeGeoJson() {
  return {
    ...chinaGeoJson,
    features: chinaGeoJson.features
      .filter((feature) => feature.properties?.adcode && feature.properties?.name)
      .map((feature) => {
        const code = String(feature.properties.adcode)
        const shortName = stripProvinceSuffix(feature.properties.name)
        const province = provinceByCode.get(code) || provinceByName.get(shortName)
        return {
          ...feature,
          properties: {
            ...feature.properties,
            adcode: code,
            name: province?.name || shortName
          }
        }
      })
      .filter((feature) => provinceByCode.has(String(feature.properties.adcode)))
  }
}

function stripProvinceSuffix(name) {
  return name
    .replace(/特别行政区$/, '')
    .replace(/壮族自治区|回族自治区|维吾尔自治区|自治区$/, '')
    .replace(/省|市$/, '')
}

function supportsWebGL() {
  try {
    const canvas = document.createElement('canvas')
    return Boolean(canvas.getContext('webgl') || canvas.getContext('experimental-webgl'))
  } catch {
    return false
  }
}

function paletteIndex(code) {
  const value = Number(String(code).slice(0, 2)) || 0
  return value % provincePalette.length
}

function colorByCount(code, count, maxCount, selected) {
  if (selected) return '#0f766e'
  const index = paletteIndex(code)
  if (count <= 0) return provincePalette[index]
  const ratio = Math.min(1, count / Math.max(maxCount, 1))
  if (ratio > 0.66) return activePalette[index]
  if (ratio > 0.33) return activePalette[(index + 2) % activePalette.length]
  return activePalette[(index + 4) % activePalette.length]
}

function buildData() {
  const maxCount = Math.max(1, ...Object.values(props.counts).map((value) => Number(value) || 0))
  return provinces.map((item) => {
    const count = Number(props.counts[item.code] || 0)
    const selected = item.code === props.selectedCode
    return {
      name: item.name,
      value: count,
      code: item.code,
      regionHeight: selected ? 5.8 : count > 0 ? 4.2 : 3.2,
      itemStyle: {
        color: colorByCount(item.code, count, maxCount, selected),
        borderColor: selected ? '#fef3c7' : '#ffffff',
        borderWidth: selected ? 2.6 : 1.35,
        opacity: 1
      },
      label: selected
        ? { show: true, color: '#ffffff', fontWeight: 700 }
        : undefined
    }
  })
}

function render() {
  if (!chartRef.value || !webglSupported.value) return
  if (!chart) {
    echarts.registerMap(mapName, normalizeGeoJson())
    chart = echarts.init(chartRef.value)
    chart.on('click', (params) => {
      const code = String(params.data?.code || '')
      const province = provinceByCode.get(code) || provinceByName.get(params.name)
      if (province) emit('select', province)
    })
  }

  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      borderWidth: 0,
      padding: [9, 12],
      textStyle: { color: '#0f172a' },
      formatter: ({ name, data }) => `${name}<br/>足迹：${data?.value || 0} 条`
    },
    series: [{
      type: 'map3D',
      map: mapName,
      name: '中国足迹地图',
      data: buildData(),
      boxWidth: 118,
      boxDepth: 92,
      regionHeight: 3.2,
      groundPlane: {
        show: false
      },
      label: {
        show: true,
        distance: 2,
        formatter: '{b}',
        textStyle: {
          color: '#0f172a',
          fontSize: 12,
          fontWeight: 600,
          backgroundColor: 'rgba(255, 255, 255, 0.76)',
          borderRadius: 3,
          padding: [2, 4]
        }
      },
      itemStyle: {
        color: '#9cc4e4',
        borderColor: '#ffffff',
        borderWidth: 1.35,
        opacity: 1
      },
      emphasis: {
        label: {
          show: true,
          textStyle: {
            color: '#ffffff',
            fontSize: 14,
            fontWeight: 700,
            backgroundColor: 'rgba(15, 118, 110, 0.85)',
            borderRadius: 4,
            padding: [3, 6]
          }
        },
        itemStyle: {
          color: '#facc15',
          borderColor: '#ffffff',
          borderWidth: 2.8
        }
      },
      shading: 'realistic',
      realisticMaterial: {
        roughness: 0.55,
        metalness: 0.08
      },
      light: {
        main: {
          intensity: 1.85,
          shadow: true,
          shadowQuality: 'medium',
          alpha: 42,
          beta: 28
        },
        ambient: {
          intensity: 0.78
        }
      },
      postEffect: {
        enable: true,
        bloom: { enable: false },
        SSAO: {
          enable: true,
          radius: 2,
          intensity: 1.1
        }
      },
      viewControl: {
        projection: 'perspective',
        alpha: 48,
        beta: 0,
        distance: 128,
        minDistance: 86,
        maxDistance: 190,
        rotateSensitivity: 1,
        zoomSensitivity: 1,
        panSensitivity: 0,
        autoRotate: false,
        center: [0, -6, 0]
      }
    }]
  }, true)
}

onMounted(() => {
  webglSupported.value = supportsWebGL()
  render()
  window.addEventListener('resize', resize)
})

watch(() => [props.counts, props.selectedCode], render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
})

function resize() {
  chart?.resize()
}
</script>

<style scoped>
.map-shell {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-box {
  width: 100%;
  height: 100%;
  min-height: 100%;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 22px;
  background: var(--map-bg);
  backdrop-filter: blur(12px) saturate(125%);
  box-shadow: 0 28px 84px rgba(0, 0, 0, 0.36);
}

.webgl-warning {
  position: absolute;
  z-index: 2;
  left: 18px;
  top: 18px;
  max-width: 360px;
  padding: 12px 14px;
  border: 1px solid rgba(251, 191, 36, 0.44);
  border-radius: 16px;
  color: #fde68a;
  background: rgba(120, 53, 15, 0.36);
  backdrop-filter: blur(10px);
  box-shadow: 0 16px 38px rgba(0, 0, 0, 0.24);
}

@media (max-width: 960px) {
  .map-box {
    min-height: 500px;
  }
}
</style>
