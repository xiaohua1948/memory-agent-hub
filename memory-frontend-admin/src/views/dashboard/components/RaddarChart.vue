<template>
  <div :class="className" :style="{height:height,width:width}" />
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import resize from './mixins/resize'

const animationDuration = 3000

export default {
  mixins: [resize],
  props: {
    className: {
      type: String,
      default: 'chart'
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '400px'
    }
  },
  data() {
    return {
      chart: null
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initChart()
    })
  },
  beforeDestroy() {
    if (!this.chart) {
      return
    }
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$el, 'macarons')

      this.chart.setOption({
        title: {
          text: '云相册内容分析',
          subtext: '最近30天用户数据统计',
          left: 'center',
          textStyle: {
            color: '#409EFF',
            fontSize: 18
          }
        },
        tooltip: {
          trigger: 'axis',
          formatter: params => {
            return `${params[0].name}<br/>${params.map(item => {
              return `${item.marker} ${item.seriesName}: ${item.value.toLocaleString()}`
            }).join('<br/>')}`
          }
        },
        radar: {
          radius: '70%',
          center: ['50%', '45%'],
          splitNumber: 5,
          shape: 'circle',
          axisName: {
            color: '#666',
            fontSize: 14
          },
          splitArea: {
            areaStyle: {
              color: ['rgba(64,158,255, 0.1)', 'rgba(64,158,255, 0.05)'],
              shadowColor: 'rgba(0, 0, 0, 0.2)',
              shadowBlur: 15
            }
          },
          indicator: [
            { name: '照片数量', max: 100000 },
            { name: '视频数量', max: 50000 },
            { name: '用户相册数', max: 1000 },
            { name: '分享次数', max: 20000 },
            { name: '收藏次数', max: 30000 },
            { name: '标签使用率', max: 100 }
          ]
        },
        legend: {
          left: 'center',
          bottom: 20,
          icon: 'circle',
          data: ['个人用户', '企业用户', 'VIP用户']
        },
        series: [{
          type: 'radar',
          symbolSize: 0,
          areaStyle: {
            opacity: 0.5
          },
          lineStyle: {
            width: 2
          },
          data: [
            {
              value: [85600, 23400, 780, 15600, 24500, 82],
              name: '个人用户',
              itemStyle: {
                color: '#36a3f7'
              }
            },
            {
              value: [42300, 38700, 450, 9800, 16700, 68],
              name: '企业用户',
              itemStyle: {
                color: '#4ecb73'
              }
            },
            {
              value: [68900, 45200, 320, 23400, 28900, 95],
              name: 'VIP用户',
              itemStyle: {
                color: '#f4516c'
              }
            }
          ],
          animationDuration: animationDuration
        }],
        grid: {
          containLabel: true
        }
      })
    }
  }
}
</script>