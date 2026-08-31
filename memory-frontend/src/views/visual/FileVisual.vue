<template>
  <div id="fileViusal">
    <el-button type="primary" @click="saveContainer" style="position: absolute;right:20px;top:20px">保存图表</el-button>
    <div class="visual-container" ref="containerRef">
      <!-- 多媒体统计 -->
      <div class="stat-card">
        <div class="stat-item">
          <h3>图片总数</h3>
          <span class="number">{{ imageCount }}</span>
        </div>
        <div class="stat-item">
          <h3>视频总数</h3>
          <span class="number">{{ videoCount }}</span>
        </div>
      </div>
      <div style="display: flex;width: 100%;height: 100%;">
        <!-- 文件大小分布饼图 -->
        <div ref="sizeChartRef" class="chart-item"></div>

        <!-- 文件类型分布柱状图 -->
        <div ref="typeChartRef" class="chart-item"></div>
      
      </div>
    
    </div>
  </div>

</template>

<script setup lang="ts">
import { ref, onMounted, shallowRef, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { selectAllFile } from '@/api/visual/visual'
import html2canvas from 'html2canvas'
import { ElLoading, ElMessage } from 'element-plus'
interface FileSizeData {
  name: string
  value: number
}

interface FileTypeData {
  name: string
  value: number
}

// 图表DOM引用
const sizeChartRef = ref<HTMLElement>()
const typeChartRef = ref<HTMLElement>()
// DOM引用
const containerRef = ref<HTMLElement>()
// 图表实例
const sizeChart = shallowRef<ECharts>()
const typeChart = shallowRef<ECharts>()

// 响应式数据
const sizeData = ref<FileSizeData[]>([])
const typeData = ref<FileTypeData[]>([])
const imageCount = ref(0)
const videoCount = ref(0)

// 初始化图表配置
const initCharts = () => {
  // 销毁旧实例
  sizeChart.value?.dispose()
  typeChart.value?.dispose()

  if (sizeChartRef.value) {
    sizeChart.value = echarts.init(sizeChartRef.value)
    sizeChart.value.setOption({
      title: {
        text: '文件大小分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'bottom',
        // 修改图例格式
        formatter: (name: string) => {
          // 在数据中查找匹配项
          const item = sizeData.value.find(item => item.name === name)
          // 返回带数量的格式
          return item ? `${name} (${item.value}张)` : name
        },
        // 增加图例间距
        padding: [20, 10],
        // 文本样式
        textStyle: {
          fontSize: 12,
          rich: {
            count: {
              color: '#666',
              padding: [0, 5]
            }
          }
        }
      },
      series: [{
        type: 'pie',
        radius: ['35%', '65%'],
        data: sizeData.value,
        label: {
          formatter: '{b|{b}}\n{d}%',
          rich: {
            b: {
              fontSize: 14,
              lineHeight: 20,
              // 标签也显示数量
              formatter: (params: any) => {
                return `${params.name}\n${params.value}张`
              }
            }
          }
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }]
    })
  }

  // 初始化类型分布柱状图
  if (typeChartRef.value) {
    typeChart.value = echarts.init(typeChartRef.value)
    typeChart.value.setOption({
      title: {
        text: '文件类型分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      xAxis: {
        type: 'category',
        name: '图片类型',  // 新增x轴名称
        nameLocation: 'end',
        nameGap: 10,     // 名称与轴线距离
        nameTextStyle: {
          fontSize: 14,
          fontWeight: 'bold'
        },
        data: typeData.value.map(item => item.name),
        axisLabel: {

          rotate: 0,
          fontSize: 13
        }
      },
      yAxis: {
        type: 'value',
        name: '数量/张',  // 新增y轴名称
        nameLocation: 'end',
        nameGap: 10,
        nameTextStyle: {
          fontSize: 14,
          fontWeight: 'bold'
        },
        axisLabel: {
          formatter: '{value} 张'  // y轴刻度值后加单位
        }
      },
      grid: {  // 调整布局留出坐标轴名称空间
        top: 50,
        bottom: 90,
        left: 80
      },
      series: [{
        type: 'bar',
        data: typeData.value,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 1, color: '#188df0' }
          ])
        },
        label: {
          show: true,
          position: 'top',
          formatter: '{c} 张'  // 柱顶标签加单位
        }
      }]
    })
  }


}

const saveContainer = async () => {
  if (!containerRef.value) return

  try {
    const loading = ElLoading.service({
      lock: true,
      text: '正在生成图片，请稍候...',
      background: 'rgba(255, 255, 255, 0.9)'
    })

    await nextTick()

    const canvas = await html2canvas(containerRef.value, {
      useCORS: true,
      scale: 2,
      backgroundColor: '#ffffff',
      logging: true,
      onclone: (clonedDoc, element) => {
        const charts = element.querySelectorAll<HTMLElement>('.chart-item')
        charts.forEach(chart => {
          chart.style.width = `${chart.offsetWidth}px`
          chart.style.height = `${chart.offsetHeight}px`
        })
      }
    })

    const link = document.createElement('a')

    link.href = canvas.toDataURL('image/png')
    link.download = `文件可视化_${new Date().toISOString().slice(0, 10)}.png`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    loading.close()
  } catch (error) {
    ElMessage.error('图片生成失败，请检查控制台')
  }
}
// 获取数据
const fetchData = async () => {
  try {
    const res = await selectAllFile()
    if (res.code === 200) {
      const data = res.data

      // 更新响应式数据
      sizeData.value = data.sizeData
      typeData.value = data.typeData
      imageCount.value = data.imageCount
      videoCount.value = data.videoCount

      // 数据更新后渲染图表
      initCharts()
    }
  } catch (error) {
  }
}

onMounted(() => {
  fetchData()
})

// 窗口resize监听
window.addEventListener('resize', () => {
  sizeChart.value?.resize()
  typeChart.value?.resize()
})

</script>

<style scoped>
#fileViusal {
  width: 100%;
  height: 100%;
  position: relative;

}

.visual-container {
  width: calc(100% - 40px);
  height: calc(100% - 40px);

  padding: 20px;
  user-select: none;
}

.stat-card {

  height: 100px;
  display: flex;
  margin-bottom: 10px;
  padding: 10px;
  gap: 40px;
  background: #fff;

  border-radius: 8px;
}

.stat-item {

  text-align: center;
}

.number {
  font-size: 32px;
  color: #409eff;
  font-weight: bold;
}

.chart-item {
  height: calc(100% - 180px);
  width: calc(50% - 40px);
  padding: 20px;
  background: #fff;

  border-radius: 8px;

}
</style>