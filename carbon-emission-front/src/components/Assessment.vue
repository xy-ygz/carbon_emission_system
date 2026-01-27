<template>
  <div class="container">
    <!-- 改改style探索下样式-->
    <div :id="ids" :style="{ width: '100%', height: '100%' }"></div>
  </div>
</template>
<!--, border: '1px solid #000'-->
<script>
export default {
  /* x, y, ids 必选；center 可选，不传则默认 ['35%','50%']。饼心偏左时，左侧“outside”的 % 易裁切，可传 ['50%','50%'] 等右移饼心。
     legendRight 可选，图例距右侧距离，如 '5%'、20；不传默认 '10%'，越小图例越靠右。 */
  props: { x: { type: Array }, y: { type: Array }, ids: { type: String }, center: { type: Array, default: null }, legendRight: { type: [String, Number], default: null } },
  mounted() {
    this.drawLine(this.y);
  },
  methods: {
    drawLine(value) {
      let haveValueFlag = false;
      for (let i = 0; i < value.length; i++) {
        var tempValue = value[i].value;
        if (tempValue == 0) {
          value[i].value = null;
        } else {
          haveValueFlag = true;
        }
      }

      this.y = value;
      // 获取 DOM 元素
      const chartDom = document.getElementById(this.ids);
      if (!chartDom) {
        return;
      }
      
      // 先检查是否已有图表实例，如果有则先销毁
      let myChart = this.$echarts.getInstanceByDom(chartDom);
      if (myChart) {
        myChart.dispose();
      }
      
      // 基于准备好的dom，初始化echarts实例
      myChart = this.$echarts.init(chartDom);
      
      // 核心修改：添加自适应缩放监听（使用 ResizeObserver 或 window.onresize）
      // 这里先移除可能存在的旧监听（如果是同一个 DOM 重新初始化）
      if (this._resizeHandler) {
        window.removeEventListener('resize', this._resizeHandler);
      }
      this._resizeHandler = () => {
        if (myChart) {
          myChart.resize();
        }
      };
      window.addEventListener('resize', this._resizeHandler);

      // 使用传入的配置或默认值（恢复原始样式）
      const chartCenter = this.center && this.center.length >= 2 ? this.center : ['35%', '50%'];
      const chartRadius = ['45%', '75%'];
      const legendRight = this.legendRight != null ? this.legendRight : '10%';
      const labelPosition = 'outside';
      const labelLineLength = 15;
      const labelLineLength2 = 10;

      // 绘制图表
      var haveValueOption = {
        legend: {
          orient: 'vertical',
          right: legendRight,
          top: 'middle',
          data: this.x,
          textStyle: {
            fontSize: 14
          },
          itemWidth: 14,
          itemHeight: 14,
          itemGap: 10
        },
        tooltip: {
          trigger: 'item',
          formatter: (params) => {
            // console.log(params);
            return '碳排放量' + '<br/>' + params.marker + params.name + ':' + parseFloat(params.value).toFixed(2) + '(kg)' + '<br/>';
          },
          show: true,
        },
        xAxis: {
          type: "category",
          show: false,
        },
        yAxis: {
          type: "value",

        },
        series: [{
          name: '碳排放量占比',
          type: 'pie',
          radius: chartRadius,
          center: chartCenter,
          barWidth: '10%',
          data: value,
          label: {
            show: true,
            position: labelPosition,
            formatter: `{d}%`,
            fontSize: 12
          },
          labelLine: {
            length: labelLineLength,
            length2: labelLineLength2
          },
          // 避免标签重叠
          avoidLabelOverlap: true
        }]
      };

      myChart.setOption(haveValueOption);

      // 组件销毁时移除监听，防止内存泄漏
      this.$once('hook:beforeDestroy', () => {
        if (this._resizeHandler) {
          window.removeEventListener('resize', this._resizeHandler);
        }
      });
    },
    resize() {
      const chartDom = document.getElementById(this.ids);
      if (chartDom) {
        let myChart = this.$echarts.getInstanceByDom(chartDom);
        if (myChart) {
          // 简单的resize，不改变配置
          myChart.resize();
        }
      }
    }
  },
};
</script>
<style scoped>
#a,
#b {
  float: left;
  margin: 0px;
}
</style>
