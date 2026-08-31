<template>
  <div id="TagVisual">
    <el-button type="primary" @click="saveContainer"
    style="position: absolute;right:20px;top:0px;z-index: 3;">保存图表</el-button>
  <div class="chart-container" ref="chartContainer">
   
    <div ref="chartEl" style="width: 100%; height: 100%;">

     
    </div>
  
    <div class="custom-legend">
      <div v-for="(item, index) in visibleLegendItems" :key="index" class="legend-item">
        <span class="legend-color" :style="{ backgroundColor: item.color }"></span>
        <span class="legend-text">{{ item.name }} ({{ item.value }})</span>
      </div>
    </div>
  </div>
 
  </div>
 
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import * as echarts from 'echarts';
import 'echarts-wordcloud';
import { useResizeObserver } from '@vueuse/core'
import { selectAllTags } from '@/api/visual/visual'
// 在组件顶部引入 html2canvas
import html2canvas from 'html2canvas';
interface WordItem {
  name: string
  value: number
  color: string
}

// 响应式数据
const chartContainer = ref<HTMLElement>()
const chartEl = ref<HTMLElement>()
const chart = ref<echarts.ECharts | null>(null)
const wordsData = ref<WordItem[]>([])


// 修改 saveContainer 方法
const saveContainer = async () => {
  if (!chartContainer.value) return;
  
  try {
    // 配置项：提升截图质量（网页5）
    const options = {
      useCORS: true, // 解决跨域图片问题（网页7）
      scale: 2,      // 双倍分辨率
      logging: false,
      allowTaint: true
    };

    // 截图操作（网页6）
    const canvas = await html2canvas(chartContainer.value, options);
    
    // 生成下载链接（网页1）
    const link = document.createElement('a');
    link.href = canvas.toDataURL('image/png');
    link.download = `图片标签词云${new Date().getTime()}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (error) {
    ElMessage.error('保存失败，请重试');
  }
}
// 从后端获取标签数据
const fetchTags = async () => {
  try {
    const res = await selectAllTags()
    if (res.code === 200 && res.data) {
      // 转换后端数据格式
      return res.data.map((item: any) => ({
        name: item.tagName || item.name,      // 根据实际字段调整
        value: item.count || item.value,      // 根据实际字段调整
        color: generateColor()                // 生成随机颜色
      })).sort((a: WordItem, b: WordItem) => b.value - a.value)
    }
    return []
  } catch (error) {
    return []
  }
}

// 生成随机颜色
const generateColor = () => {
  return `hsl(${Math.random() * 360}, 70%, 60%)`
}

// 显示的图例项（取前15个）
const visibleLegendItems = computed(() => 
  wordsData.value.slice(0, 15)
)

// 初始化图表
const initChart = async () => {
  if (!chartEl.value) return
  
  // 销毁旧图表实例
  if (chart.value) {
    chart.value.dispose()
  }

  // 获取真实数据
  wordsData.value = await fetchTags()
 
  // 初始化新图表
  chart.value = echarts.init(chartEl.value)
  chart.value.setOption({
    title: {
      text: '图片标签词云分布',
      subtext: '数据来源：系统统计',
      left: 'center'
    },
    tooltip: {
      formatter: (params: any) => {
        return `${params.name}: ${params.value}次`
      }
    },
    series: [{
      type: 'wordCloud',
      shape: 'pentagon',
      sizeRange: [16, 80],
      rotationRange: [0, 45],
      rotationStep: 45,
      gridSize: 12,
      drawOutOfBound: false,
      layoutAnimation: true,
      textStyle: {
        fontFamily: 'sans-serif',
        fontWeight: 'bold',
        color: (params: any) => params.data.color
      },
      emphasis: {
        focus: 'self',
        textStyle: {
          textShadowBlur: 8,
          textShadowColor: 'rgba(0,0,0,0.3)'
        }
      },
      data: wordsData.value
    }]
  })
}

// 响应式调整
useResizeObserver(chartEl, () => {
  chart.value?.resize()
})

onMounted(async () => {
  await initChart()
})

onBeforeUnmount(() => {
  chart.value?.dispose()
})
</script>

<style scoped>
#TagVisual{
  position: relative;
  width: 100%;
  height: 100%;
}
.chart-container {
  margin-top: 20px ;
  position: relative;
  width: 100%;
  height: calc(100% - 30px);
}

.custom-legend {
  position: absolute;
  left: 20px;
  top: 20px;
  background: rgba(255, 255, 255, 0.9);
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  max-height: 60vh;
  overflow-y: auto;
  z-index: 1;
}

.legend-item {
  display: flex;
  align-items: center;
  margin: 6px 0;
  font-size: 12px;
}

.legend-color {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  margin-right: 8px;
}

.legend-text {
  white-space: nowrap;
}

/* 滚动条样式 */
.custom-legend::-webkit-scrollbar {
  width: 6px;
}

.custom-legend::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
}

.custom-legend::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}
</style>