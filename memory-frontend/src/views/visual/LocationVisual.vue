<template>
    <div id="locationVisual">
        <div class="container">
            <!-- 统计数据 -->
            <div class="stats-container">
                <div class="stat-item">
                    <h3>照片总数量</h3>
                    <p style="color: #ff6b6b;">{{ photoCount }}</p>
                </div>
                <div class="stat-item">
                    <h3>全国城市</h3>
                    <p>{{ 333 }}</p>
                </div>

                <div class="stat-item">
                    <h3>踏寻足迹城市</h3>
                    <p style="color: #ff6b6b;">{{ cityCount }}</p>
                </div>

            </div>
            <el-button type="primary" @click="toggleSymbol"
                style="position: absolute;right:20px;top:100px;z-index: 3;">{{ showSymbol ? '关闭照片' : '显示照片' }}</el-button>
            <div ref="mainContainer" class="map-main-container">
                <!-- 地图容器 -->
                <div ref="chartRef" class="map-container"></div>

                <div class="custom-legend">
                    <h4>足迹城市统计</h4>
                    <div class="legend-scroll">
                        <div v-for="(city, index) in sortedCities" :key="index" class="legend-item"
                            @click="handleLegendClick(city)">
                            <span class="legend-marker" :style="{ backgroundColor: '#ff6b6b' }"></span>
                            <span class="legend-city">{{ city.city }}</span>
                            <span class="legend-count">{{ city.pictureCount }}张</span>
                        </div>
                    </div>
                </div>
            </div>

        </div>
    </div>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { nextTick } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { useRouter } from 'vue-router'
import { selectAllLocation } from '@/api/visual/visual' // 修正API方法名拼写
import html2canvas from 'html2canvas'
import { ElLoading, ElMessage } from 'element-plus'
const router = useRouter()
const chartRef = ref<HTMLElement>()
const mainContainer = ref<HTMLElement>()
let chart: ECharts | null = null
const geoJsonUrl = '/json/china.json'

// 响应式数据
const photoCount = ref(0)
const countryCount = ref(1) // 默认中国
const cityCount = ref(0)
const visitedCities = ref<Array<{
    city: string
    pictureCount: number
    coverUrl: string
    coordinates: [number, number]
}>>([])
// 图例点击处理（需补充在setup脚本中）
const handleLegendClick = (city: any) => {
    router.push({
        name: 'LocationAlbum',
        params: {
            locationLevel: 'city',
            locationValue: city.city // 确保参数名与路由定义一致
        }
    })
}
const showSymbol = ref(true) // 响应式状态

const toggleSymbol = () => {
    showSymbol.value = !showSymbol.value
    chart?.setOption({
        geo: [{
            regions: visitedCities.value.map(city => ({
                name: city.city,
                itemStyle: {
                    areaColor: '#ff6b6b',
                    borderColor: '#ff4757',
                    borderWidth: 1.2
                },
                label: { show: !showSymbol.value, color: '#333' }

            })),
        }],
        series: [{
            symbol: showSymbol.value
                ? (value: any, params: any) => { value; return `image://${params.data.coverUrl}` }
                : 'none',
            symbolSize: showSymbol.value ? [60, 60] : 0
        }]
    })
}

const saveContainer = async () => {

}
// 从后端获取地理位置数据
const fetchLocationData = async () => {
    try {
        const res = await selectAllLocation()
        if (res.code === 200 && res.data) {
            // 转换后端数据格式
            visitedCities.value = res.data.map((item: { city: any; pictureCount: any; coverUrl: any; longitude: any; latitude: any }) => ({
                city: item.city,
                pictureCount: Number(item.pictureCount) || 0,
                coverUrl: item.coverUrl,
                coordinates: [item.longitude, item.latitude] // 转换坐标格式
            }))

            // 更新统计数据
            updateStatistics()
        }
    } catch (error) {
        console.error('获取地理位置数据失败:', error)
    }
}

// 更新统计指标
const updateStatistics = () => {
    cityCount.value = visitedCities.value.length
    photoCount.value = visitedCities.value.reduce((sum, city) => sum + city.pictureCount, 0)
}

// 排序后的城市数据
const sortedCities = computed(() => {
    return [...visitedCities.value].sort((a, b) => b.pictureCount - a.pictureCount)
})

