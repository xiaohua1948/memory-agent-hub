<template>
  <div :class="className" :style="{ height: height, width: width }" />
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import resize from './mixins/resize'

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
          text: '媒体文件类型分布图',
          subtext: '最近30天上传数据统计',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b} : {c} ({d}%)'
        },
        legend: {
          left: 'center',
          bottom: '10',
          data: ['PNG', 'JPG', 'MP4', 'MOV', 'GIF', 'BMP']
        },
        series: [
          {
            name: '文件类型分布',
            type: 'pie',
            roseType: 'radius',
            radius: [15, 95],
            center: ['50%', '38%'],
            data: [
              { value: 1560, name: 'PNG' },
              { value: 2340, name: 'JPG' },
              { value: 980, name: 'MP4' },
              { value: 650, name: 'MOV' },
              { value: 432, name: 'GIF' },
              { value: 320, name: 'BMP' }
            ],
            itemStyle: {
              color: function(params) {
                // 自定义颜色系列
                const colorList = [
                  '#4fd2dd','#36cbcb','#34d1c3',
                  '#40D9AB','#69c0ff','#b37feb'
                ];
                return colorList[params.dataIndex]
              }
            },
            animationEasing: 'cubicInOut',
            animationDuration: 2600,
            label: {
              fontSize: 14
            }
          }
        ]
      })
    }
  }
}
</script>