// 初始化地图
const initChart = async () => {
    if (!chartRef.value) return

    // 销毁旧图表实例
    if (chart) {
        chart.dispose()
    }

    // 加载地理数据
    const geoJson = await (await fetch(geoJsonUrl)).json()
    echarts.registerMap('china', geoJson)

    // 初始化图表
    chart = echarts.init(chartRef.value, 'null', {
        renderer: 'canvas'
    })

    // 准备散点图数据
    const scatterData = visitedCities.value.map(city => ({
        name: city.city,
        value: city.coordinates,
        coverUrl: city.coverUrl,
        pictureCount: city.pictureCount,
    }))

    const option: echarts.EChartsOption = {
        tooltip: {
            trigger: 'item',
            formatter: (params: any) => {

                return params.data?.pictureCount
                    ? `${params.name}: ${params.data.pictureCount}张照片`
                    : params.name
            },
            backgroundColor: '#fff',
            borderColor: '#eee',
            textStyle: { color: '#333' }
        },
        visualMap: { show: false },
        geo: [{
            type: 'map',
            map: 'china',
            roam: true,
            center: [104.1954, 35.8617],
            zoom: 1.75,
            scaleLimit: { min: 1, max: 20 },
            regions: visitedCities.value.map(city => ({
                name: city.city,
                itemStyle: {
                    areaColor: '#ff6b6b',
                    borderColor: '#ff4757',
                    borderWidth: 1.2
                },
                label: { show: false, color: '#333' }

            })),
            itemStyle: {
                areaColor: '#f5f5f7',
                borderColor: '#d0d0d5',
                borderWidth: 0.8
            },
            emphasis: {
                itemStyle: {
                    areaColor: '#1890ff',
                    borderWidth: 1.2
                },
                label: { show: true, color: '#333' }
            }
        }],
        series: [{
            type: 'scatter',
            coordinateSystem: 'geo',
            symbol: (value: any, params: any) => {

                const url = params?.data?.coverUrl;
                return `image://${url}`
            },
            symbolSize: [60, 60],
            symbolOffset: [0, '-50%'],
            symbolKeepAspect: false,
            itemStyle: {

                borderColor: '#fff',
                borderWidth: 2,
                shadowColor: 'rgba(0,0,0,0.3)',
                shadowBlur: 6,
                shadowOffsetY: 3
            },
            emphasis: {
                scale: 1.5,
                itemStyle: {
                    shadowBlur: 10,
                    shadowColor: 'rgba(0,0,0,0.5)'
                }
            },
            label: {
                show: true,
                color: '#1890ff',
                fontSize: 12,
                fontWeight: 'bold',
                position: 'top',
                formatter: (params: any) =>
                    `${params.name}：${params.data.pictureCount}张`
            },
            data: scatterData,
        }]
    }

    chart.setOption(option)

    // 添加事件监听
    chart.on('click', (params: any) => {
        if (params.componentType === 'series' && params.seriesIndex === 0) {
            router.push({
                name: 'LocationAlbum',
                params: {
                    locationLevel: 'city',
                    locationValue: params.data.name
                }
            })
        }
    })

    // 窗口调整监听
    window.addEventListener('resize', () => chart?.resize())
}

onMounted(async () => {
    await fetchLocationData()  // 先获取数据
    await initChart()          // 再初始化图表
})

onUnmounted(() => {
    // 清理资源
    chart?.dispose()
    window.removeEventListener('resize', () => { })
})
</script>

<style scoped>
#locationVisual {
    width: 100%;
    height: 100%;
    position: relative;
}

.container {
    width: calc(100% - 20px);
    height: calc(100% - 20px);
    padding: 10px;
    position: relative;
}

.stats-container {
    height: 70px;
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 10px;
}

.stat-item {
    flex: 1;
    padding: 5px;
    background: #f5f5f5;
    border-radius: 8px;
    text-align: center;

}

.stat-item h3 {
    margin: 0 0 0 0;
    color: #666;
}

.stat-item p {
    margin: 0;
    font-size: 24px;
    font-weight: bold;
    color: #1890ff;
}

.map-main-container {
    width: 100%;
    height: calc(100% - 80px);
    position: relative;
}

.map-container {
    width: 100%;
    height: 100%;
    border: 1px solid #eee;
    border-radius: 8px;
}

.custom-legend {
    position: absolute;
    left: 10px;
    top: 20px;
    width: 180px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    z-index: 10;
    padding: 15px;
    backdrop-filter: blur(5px);
    overflow: hidden;
}

.custom-legend h4 {
    margin: 0 0 12px 0;
    color: #333;
    font-size: 16px;
    border-bottom: 1px solid #eee;
    padding-bottom: 8px;
}

.legend-scroll {
    max-height: 300px;
    overflow-y: auto;
    overflow-x: hidden;
}

.legend-item {
    display: flex;
    align-items: center;
    padding: 10px 5px 5px 5px;
    cursor: pointer;
    transition: all 0.2s;
    border-radius: 4px;
}

.legend-item:hover {
    background-color: rgba(245, 245, 247, 0.8);
    transform: translateX(5px);
}

.legend-marker {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    margin-right: 10px;
}

.legend-city {
    flex: 1;
    color: #666;
    font-size: 14px;
}

.legend-count {
    color: #1890ff;
    font-weight: 500;
    font-size: 13px;
    margin-left: 10px;
}

/* 滚动条样式 */
.legend-scroll::-webkit-scrollbar {
    width: 6px;
}

.legend-scroll::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.05);
}

.legend-scroll::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.2);
    border-radius: 4px;
}

@media (max-width: 768px) {
    .custom-legend {
        width: 90%;
        right: 5%;
        top: auto;
        bottom: 20px;
    }

    .map-container {
        height: calc(100% - 200px);
    }
}
</style>